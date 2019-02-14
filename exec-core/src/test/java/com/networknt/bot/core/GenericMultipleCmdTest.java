package com.networknt.bot.core;

import com.networknt.bot.core.cmd.GenericMultipleCmd;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class GenericMultipleCmdTest {
    @Test
    public void testGenericMultipleCmd() throws IOException, InterruptedException {
        // run the 'touch gmc003899383.txt, echo "this is a test" > gmc003899383.txt',  with GenericMultipleCmd
        List<String> cmds = new LinkedList<>();
        cmds.add("touch gmc003899383.txt");
        cmds.add("echo \"this is a test.\" > gmc003899383.txt");
        cmds.add("rm gmc003899383.txt");
        cmds.add("ls -l");
        GenericMultipleCmd cmd = new GenericMultipleCmd(cmds, new File(System.getProperty("java.io.tmpdir")).toPath());
        cmd.execute();
    }
}
