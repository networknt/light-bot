package com.networknt.bot.docker;

import org.junit.jupiter.api.Assertions;

import java.io.IOException;

public class ReleaseDockerTaskTest {
    //@Test
    public void testReleaseDocker() throws IOException, InterruptedException {
        ReleaseDockerTask cmd = new ReleaseDockerTask();
        int result = cmd.execute();
        Assertions.assertEquals(0, result);
    }
}
