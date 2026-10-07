package com.networknt.bot.core.cmd;

import com.networknt.bot.core.CommandExecutor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Runs only git, with no operator config, templates, hooks, or inherited git overrides. */
class IsolatedGitExecutor extends CommandExecutor {
    private final Path emptyConfig;
    private final Path emptyDirectory;
    private String stdout;
    private String stderr;

    IsolatedGitExecutor(Path root) throws IOException {
        emptyConfig = root.resolve("empty-git-config");
        if (!Files.exists(emptyConfig)) Files.writeString(emptyConfig, "");
        emptyDirectory = root.resolve("empty-git-directory");
        Files.createDirectories(emptyDirectory);
    }

    @Override
    public int execute(List<String> commands, File directory) throws IOException, InterruptedException {
        if (!"git".equals(commands.get(0))) throw new AssertionError("Unexpected command: " + commands.get(0));
        List<String> isolated = new ArrayList<>(List.of("git", "-c", "core.hooksPath=" + emptyDirectory,
                "-c", "init.templateDir=" + emptyDirectory, "-c", "i18n.logOutputEncoding=UTF-8"));
        isolated.addAll(commands.subList(1, commands.size()));
        Path errors = Files.createTempFile(emptyDirectory, "git-errors", ".txt");
        try {
            ProcessBuilder builder = new ProcessBuilder(isolated).directory(directory).redirectError(errors.toFile());
            Map<String, String> env = builder.environment();
            env.keySet().removeIf(key -> key.startsWith("GIT_"));
            env.put("GIT_CONFIG_NOSYSTEM", "1");
            env.put("GIT_CONFIG_GLOBAL", emptyConfig.toString());
            Process process = builder.start();
            stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int result = process.waitFor();
            stderr = Files.readString(errors);
            return result;
        } finally {
            Files.deleteIfExists(errors);
        }
    }

    @Override
    public String getStdout() { return stdout; }

    @Override
    public String getStderr() { return stderr; }
}
