package com.networknt.bot.branch;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

public class DeleteBranchTaskTest {
    @Test
    @Ignore
    public void testDeleteBranch() throws IOException, InterruptedException {
        DeleteBranchTask cmd = new DeleteBranchTask();
        int result = cmd.execute();
        Assert.assertEquals(0, result);
    }
}
