package com.networknt.bot.release;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class ReleaseCommandTest {
    //@Test
    public void testRelease() throws IOException, InterruptedException {
        ReleaseCommand cmd = new ReleaseCommand();
        int result = cmd.execute();
        Assert.assertEquals(0, result);
    }
}
