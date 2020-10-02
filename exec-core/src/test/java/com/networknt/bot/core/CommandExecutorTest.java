package com.networknt.bot.core;

import com.networknt.service.SingletonServiceFactory;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class CommandExecutorTest {
    @Test
    public void testSimpleCommand() throws IOException, InterruptedException {
        // build the system command we want to run
        List<String> commands = new ArrayList<String>();
        commands.add("/bin/sh");
        commands.add("-c");
        commands.add("ls -l /var/tmp | wc -l");

        // execute the command
        Executor executor = SingletonServiceFactory.getBean(Executor.class);
        int result = executor.execute(commands, new File(System.getProperty("user.home")));
    }

    @Test
    public void testLsCommand() throws IOException, InterruptedException {
        List<String> commands = new ArrayList<>();
        commands.add("ls");
        // execute the command
        Executor executor = SingletonServiceFactory.getBean(Executor.class);
        int result = executor.execute(commands, new File(System.getProperty("user.home")));

    }

    @Test
    public void testGitClone() throws IOException, InterruptedException {
        List<String> commands = new ArrayList<>();
        commands.add("bash");
        commands.add("-c");
        commands.add("rm -rf light-4j");
        // execute the command
        Executor executor = SingletonServiceFactory.getBean(Executor.class);
        int result = executor.execute(commands, new File(System.getProperty("java.io.tmpdir")));

        commands = new ArrayList<>();
        commands.add("bash");
        commands.add("-c");
        commands.add("git clone https://github.com/networknt/light-4j.git");
        result = executor.execute(commands, new File(System.getProperty("java.io.tmpdir")));

        commands = new ArrayList<>();
        // switch to branch and pull
        commands.add("bash");
        commands.add("-c");
        commands.add("git checkout master");
        Path path = Paths.get(System.getProperty("java.io.tmpdir"),"light-4j");
        result = executor.execute(commands, path.toFile());
    }
}
