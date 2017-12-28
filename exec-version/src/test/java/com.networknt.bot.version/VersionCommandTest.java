package com.networknt.bot.version;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class VersionCommandTest {
    //@Test
    public void testVersion() throws IOException, InterruptedException {
        VersionCommand cmd = new VersionCommand();
        int result = cmd.execute();
        Assert.assertEquals(0, result);
    }
}
