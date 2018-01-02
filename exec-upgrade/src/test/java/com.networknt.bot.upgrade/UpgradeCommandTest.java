package com.networknt.bot.upgrade;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class UpgradeCommandTest {
    @Test
    public void testUpgrade() throws IOException, InterruptedException {
        UpgradeCommand cmd = new UpgradeCommand();
        int result = cmd.execute();
        Assert.assertEquals(0, result);
    }
}
