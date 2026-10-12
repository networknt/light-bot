package com.networknt.bot.release;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class PublicationPreflightTest {
    @TempDir Path directory;
    private final List<PublicationPreflight> resources = new ArrayList<>();

    private PublicationPreflight newCheck(com.networknt.bot.core.Executor executor, URI endpoint) {
        PublicationPreflight check = new PublicationPreflight(executor, HttpClient.newHttpClient(), endpoint, "Bearer fixture");
        resources.add(check);
        return check;
    }

    @org.junit.jupiter.api.AfterEach
    void closeClients() { resources.forEach(PublicationPreflight::close); }

    private String project(String artifact, String version) {
        return "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"><groupId>com.example</groupId>"
                + "<artifactId>" + artifact + "</artifactId><version>" + version + "</version></project>";
    }

    @Test
    void checksEveryReactorModuleAndBlocksPartialPublication() throws Exception {
        List<String> requests = java.util.Collections.synchronizedList(new ArrayList<>());
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requests.add(exchange.getRequestURI().getQuery());
            assertEquals("GET", exchange.getRequestMethod());
            assertEquals("Bearer fixture", exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] body = ("{\"published\":" + exchange.getRequestURI().getQuery().contains("name=parent") + "}").getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            Path pom = directory.resolve("effective.xml");
            Files.writeString(pom, "<projects>" + project("parent", "2.4.1") + project("child", "2.4.1") + "</projects>");
            PublicationPreflight check = newCheck(null,
                    URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/"));
            IOException failure = assertThrows(IOException.class, () -> check.checkEffectivePom(pom));
            assertTrue(failure.getMessage().contains("Partially published reactor"));
            assertEquals(java.util.Set.of("namespace=com.example&name=parent&version=2.4.1",
                    "namespace=com.example&name=child&version=2.4.1"), new java.util.HashSet<>(requests));
            assertEquals(2, requests.size());
            Files.writeString(pom, "<projects>" + project("parent", "2.4.1")
                    + project("bad", "${revision}") + project("last", "2.4.1") + "</projects>");
            String message = assertThrows(IOException.class, () -> check.checkEffectivePom(pom)).getMessage();
            assertTrue(message.contains("Published modules: com.example:parent:2.4.1"));
            assertTrue(message.contains("module #2 (artifactId=bad)"));
            assertTrue(requests.stream().anyMatch(query -> query.contains("name=last")));
        } finally { server.stop(0); }
    }

    @Test
    void onlyPublishedFalseAllowsPublicationAndUnknownStatusBlocks() throws Exception {
        int[] status = {200};
        String[] response = {"{\"published\":false}"};
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            byte[] body = response[0].getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status[0], body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            Path pom = directory.resolve("effective.xml");
            Files.writeString(pom, project("client", "2.4.1"));
            PublicationPreflight check = newCheck(null,
                    URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/"));
            assertEquals(PublicationPreflight.Status.UNPUBLISHED, check.checkEffectivePom(pom));
            response[0] = "{\"published\":true}";
            Files.writeString(pom, "<projects>" + project("parent", "2.4.1") + project("child", "2.4.1") + "</projects>");
            assertEquals(PublicationPreflight.Status.PUBLISHED, check.checkEffectivePom(pom));
            response[0] = "{\"published\":false}";
            Files.writeString(pom, project("client", "2.4.1"));
            for (int code : List.of(401, 403, 404, 429, 500, 302)) {
                status[0] = code;
                assertTrue(assertThrows(IOException.class, () -> check.checkEffectivePom(pom))
                        .getMessage().contains("HTTP " + code));
            }
            status[0] = 200;
            for (String invalid : List.of("{}", "{\"published\":\"false\"}", "not JSON")) {
                response[0] = invalid;
                assertTrue(assertThrows(IOException.class, () -> check.checkEffectivePom(pom))
                        .getMessage().contains("publication status unknown"));
            }
            for (String snapshot : List.of("2.4.1-SNAPSHOT", "2.4.1-snapshot", "2.4.1-20261011.192306-1")) {
                Files.writeString(pom, project("client", snapshot));
                assertTrue(assertThrows(IOException.class, () -> check.checkEffectivePom(pom))
                        .getMessage().contains("snapshot"));
            }
            Files.writeString(pom, project("client", "${revision}"));
            assertThrows(IOException.class, () -> check.checkEffectivePom(pom));
            Files.writeString(pom, "<projects/>");
            assertThrows(IOException.class, () -> check.checkEffectivePom(pom));
        } finally { server.stop(0); }
    }

    @Test
    void mavenModelFailureBlocksAndCleansTemporaryOutput() throws Exception {
        List<String> command = new ArrayList<>();
        com.networknt.bot.core.Executor executor = (com.networknt.bot.core.Executor)
                java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),
                        new Class<?>[]{com.networknt.bot.core.Executor.class}, (proxy, method, args) -> {
                            if (method.getName().equals("execute")) {
                                @SuppressWarnings("unchecked") List<String> actual = (List<String>) args[0];
                                command.addAll(actual);
                                assertEquals(directory.toFile(), args[1]);
                                return 17;
                            }
                            if (method.getName().equals("getStdout")) return "model diagnostic";
                            if (method.getName().equals("getStderr")) return "model error";
                            throw new AssertionError("Unexpected executor call");
                        });
        PublicationPreflight check = newCheck(executor,
                URI.create("http://127.0.0.1:1/"));
        var logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(PublicationPreflight.class);
        var previousLevel = logger.getLevel();
        var logs = new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
        logs.start();
        logger.setLevel(ch.qos.logback.classic.Level.DEBUG);
        logger.addAppender(logs);
        try {
            assertTrue(assertThrows(IOException.class, () -> check.check(directory)).getMessage().contains("exit 17"));
            assertTrue(logs.list.stream().anyMatch(event -> event.getFormattedMessage().equals("model diagnostic")));
            assertTrue(logs.list.stream().anyMatch(event -> event.getFormattedMessage().equals("model error")));
        } finally {
            logger.detachAppender(logs);
            logger.setLevel(previousLevel);
            logs.stop();
        }
        assertTrue(command.contains("-DperformRelease"));
        assertFalse(command.contains("deploy"));
        Path output = Path.of(command.getLast().substring("-Doutput=".length()));
        assertFalse(Files.exists(output));
    }

    @Test
    void connectionFailureBlocksInsteadOfTreatingArtifactAsAbsent() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = server.getAddress().getPort();
        server.createContext("/", exchange -> exchange.close());
        server.start();
        try {
            Path pom = directory.resolve("effective.xml");
            Files.writeString(pom, project("client", "2.4.1"));
            PublicationPreflight check = newCheck(null,
                    URI.create("http://127.0.0.1:" + port + "/"));
            assertTrue(assertThrows(IOException.class, () -> check.checkEffectivePom(pom))
                    .getMessage().contains("publication status unknown"));
        } finally { server.stop(0); }
    }

    @Test
    void credentialsUseConfiguredServerAndErrorsDoNotExposeSecrets() throws Exception {
        Path settings = directory.resolve("settings.xml");
        Files.writeString(settings, "<settings><servers><server><id>custom</id>"
                + "<username>fixture-user</username><password>fixture-secret</password></server></servers></settings>");
        assertEquals("Bearer " + java.util.Base64.getEncoder().encodeToString("fixture-user:fixture-secret".getBytes()),
                PublicationPreflight.authorizationFromSettings(settings, "custom"));
        assertThrows(IOException.class, () -> PublicationPreflight.authorizationFromSettings(settings, "missing"));
        Files.writeString(settings, "<settings><servers><server><id>custom</id>"
                + "<username>fixture-user</username><password>{encrypted-secret}</password></server></servers></settings>");
        String message = assertThrows(IOException.class,
                () -> PublicationPreflight.authorizationFromSettings(settings, "custom")).getMessage();
        assertFalse(message.contains("encrypted-secret"));
    }

    @Test
    void checksModulesConcurrentlyWithBoundedWorkers() throws Exception {
        var active = new java.util.concurrent.atomic.AtomicInteger();
        var maximum = new java.util.concurrent.atomic.AtomicInteger();
        var firstWave = new java.util.concurrent.CountDownLatch(4);
        var handlers = java.util.concurrent.Executors.newFixedThreadPool(8);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(handlers);
        server.createContext("/", exchange -> {
            int count = active.incrementAndGet();
            maximum.accumulateAndGet(count, Math::max);
            firstWave.countDown();
            try {
                firstWave.await(5, java.util.concurrent.TimeUnit.SECONDS);
                byte[] body = "{\"published\":false}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                active.decrementAndGet();
                exchange.close();
            }
        });
        server.start();
        try {
            Path pom = directory.resolve("effective.xml");
            StringBuilder model = new StringBuilder("<projects>");
            for (int i = 0; i < 8; i++) model.append(project("module" + i, "2.4.1"));
            Files.writeString(pom, model.append("</projects>"));
            newCheck(null, URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/")).checkEffectivePom(pom);
            assertEquals(4, maximum.get());
        } finally {
            server.stop(0);
            handlers.shutdownNow();
            handlers.close();
        }
    }

    @Test
    void directorySelectionMatchesReleaseTokenization() throws Exception {
        Map<String, Object> config = config();
        config.put("release", List.of("org//first"));
        List<Path> checked = new ArrayList<>();
        List<Path> published = new ArrayList<>();
        ReleaseMavenTask task = new ReleaseMavenTask(config) {
            @Override PublicationPreflight.Status runPublicationPreflight(Path path) { checked.add(path); return PublicationPreflight.Status.UNPUBLISHED; }
            @Override int runRelease(Path path) { published.add(path); return 0; }
        };
        assertEquals(0, task.execute());
        assertEquals(List.of(directory.resolve("first")), checked);
        assertEquals(checked, published);
    }

    private Map<String, Object> config() {
        Map<String, Object> config = new HashMap<>();
        config.put("workspace", directory.toString());
        config.put("version", "2.4.1");
        for (String flag : List.of("skip_checkout", "skip_prepare", "skip_change_log", "skip_checkin",
                "skip_release_note", "skip_deploy", "skip_upload")) config.put(flag, true);
        config.put("skip_release", false);
        config.put("release", List.of("org/first", "org/second"));
        return config;
    }

    @Test
    void allRepositoriesAreCheckedBeforeFirstPublication() throws Exception {
        List<String> calls = new ArrayList<>();
        ReleaseMavenTask task = new ReleaseMavenTask(config()) {
            @Override PublicationPreflight.Status runPublicationPreflight(Path path) { calls.add("check " + path.getFileName()); return PublicationPreflight.Status.UNPUBLISHED; }
            @Override int runRelease(Path path) { calls.add("publish " + path.getFileName()); return 0; }
        };
        assertEquals(0, task.execute());
        assertEquals(List.of("check first", "check second", "publish first", "publish second"), calls);
    }

    @Test
    void failureStopsBeforeChangelogAndChecksRemainingRepositories() throws Exception {
        List<String> calls = new ArrayList<>();
        Map<String, Object> config = config();
        config.put("skip_change_log", false);
        ReleaseMavenTask task = new ReleaseMavenTask(config) {
            @Override PublicationPreflight.Status runPublicationPreflight(Path path) throws IOException {
                calls.add("check " + path.getFileName());
                if (path.getFileName().toString().equals("first")) throw new IllegalStateException("unexpected failure");
                return PublicationPreflight.Status.UNPUBLISHED;
            }
            @Override int runRelease(Path path) { fail("must not publish"); return 0; }
        };
        assertThrows(IOException.class, task::execute);
        assertEquals(List.of("check first", "check second"), calls);
    }

    @Test
    void retryPublishesOnlyFullyUnpublishedRepositories() throws Exception {
        List<String> calls = new ArrayList<>();
        ReleaseMavenTask task = new ReleaseMavenTask(config()) {
            @Override PublicationPreflight.Status runPublicationPreflight(Path path) {
                calls.add("check " + path.getFileName());
                return path.getFileName().toString().equals("first")
                        ? PublicationPreflight.Status.PUBLISHED : PublicationPreflight.Status.UNPUBLISHED;
            }
            @Override int runRelease(Path path) { calls.add("publish " + path.getFileName()); return 0; }
        };
        assertEquals(0, task.execute());
        assertEquals(List.of("check first", "check second", "publish second"), calls);
    }

    @Test
    void publishedRepositoryDoesNotRequireAnotherChangelog() throws Exception {
        Path pending = Files.createDirectory(directory.resolve("second"));
        for (List<String> command : List.of(List.of("git", "init", "-q"),
                List.of("git", "-c", "user.name=Fixture", "-c", "user.email=fixture@example.com",
                        "commit", "--allow-empty", "-q", "-m", "baseline"),
                List.of("git", "tag", "2.4.0"))) {
            Process process = new ProcessBuilder(command).directory(pending.toFile()).redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            assertEquals(0, process.waitFor(), output);
        }
        Map<String, Object> config = config();
        config.put("prev_tag", "2.4.0");
        config.put("skip_change_log", false);
        ReleaseMavenTask task = new ReleaseMavenTask(config) {
            @Override PublicationPreflight.Status runPublicationPreflight(Path path) {
                return path.getFileName().toString().equals("first")
                        ? PublicationPreflight.Status.PUBLISHED : PublicationPreflight.Status.UNPUBLISHED;
            }
            @Override int runRelease(Path path) { assertEquals(pending, path); return 0; }
        };
        assertEquals(0, task.execute());
        assertTrue(Files.readString(pending.resolve("CHANGELOG.md")).contains("## [2.4.1]"));
        assertFalse(Files.exists(directory.resolve("first/CHANGELOG.md")));
    }

    @Test
    void allPublishedStillEnsuresReleaseNotesWithoutRepublishing() throws Exception {
        Map<String, Object> config = config();
        for (String flag : List.of("skip_change_log", "skip_checkin", "skip_release_note", "skip_deploy", "skip_upload"))
            config.put(flag, false);
        List<String> calls = new ArrayList<>();
        for (String repo : List.of("first", "second")) {
            Path path = Files.createDirectory(directory.resolve(repo));
            Files.writeString(path.resolve("CHANGELOG.md"), "## [2.4.1]\nRecovered notes for " + repo);
        }
        ReleaseMavenTask task = new ReleaseMavenTask(config) {
            @Override PublicationPreflight.Status runPublicationPreflight(Path path) {
                calls.add("check " + path.getFileName());
                return PublicationPreflight.Status.PUBLISHED;
            }
            @Override int runRelease(Path path) { fail("must not republish"); return 0; }
            @Override int runReleaseNote(String org, String repo, String branch, String version, String body, Path path) {
                calls.add("notes " + repo);
                return 0;
            }
        };
        assertEquals(0, task.execute());
        assertEquals(List.of("check first", "check second", "notes first", "notes second"), calls);
    }

    @Test
    void mixedRetryEnsuresNotesForBothPublishedAndNewlyPublishedRepositories() throws Exception {
        Map<String, Object> config = config();
        config.put("skip_release_note", false);
        List<String> calls = new ArrayList<>();
        for (String repo : List.of("first", "second")) {
            Path path = Files.createDirectory(directory.resolve(repo));
            Files.writeString(path.resolve("CHANGELOG.md"), "## [2.4.1]\nRecovered notes for " + repo);
        }
        ReleaseMavenTask task = new ReleaseMavenTask(config) {
            @Override PublicationPreflight.Status runPublicationPreflight(Path path) {
                return path.getFileName().toString().equals("first")
                        ? PublicationPreflight.Status.PUBLISHED : PublicationPreflight.Status.UNPUBLISHED;
            }
            @Override int runRelease(Path path) { calls.add("publish " + path.getFileName()); return 0; }
            @Override int runReleaseNote(String org, String repo, String branch, String version, String body, Path path) {
                calls.add("notes " + repo);
                assertTrue(body.contains("Recovered notes for " + repo));
                return 0;
            }
        };
        assertEquals(0, task.execute());
        assertEquals(List.of("publish second", "notes first", "notes second"), calls);
    }

    @Test
    void releaseNoteFailureAfterAllPublishedStillFailsTask() throws Exception {
        Map<String, Object> config = config();
        config.put("skip_release_note", false);
        for (String repo : List.of("first", "second")) {
            Path path = Files.createDirectory(directory.resolve(repo));
            Files.writeString(path.resolve("CHANGELOG.md"), "## [2.4.1]\nNotes");
        }
        ReleaseMavenTask task = new ReleaseMavenTask(config) {
            @Override PublicationPreflight.Status runPublicationPreflight(Path path) { return PublicationPreflight.Status.PUBLISHED; }
            @Override int runReleaseNote(String org, String repo, String branch, String version, String body, Path path) { return 22; }
        };
        assertEquals(22, task.execute());
    }

    @Test
    void explicitReleaseNoteRecoveryKeepsFullListWithoutPublicationChecks() throws Exception {
        Map<String, Object> config = config();
        config.put("skip_release", true);
        config.put("skip_release_note", false);
        List<String> notes = new ArrayList<>();
        for (String repo : List.of("first", "second")) {
            Path path = Files.createDirectory(directory.resolve(repo));
            Files.writeString(path.resolve("CHANGELOG.md"), "## [2.4.1]\nNotes");
        }
        ReleaseMavenTask task = new ReleaseMavenTask(config) {
            @Override PublicationPreflight.Status runPublicationPreflight(Path path) { fail("must not check publication"); return null; }
            @Override int runRelease(Path path) { fail("must not publish"); return 0; }
            @Override int runReleaseNote(String org, String repo, String branch, String version, String body, Path path) {
                notes.add(repo);
                return 0;
            }
        };
        assertEquals(0, task.execute());
        assertEquals(List.of("first", "second"), notes);
    }

    @Test
    void partialPublicationBlocksEvenWhenAnotherRepositoryIsReady() throws Exception {
        List<String> calls = new ArrayList<>();
        ReleaseMavenTask task = new ReleaseMavenTask(config()) {
            @Override PublicationPreflight.Status runPublicationPreflight(Path path) throws IOException {
                calls.add("check " + path.getFileName());
                if (path.getFileName().toString().equals("second")) throw new IOException("Partially published reactor");
                return PublicationPreflight.Status.UNPUBLISHED;
            }
            @Override int runRelease(Path path) { fail("must not publish before full preflight passes"); return 0; }
        };
        assertThrows(IOException.class, task::execute);
        assertEquals(List.of("check first", "check second"), calls);
    }

    @Test
    void skippedPublicationDoesNotContactCentral() throws Exception {
        Map<String, Object> config = config();
        config.put("skip_release", true);
        ReleaseMavenTask task = new ReleaseMavenTask(config) {
            @Override PublicationPreflight.Status runPublicationPreflight(Path path) { fail("must not check"); return PublicationPreflight.Status.UNPUBLISHED; }
            @Override int runRelease(Path path) { fail("must not publish"); return 0; }
        };
        assertEquals(0, task.execute());
    }
}
