package com.networknt.bot.core;

import com.networknt.bot.core.cmd.GenericSingleCmd;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class GenericSingleCmdTest {

    @Test
    public void testGenericSingleCmd() throws IOException, InterruptedException {
        // run the 'ls -l' with GenericSingleCmd
        GenericSingleCmd cmd = new GenericSingleCmd("ls -l", new File(System.getProperty("java.io.tmpdir")).toPath());
        cmd.execute();
    }

}
