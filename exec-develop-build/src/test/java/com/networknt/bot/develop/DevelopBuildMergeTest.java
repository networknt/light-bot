package com.networknt.bot.develop;

import com.networknt.bot.core.Constants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DevelopBuildMergeTest {
    @TempDir
    Path workspace;

    @Test
    void mergesRepositoryConfigsAndWritesReadableYaml() throws Exception {
        Path first = Files.createDirectories(workspace.resolve("first"));
        Path second = Files.createDirectories(workspace.resolve("second"));
        Path output = Files.createDirectories(workspace.resolve("output"));
        Files.writeString(first.resolve("values.yml"), "first: café\n");
        Files.writeString(second.resolve("values.yaml"), "second:\n  enabled: true\n");

        DevelopBuildTask task = new DevelopBuildTask();
        task.workspace = workspace.toString();
        task.skipMergeConfig = false;
        task.mergeConfig = Map.of("merge", List.of(Map.of(
                Constants.SKIP, false,
                Constants.FILE, "values",
                Constants.PROJECT, "synthetic",
                Constants.REPOSITORY, List.of("first", "second"),
                Constants.OUTPUT_FORMAT, "yml",
                Constants.OUTPUT, "output")));

        assertEquals(0, task.mergeConfig("merge"));
        assertEquals(Map.of("first", "café", "second", Map.of("enabled", true)),
                ConfigUtils.getInstance().loadMapConfig("values", output.toFile()));
        assertEquals(Map.of("first", "café", "second", Map.of("enabled", true)),
                new Yaml().load(Files.readString(output.resolve("values.yml"))));
    }

    @Test
    void missingRepositoryConfigReturnsMinusOneWithoutWritingOutput() throws Exception {
        Files.createDirectories(workspace.resolve("first"));
        Path output = Files.createDirectories(workspace.resolve("output"));
        Files.writeString(output.resolve("values.yml"), "sentinel");
        assertEquals(-1, mergeTask("values", List.of("first")).mergeConfig("merge"));
        assertEquals("sentinel", Files.readString(output.resolve("values.yml")));
    }

    @Test
    void missingRepositoryConfigNeverUsesClasspathResource() throws Exception {
        Files.createDirectories(workspace.resolve("first"));
        Path output = Files.createDirectories(workspace.resolve("output"));
        Files.writeString(output.resolve("engine-root.yml"), "sentinel");
        // engine-root.yml exists at the classpath root, but is not a repository input.
        assertEquals(-1, mergeTask("engine-root", List.of("first")).mergeConfig("merge"));
        assertEquals("sentinel", Files.readString(output.resolve("engine-root.yml")));
        // The general-purpose API still has its explicit classpath fallback contract.
        assertEquals("root", ConfigUtils.getInstance().loadMapConfig("engine-root", workspace.resolve("first").toFile()).get("source"));
    }

    @Test
    void duplicateRepositoryKeysReturnMinusOneWithoutWritingOutput() throws Exception {
        Path first = Files.createDirectories(workspace.resolve("first"));
        Path second = Files.createDirectories(workspace.resolve("second"));
        Path output = Files.createDirectories(workspace.resolve("output"));
        Files.writeString(first.resolve("values.yml"), "duplicate: one\n");
        Files.writeString(second.resolve("values.yml"), "duplicate: two\n");
        Files.writeString(output.resolve("values.yml"), "sentinel");
        assertEquals(-1, mergeTask("values", List.of("first", "second")).mergeConfig("merge"));
        assertEquals("sentinel", Files.readString(output.resolve("values.yml")));
    }

    private DevelopBuildTask mergeTask(String filename, List<String> repositories) {
        DevelopBuildTask task = new DevelopBuildTask();
        task.workspace = workspace.toString();
        task.skipMergeConfig = false;
        task.mergeConfig = Map.of("merge", List.of(Map.of(
                Constants.SKIP, false,
                Constants.FILE, filename,
                Constants.PROJECT, "synthetic",
                Constants.REPOSITORY, repositories,
                Constants.OUTPUT_FORMAT, "yml",
                Constants.OUTPUT, "output")));
        return task;
    }
}
