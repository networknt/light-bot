package net.lightapi.bot.executor;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class BuildCommandTest {
    @Test
    public void testBuild() throws IOException, InterruptedException {
        BuildCommand cmd = new BuildCommand();
        int result = cmd.execute();
        Assert.assertEquals(0, result);
    }

}
