package net.lightapi.bot.executor;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class TestCommandTest {
    @Test
    public void testTestCommand() throws IOException, InterruptedException {
        TestCommand cmd = new TestCommand();
        int result = cmd.execute();
        Assert.assertEquals(0, result);
    }
}
