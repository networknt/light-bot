package com.networknt.bot.release;

import com.networknt.bot.core.Executor;
import com.networknt.config.Config;
import com.networknt.service.SingletonServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

/** Read-only, fail-closed check of official release coordinates against Central Portal. */
final class PublicationPreflight implements AutoCloseable {
    enum Status { PUBLISHED, UNPUBLISHED }
    private record ModuleStatus(String coordinate, boolean published, String failure) {}

    private static final Logger logger = LoggerFactory.getLogger(PublicationPreflight.class);
    private static final Pattern SNAPSHOT = Pattern.compile(".*(?:SNAPSHOT|-[0-9]{8}\\.[0-9]{6}-[0-9]+)$", Pattern.CASE_INSENSITIVE);
    private final Executor executor;
    private final HttpClient client;
    private final URI endpoint;
    private final String providedAuthorization;

    PublicationPreflight() {
        this(SingletonServiceFactory.getBean(Executor.class),
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
                URI.create("https://central.sonatype.com/api/v1/publisher/published"), null);
    }

    PublicationPreflight(Executor executor, HttpClient client, URI endpoint, String authorization) {
        this.executor = executor;
        this.client = client;
        this.endpoint = endpoint;
        this.providedAuthorization = authorization;
    }

    Status check(Path directory) throws IOException, InterruptedException {
        Path output = Files.createTempFile("light-bot-effective-pom-", ".xml");
        try {
            int result = executor.execute(List.of("mvn", "-B", "-DskipTests", "-DperformRelease",
                    "org.apache.maven.plugins:maven-help-plugin:3.5.1:effective-pom",
                    "-Doutput=" + output), directory.toFile());
            String stdout = executor.getStdout();
            String stderr = executor.getStderr();
            if (stdout != null && !stdout.isEmpty()) logger.debug("{}", stdout);
            if (stderr != null && !stderr.isEmpty()) logger.info("{}", stderr);
            if (result != 0) throw new IOException("Cannot resolve release reactor (Maven exit " + result + ")");
            List<Element> projects = projects(output);
            String authorization = providedAuthorization != null ? providedAuthorization : authorization(directory, projects);
            return checkProjects(projects, authorization);
        } finally {
            Files.deleteIfExists(output);
        }
    }

    Status checkEffectivePom(Path output) throws IOException, InterruptedException {
        if (providedAuthorization == null) throw new IOException("Publishing authentication is required");
        return checkProjects(projects(output), providedAuthorization);
    }

    private static Element xml(Path output) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            return factory.newDocumentBuilder().parse(output.toFile()).getDocumentElement();
        } catch (Exception e) {
            // Do not include parser input or exception text: settings may contain credentials.
            throw new IOException("Cannot read Maven XML (" + e.getClass().getSimpleName() + ")");
        }
    }

    private static List<Element> projects(Path output) throws IOException {
        Element root = xml(output);
        List<Element> projects = new ArrayList<>();
        if (root.getTagName().equals("project")) projects.add(root);
        else if (root.getTagName().equals("projects")) projects.addAll(children(root, "project"));
        if (projects.isEmpty()) throw new IOException("Effective POM contains no reactor projects");
        return projects;
    }

    private Status checkProjects(List<Element> projects, String authorization) throws IOException, InterruptedException {
        List<String> failures = new ArrayList<>();
        List<Future<ModuleStatus>> checks = new ArrayList<>();
        List<String> published = new ArrayList<>();
        List<String> unpublished = new ArrayList<>();
        try (var workers = Executors.newFixedThreadPool(4)) {
            int index = 0;
            for (Element project : projects) {
                index++;
                String label = "module #" + index + " (artifactId=" + text(project, "artifactId") + ")";
                try {
                    String group = value(project, "groupId");
                    String artifact = value(project, "artifactId");
                    String version = value(project, "version");
                    String coordinate = group + ":" + artifact + ":" + version;
                    if (SNAPSHOT.matcher(version).matches()) {
                        failures.add(coordinate + " is a snapshot, not an official release");
                    } else checks.add(workers.submit(() -> published(group, artifact, version, authorization)));
                } catch (IOException | RuntimeException e) {
                    failures.add(label + ": " + (e instanceof IOException ? e.getMessage() : e.getClass().getSimpleName()));
                }
            }
            try {
                for (Future<ModuleStatus> check : checks) {
                    try {
                        ModuleStatus status = check.get();
                        if (status.failure() != null) failures.add(status.failure());
                        else if (status.published()) published.add(status.coordinate());
                        else unpublished.add(status.coordinate());
                    } catch (ExecutionException e) {
                        if (e.getCause() instanceof InterruptedException interrupted) throw interrupted;
                        failures.add("Publication status check failed (" + e.getCause().getClass().getSimpleName() + ")");
                    }
                }
            } catch (InterruptedException e) {
                checks.forEach(check -> check.cancel(true));
                workers.shutdownNow();
                throw e;
            }
        }
        if (!failures.isEmpty()) {
            if (!published.isEmpty()) failures.add("Published modules: " + String.join(", ", published));
            if (!unpublished.isEmpty()) failures.add("Unpublished modules: " + String.join(", ", unpublished));
            throw new IOException(String.join("; ", failures));
        }
        if (!published.isEmpty() && !unpublished.isEmpty()) {
            throw new IOException("Partially published reactor. Published modules: " + String.join(", ", published)
                    + "; Unpublished modules: " + String.join(", ", unpublished));
        }
        return published.isEmpty() ? Status.UNPUBLISHED : Status.PUBLISHED;
    }

    private ModuleStatus published(String group, String artifact, String version, String authorization) throws InterruptedException {
        String coordinate = group + ":" + artifact + ":" + version;
        URI uri = URI.create(endpoint + "?namespace=" + encode(group) + "&name=" + encode(artifact) + "&version=" + encode(version));
        try {
            var response = client.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(15))
                    .header("Authorization", authorization).header("Accept", "application/json").GET().build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) return new ModuleStatus(coordinate, false, coordinate + " publication status unknown (HTTP " + response.statusCode() + ")");
            var published = Config.getInstance().getMapper().readTree(response.body()).get("published");
            if (published == null || !published.isBoolean()) return new ModuleStatus(coordinate, false, coordinate + " publication status unknown (invalid Portal response)");
            if (published.booleanValue()) {
                logger.info("PUBLISHED {}", coordinate);
                return new ModuleStatus(coordinate, true, null);
            }
            logger.info("UNPUBLISHED {}", coordinate);
            return new ModuleStatus(coordinate, false, null);
        } catch (IOException | RuntimeException e) {
            // Never log HTTP bodies, headers, or exception messages containing credentials.
            return new ModuleStatus(coordinate, false, coordinate + " publication status unknown (" + e.getClass().getSimpleName() + ")");
        }
    }

    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }

    private static String authorization(Path directory, List<Element> projects) throws IOException, InterruptedException {
        String serverId = null;
        for (Element project : projects) {
            Element build = child(project, "build");
            Element plugins = build == null ? null : child(build, "plugins");
            if (plugins == null) throw new IOException("No active Central publishing plugin in " + text(project, "artifactId"));
            String moduleServer = null;
            for (Element plugin : children(plugins, "plugin")) {
                if (text(plugin, "groupId").equals("org.sonatype.central")
                        && text(plugin, "artifactId").equals("central-publishing-maven-plugin")) {
                    Element config = child(plugin, "configuration");
                    moduleServer = config == null ? "central" : text(config, "publishingServerId");
                    if (moduleServer.isEmpty()) moduleServer = "central";
                }
            }
            if (moduleServer == null) throw new IOException("No active Central publishing plugin in " + text(project, "artifactId"));
            if (serverId != null && !serverId.equals(moduleServer)) throw new IOException("Multiple publishing server IDs in reactor are not supported");
            serverId = moduleServer;
        }
        Path output = Files.createTempFile("light-bot-publishing-settings-", ".xml");
        try {
            // This command's output must never enter the shared executor or logs.
            Process process = new ProcessBuilder("mvn", "-B", "-q", "-DperformRelease",
                    "org.apache.maven.plugins:maven-help-plugin:3.5.1:effective-settings",
                    "-DshowPasswords=true", "-Doutput=" + output).directory(directory.toFile())
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();
            int result;
            try { result = process.waitFor(); }
            catch (InterruptedException e) { process.destroyForcibly(); throw e; }
            if (result != 0) throw new IOException("Cannot resolve publishing settings (Maven exit " + result + "; output suppressed to protect credentials)");
            return authorizationFromSettings(output, serverId);
        } finally { Files.deleteIfExists(output); }
    }

    static String authorizationFromSettings(Path output, String serverId) throws IOException {
        Element servers = child(xml(output), "servers");
        if (servers != null) for (Element server : children(servers, "server")) {
            if (text(server, "id").equals(serverId)) {
                String username = text(server, "username");
                String password = text(server, "password");
                if (username.isEmpty() || password.isEmpty() || username.contains("${") || password.contains("${")
                        || password.startsWith("{")) throw new IOException("Publishing credentials missing, unresolved, or encrypted; preflight requires resolved token credentials");
                return "Bearer " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
            }
        }
        throw new IOException("Publishing server credentials not found in effective Maven settings");
    }

    private static List<Element> children(Element parent, String name) {
        List<Element> result = new ArrayList<>();
        for (var node = parent.getFirstChild(); node != null; node = node.getNextSibling())
            if (node instanceof Element element && element.getTagName().equals(name)) result.add(element);
        return result;
    }
    private static Element child(Element parent, String name) {
        List<Element> children = children(parent, name);
        return children.isEmpty() ? null : children.getFirst();
    }
    private static String text(Element parent, String name) {
        Element child = child(parent, name);
        return child == null ? "" : child.getTextContent().trim();
    }
    private static String value(Element project, String name) throws IOException {
        String value = text(project, name);
        if (value.matches("[A-Za-z0-9_][A-Za-z0-9_.+-]*")) return value;
        throw new IOException("Missing, unresolved, or invalid Maven " + name);
    }
    @Override public void close() { client.close(); }
}
