package com.networknt.bot.sync;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class SyncGitRepoTaskTest {
    @Test
    @Disabled
    public void testSyncGitRepoTask() throws IOException, InterruptedException {
        SyncGitRepoTask cmd = new SyncGitRepoTask();
        int result = cmd.execute();
        Assertions.assertEquals(0, result);
    }
}
