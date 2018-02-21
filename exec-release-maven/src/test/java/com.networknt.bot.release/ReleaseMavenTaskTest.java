package com.networknt.bot.release;

import org.junit.Assert;

import java.io.IOException;

public class ReleaseMavenTaskTest {
    //@Test
    public void testReleaseMaven() throws IOException, InterruptedException {
        ReleaseMavenTask cmd = new ReleaseMavenTask();
        int result = cmd.execute();
        Assert.assertEquals(0, result);
    }
}
