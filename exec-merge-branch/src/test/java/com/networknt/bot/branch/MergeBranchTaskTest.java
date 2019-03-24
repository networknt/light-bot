package com.networknt.bot.branch;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class MergeBranchTaskTest {
    @Test
    public void testMergeBranch() throws IOException, InterruptedException {
        MergeBranchTask cmd = new MergeBranchTask();
        int result = cmd.execute();
        Assert.assertEquals(0, result);
    }
}
