package com.networknt.bot.core.cmd;

import com.networknt.config.JsonMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

class GithubReleaseRecoveryTest {
    @TempDir Path directory;

    private GithubReleaseCmd command(HttpServer server, String body) {
        return new GithubReleaseCmd("example", "demo", "main", "1.1.0", body, directory,
                "fixture-token", null, true, URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/"));
    }

    private static void reply(com.sun.net.httpserver.HttpExchange exchange, int code, String body) throws java.io.IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    @Test
    void existingReleaseIsSuccessWithoutPostingOrOverwriting() throws Exception {
        List<String> requests = new ArrayList<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requests.add(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath());
            reply(exchange, 200, "{\"tag_name\":\"1.1.0\",\"body\":\"existing notes\"}");
        });
        server.start();
        try {
            assertEquals(0, command(server, "replacement notes").execute());
            assertEquals(List.of("GET /repos/example/demo/releases/tags/1.1.0"), requests);
        } finally { server.stop(0); }
    }

    @Test
    void missingReleaseIsCreatedWithExactLargeBody() throws Exception {
        List<String> requests = new ArrayList<>();
        List<String> payloads = new ArrayList<>();
        AtomicBoolean authenticated = new AtomicBoolean(true);
        String notes = "Fix user's parser \"quote\" $(touch marker) `touch marker`\n".repeat(5000);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requests.add(exchange.getRequestMethod());
            if (!"Bearer fixture-token".equals(exchange.getRequestHeaders().getFirst("Authorization"))) authenticated.set(false);
            if (exchange.getRequestMethod().equals("GET")) reply(exchange, 404, "{}");
            else {
                payloads.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                reply(exchange, 201, "{\"tag_name\":\"1.1.0\"}");
            }
        });
        server.start();
        try {
            assertEquals(0, command(server, notes).execute());
            assertEquals(List.of("GET", "POST"), requests);
            assertTrue(authenticated.get());
            assertEquals(notes, JsonMapper.string2Map(payloads.getFirst()).get("body"));
        } finally { server.stop(0); }
    }

    @Test
    void lookupErrorsNeverCreateRelease() throws Exception {
        int[] status = {403};
        List<String> requests = new ArrayList<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> { requests.add(exchange.getRequestMethod()); reply(exchange, status[0], "{}"); });
        server.start();
        try {
            for (int code : List.of(401, 403, 429, 500)) {
                status[0] = code;
                assertEquals(22, command(server, "notes").execute());
            }
            assertEquals(List.of("GET", "GET", "GET", "GET"), requests);
        } finally { server.stop(0); }
    }

    @Test
    void validationErrorIsSuccessOnlyIfRequestedTagNowExists() throws Exception {
        boolean[] concurrentCreator = {true};
        int[] lookupCount = {0};
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            if (exchange.getRequestMethod().equals("POST")) reply(exchange, 422, "{}");
            else {
                lookupCount[0]++;
                reply(exchange, lookupCount[0] == 2 && concurrentCreator[0] ? 200 : 404,
                        "{\"tag_name\":\"1.1.0\"}");
            }
        });
        server.start();
        try {
            assertEquals(0, command(server, "notes").execute());
            concurrentCreator[0] = false;
            lookupCount[0] = 0;
            assertEquals(22, command(server, "notes").execute());
        } finally { server.stop(0); }
    }

    @Test
    void malformedOrWrongTagLookupBlocks() throws Exception {
        String[] body = {"{}"};
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> reply(exchange, 200, body[0]));
        server.start();
        try {
            for (String response : List.of("{}", "not JSON", "{\"tag_name\":\"other\"}")) {
                body[0] = response;
                assertThrows(java.io.IOException.class, () -> command(server, "notes").execute());
            }
        } finally { server.stop(0); }
    }
}
