package com.networknt.bot.release;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReleasePreparationTest {
    @TempDir
    Path workspace;

    private Map<String, Object> config() {
        Map<String, Object> config = new HashMap<>();
        config.put("workspace", workspace.toString());
        config.put("version", "2.4.1");
        config.put("prev_tag", "2.4.0");
        for (String flag : List.of("skip_checkout", "skip_change_log", "skip_checkin",
                "skip_release_note", "skip_deploy", "skip_upload")) config.put(flag, true);
        config.put("skip_release", false);
        config.put("release", List.of("networknt/http-client", "networknt/light-4j"));
        return config;
    }

    private static class RecordingTask extends ReleaseMavenTask {
        final List<String> calls = new ArrayList<>();
        final List<Path> directories = new ArrayList<>();
        int preparationResult;
        int releaseResult;

        RecordingTask(Map<String, Object> config) {
            super(config);
        }

        @Override
        int runPreparationCommand(String command, Path directory) throws IOException {
            calls.add(command);
            directories.add(directory);
            return preparationResult;
        }

        @Override
        int runRelease(Path directory) {
            calls.add("release " + directory.getFileName());
            directories.add(directory);
            return releaseResult;
        }
    }

    @Test
    void installsFoundationsBeforePublishingClientThenFramework() throws Exception {
        Map<String, Object> config = config();
        String build = "mvn clean install -pl status,monad-result,config,client-config,cluster -am";
        config.put("prepare", List.of(Map.of("light-4j", List.of(build))));
        RecordingTask task = new RecordingTask(config);

        assertEquals(0, task.execute());
        assertEquals(List.of(build, "release http-client", "release light-4j"), task.calls);
        assertEquals(List.of(workspace.resolve("light-4j"), workspace.resolve("http-client"),
                workspace.resolve("light-4j")), task.directories);
    }

    @Test
    void preparationFailureStopsBeforeChangelogsAndPublication() throws Exception {
        Map<String, Object> config = config();
        config.put("skip_change_log", false);
        config.put("prepare", List.of(Map.of("light-4j", List.of("first", "second")),
                Map.of("http-client", List.of("third"))));
        RecordingTask task = new RecordingTask(config);
        task.preparationResult = 17;

        assertEquals(17, task.execute());
        assertEquals(List.of("first"), task.calls);
    }

    @Test
    void preparationRunsInConfiguredOrderEvenWhenPublicationIsSkipped() throws Exception {
        Map<String, Object> config = config();
        config.put("skip_release", true);
        config.put("prepare", List.of(Map.of("light-4j", List.of("first", "second")),
                Map.of("http-client", List.of("third"))));
        RecordingTask task = new RecordingTask(config);

        assertEquals(0, task.execute());
        assertEquals(List.of("first", "second", "third"), task.calls);
    }

    @Test
    void skipPrepareAllowsRetryWithoutRepeatingLocalBuilds() throws Exception {
        Map<String, Object> config = config();
        config.put("skip_prepare", true);
        config.put("prepare", List.of(Map.of("light-4j", List.of("must not run"))));
        RecordingTask task = new RecordingTask(config);

        assertEquals(0, task.execute());
        assertEquals(List.of("release http-client", "release light-4j"), task.calls);
    }

    @Test
    void oldConfigurationNeedsNoNewKeys() throws Exception {
        RecordingTask task = new RecordingTask(config());
        assertEquals(0, task.execute());
        assertEquals(List.of("release http-client", "release light-4j"), task.calls);
        assertEquals("2.4.0", task.previousTag("networknt/http-client"));
    }

    @Test
    void clientPublicationFailureStopsBeforeFrameworkPublication() throws Exception {
        RecordingTask task = new RecordingTask(config());
        task.releaseResult = 23;
        assertEquals(23, task.execute());
        assertEquals(List.of("release http-client"), task.calls);
    }

    @Test
    void joiningRepositoryUsesItsOwnPreviousTag() {
        Map<String, Object> config = config();
        config.put("prev_tags", Map.of("networknt/http-client", "1.0.18"));
        RecordingTask task = new RecordingTask(config);

        assertEquals("1.0.18", task.previousTag("networknt/http-client"));
        assertEquals("2.4.0", task.previousTag("networknt/light-4j"));
    }
}
