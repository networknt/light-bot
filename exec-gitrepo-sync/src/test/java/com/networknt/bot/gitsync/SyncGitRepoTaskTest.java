package com.networknt.bot.gitsync;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

public class SyncGitRepoTaskTest {
    @Test
    @Ignore
    public void testSyncGitRepoTask() throws IOException, InterruptedException {
        SyncGitRepoTask cmd = new SyncGitRepoTask();
        int result = cmd.execute();
        Assert.assertEquals(0, result);
    }
}
