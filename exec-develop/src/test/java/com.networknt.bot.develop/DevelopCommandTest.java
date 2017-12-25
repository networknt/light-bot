package com.networknt.bot.develop;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class DevelopCommandTest {
    @Test
    public void testDevelop() throws IOException, InterruptedException {
        DevelopCommand cmd = new DevelopCommand();
        int result = cmd.execute();
        Assert.assertEquals(0, result);
    }

}
