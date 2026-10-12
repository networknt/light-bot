package com.networknt.bot.core.cmd;

import com.networknt.bot.core.Command;
import com.networknt.bot.core.Executor;
import com.networknt.config.Config;
import com.networknt.service.SingletonServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GithubReleaseCmd implements Command {
    private static final Logger logger = LoggerFactory.getLogger(GithubReleaseCmd.class);
    private final Executor executor;
    private Path rPath;
    private String token;
    private String organization;
    private String repository;
    private String branch;
    private String version;
    private String body;
    private final boolean allowExisting;
    private final URI api;


    public GithubReleaseCmd(String organization, String repository, String branch, String version, String body, Path rPath) {
        this(organization, repository, branch, version, body, rPath,
                System.getenv("CHANGELOG_GITHUB_TOKEN"), SingletonServiceFactory.getBean(Executor.class));
    }

    public GithubReleaseCmd(String organization, String repository, String branch, String version, String body,
                            Path rPath, boolean allowExisting) {
        this(organization, repository, branch, version, body, rPath,
                System.getenv("CHANGELOG_GITHUB_TOKEN"), SingletonServiceFactory.getBean(Executor.class),
                allowExisting, URI.create("https://api.github.com/"));
    }

    GithubReleaseCmd(String organization, String repository, String branch, String version, String body,
                     Path rPath, String token, Executor executor) {
        this(organization, repository, branch, version, body, rPath, token, executor,
                false, URI.create("https://api.github.com/"));
    }

    GithubReleaseCmd(String organization, String repository, String branch, String version, String body,
                     Path rPath, String token, Executor executor, boolean allowExisting, URI api) {
        this.organization = organization;
        this.repository = repository;
        this.branch = branch;
        this.version = version;
        this.body = body;
        this.rPath = rPath;
        this.token = token;
        this.executor = executor;
        this.allowExisting = allowExisting;
        this.api = api;
    }

    @Override
    public int execute() throws IOException, InterruptedException {
        Map<String, Object> bodyMap = new HashMap<String, Object>();
        bodyMap.put("tag_name", version);
        bodyMap.put("target_commitish", branch);
        bodyMap.put("name", version);
        bodyMap.put("body", body);
        bodyMap.put("draft", false);
        bodyMap.put("prerelease", false);

        String json = Config.getInstance().getMapper().writeValueAsString(bodyMap);
        if (allowExisting) return ensureRelease(json);
        // Pass JSON as one argument. Release text and credentials never become shell code.
        List<String> commands = Arrays.asList("curl", "--fail", "--silent", "--show-error",
                "-X", "POST", "https://api.github.com/repos/" + organization + "/" + repository + "/releases",
                "-H", "authorization: token " + token, "-H", "content-type: application/json",
                "--data-raw", json);
        logger.info("Creating GitHub release {}/{} {}", organization, repository, version);
        int result = executor.execute(commands, rPath.toFile());
        String stdout = executor.getStdout();
        if(stdout != null && stdout.length() > 0) logger.debug(stdout);
        String stderr = executor.getStderr();
        if(stderr != null && stderr.length() > 0) logger.info(stderr);
        return result;
    }

    private int ensureRelease(String json) throws IOException, InterruptedException {
        if (token == null || token.isBlank()) throw new IOException("CHANGELOG_GITHUB_TOKEN is required for release-note recovery");
        URI releases = api.resolve("repos/" + organization + "/" + repository + "/releases");
        URI tag = URI.create(releases + "/tags/" + URLEncoder.encode(version, StandardCharsets.UTF_8).replace("+", "%20"));
        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()) {
            HttpResponse<String> existing = request(client, tag, null);
            if (existing.statusCode() == 200) return existingRelease(existing);
            if (existing.statusCode() != 404) return httpFailure("lookup", existing.statusCode());
            logger.info("Creating missing GitHub release {}/{} {}", organization, repository, version);
            HttpResponse<String> created = request(client, releases, json);
            if (created.statusCode() == 201) return 0;
            if (created.statusCode() == 422) {
                // A concurrent creator may have won; a generic validation error is not success.
                HttpResponse<String> raced = request(client, tag, null);
                if (raced.statusCode() == 200) return existingRelease(raced);
            }
            return httpFailure("creation", created.statusCode());
        }
    }

    private HttpResponse<String> request(HttpClient client, URI uri, String json) throws IOException, InterruptedException {
        var builder = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + token).header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28");
        if (json == null) builder.GET();
        else builder.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
        try {
            return client.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException e) {
            // Do not expose tokens, request headers, release bodies, or API response bodies.
            throw new IOException("GitHub release API request failed (" + e.getClass().getSimpleName() + ")");
        }
    }

    private int existingRelease(HttpResponse<String> response) throws IOException {
        try {
            var tag = Config.getInstance().getMapper().readTree(response.body()).get("tag_name");
            if (tag == null || !tag.isTextual() || !version.equals(tag.textValue()))
                throw new IOException("GitHub release lookup returned an unexpected tag");
        } catch (IOException | RuntimeException e) {
            throw new IOException("Cannot verify existing GitHub release for requested tag");
        }
        logger.info("GitHub release {}/{} {} already exists; keeping existing release notes", organization, repository, version);
        return 0;
    }

    private int httpFailure(String operation, int status) {
        logger.error("GitHub release {} failed for {}/{} {} (HTTP {})", operation, organization, repository, version, status);
        return 22;
    }

    @Override
    public String getName() {
        return "GithubRelease";
    }
}
