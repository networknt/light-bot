package com.networknt.bot.branch;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class DeleteBranchTaskTest {
    @Test
    @Disabled
    public void testDeleteBranch() throws IOException, InterruptedException {
        DeleteBranchTask cmd = new DeleteBranchTask();
        int result = cmd.execute();
        Assertions.assertEquals(0, result);
    }
}
