package net.lightapi.bot.executor;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class CheckoutCommandTest {
    @Test
    public void testGetDirFromRepo() {
        String repo = "git@github.com:networknt/light-4j.git";
        String d = CheckoutCommand.getDirFromRepo(repo);
        Assert.assertEquals("light-4j", d);
    }

    @Test
    public void testCheckout() throws IOException, InterruptedException {
        CheckoutCommand cmd = new CheckoutCommand();
        int result = cmd.execute();
        Assert.assertEquals(0, result);
    }
}
