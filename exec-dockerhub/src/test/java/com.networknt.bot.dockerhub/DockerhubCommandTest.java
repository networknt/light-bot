package com.networknt.bot.dockerhub;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class DockerhubCommandTest {
    @Test
    public void testDockerhub() throws IOException, InterruptedException {
        DockerhubCommand cmd = new DockerhubCommand();
        int result = cmd.execute();
        Assert.assertEquals(0, result);
    }
}
