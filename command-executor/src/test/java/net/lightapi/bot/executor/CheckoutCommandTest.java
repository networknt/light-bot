package net.lightapi.bot.executor;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class CheckoutCommandTest {
    @Test
    public void testCheckout() throws IOException, InterruptedException {
        CheckoutCommand cmd = new CheckoutCommand();
        int result = cmd.execute();
        Assert.assertEquals(0, result);
    }
}