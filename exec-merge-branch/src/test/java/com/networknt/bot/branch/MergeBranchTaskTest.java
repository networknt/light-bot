package com.networknt.bot.branch;

import org.junit.jupiter.api.Assertions;

import java.io.IOException;

public class MergeBranchTaskTest {
    //@Test
    public void testMergeBranch() throws IOException, InterruptedException {
        MergeBranchTask cmd = new MergeBranchTask();
        int result = cmd.execute();
        Assertions.assertEquals(0, result);
    }
}
