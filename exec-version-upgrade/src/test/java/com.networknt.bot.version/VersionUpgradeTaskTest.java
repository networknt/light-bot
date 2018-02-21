package com.networknt.bot.version;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class VersionUpgradeTaskTest {
    //@Test
    public void testVersionUpgrade() throws IOException, InterruptedException {
        VersionUpgradeTask cmd = new VersionUpgradeTask();
        int result = cmd.execute();
        Assert.assertEquals(0, result);
    }
}
