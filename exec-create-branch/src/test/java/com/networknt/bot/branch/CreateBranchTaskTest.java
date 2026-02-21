package com.networknt.bot.branch;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class CreateBranchTaskTest {
    @Test
    @Disabled
    public void testCreateBranch() throws IOException, InterruptedException {
        CreateBranchTask cmd = new CreateBranchTask();
        int result = cmd.execute();
        Assertions.assertEquals(0, result);
    }
}
