package com.networknt.bot.develop;

import com.networknt.bot.core.Constants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

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
    }
}
