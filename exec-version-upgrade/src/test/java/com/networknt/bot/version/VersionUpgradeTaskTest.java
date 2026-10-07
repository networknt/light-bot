package com.networknt.bot.version;

import org.junit.jupiter.api.Assertions;

import java.io.IOException;

public class VersionUpgradeTaskTest {
    //@Test
    public void testVersionUpgrade() throws IOException, InterruptedException {
        VersionUpgradeTask cmd = new VersionUpgradeTask();
        int result = cmd.execute();
        Assertions.assertEquals(0, result);
    }
}
