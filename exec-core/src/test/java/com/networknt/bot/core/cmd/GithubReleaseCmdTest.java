package com.networknt.bot.core.cmd;

import com.networknt.bot.core.CommandExecutor;
import com.networknt.config.JsonMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GithubReleaseCmdTest {
    @TempDir
    Path directory;

    private static class RecordingExecutor extends CommandExecutor {
        List<String> arguments;
        int result;

        @Override
        public int execute(List<String> commands, File workingDir) {
            arguments = List.copyOf(commands);
            return result;
        }

        @Override
        public String getStdout() { return ""; }

        @Override
        public String getStderr() { return ""; }
    }

    @Test
    void releaseTextIsOneJsonArgumentAndNeverShellCode() throws Exception {
        RecordingExecutor executor = new RecordingExecutor();
        String body = "Fix user's parser\n'; touch marker; echo ' $(touch marker) `touch marker`"
                + "\nQuotes: \"value\"; Unicode: café";
        GithubReleaseCmd command = new GithubReleaseCmd("example", "demo", "main", "1.1.0", body,
                directory, "test-token", executor);
        assertEquals(0, command.execute());
        assertEquals("curl", executor.arguments.get(0));
        assertFalse(executor.arguments.contains("bash"));
        assertFalse(executor.arguments.contains("-c"));
        int payload = executor.arguments.indexOf("--data-raw") + 1;
        assertEquals(executor.arguments.size() - 1, payload);
        Map<String, Object> json = JsonMapper.string2Map(executor.arguments.get(payload));
        assertEquals(body, json.get("body"));
        assertEquals("1.1.0", json.get("tag_name"));
        assertTrue(executor.arguments.contains("--fail"));
    }

    @Test
    void propagatesCurlFailure() throws Exception {
        RecordingExecutor executor = new RecordingExecutor();
        executor.result = 22;
        assertEquals(22, new GithubReleaseCmd("example", "demo", "main", "1.1.0", "notes",
                directory, "test-token", executor).execute());
    }
}
