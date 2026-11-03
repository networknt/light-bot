package com.networknt.bot.core.cmd;

import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryJsonTest {
    private final SyncGitRepoCmd command = new SyncGitRepoCmd(
            Path.of("."), "source", "target", "organization", "light-bot");

    @Test
    void findsRepositoryInProviderJson() throws IOException {
        String repositories = """
                [{"name":"other","id":1},
                 {"name":"light-bot","private":true,"owner":{"login":"networknt"}}]
                """;
        assertTrue(command.repoExist(repositories, "light-bot"));
        assertFalse(command.repoExist(repositories, "missing"));
        assertFalse(command.repoExist("[]", "light-bot"));
    }

    @Test
    void rejectsMalformedProviderJson() {
        assertThrows(JacksonException.class, () -> command.repoExist("[{", "light-bot"));
    }
}
