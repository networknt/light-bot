package com.networknt.bot.release;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class ReleaseMavenTaskTest {
    //@Test
    public void testReleaseMaven() throws IOException, InterruptedException {
        ReleaseMavenTask cmd = new ReleaseMavenTask();
        int result = cmd.execute();
        Assert.assertEquals(0, result);
    }

    //@Test
    public void testDeploy() throws IOException, InterruptedException {
        ReleaseMavenTask task = new ReleaseMavenTask();
        int result = task.deploy();
        Assert.assertEquals(0, result);
    }

    //@Test
    public void testUpload() throws IOException, InterruptedException {
        ReleaseMavenTask task = new ReleaseMavenTask();
        int result = task.upload();
        Assert.assertEquals(0, result);
    }
}
