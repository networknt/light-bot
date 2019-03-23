package com.networknt.bot.branch;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class CreateBranchTaskTest {
    //@Test
    public void testCreateBranch() throws IOException, InterruptedException {
        CreateBranchTask cmd = new CreateBranchTask();
        int result = cmd.execute();
        Assert.assertEquals(0, result);
    }
}
