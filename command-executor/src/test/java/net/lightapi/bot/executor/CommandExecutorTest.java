package net.lightapi.bot.executor;

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

        // get the stdout and stderr from the command that was run
        StringBuilder stdout = executor.getStdout();
        StringBuilder stderr = executor.getStderr();

        // print the stdout and stderr
        System.out.println("The numeric result of the command was: " + result);
        System.out.println("STDOUT:");
        System.out.println(stdout);
        System.out.println("STDERR:");
        System.out.println(stderr);
    }

    @Test
    public void testLsCommand() throws IOException, InterruptedException {
        List<String> commands = new ArrayList<>();
        commands.add("ls");
        // execute the command
        Executor executor = SingletonServiceFactory.getBean(Executor.class);
        int result = executor.execute(commands, new File(System.getProperty("user.home")));

        // get the stdout and stderr from the command that was run
        StringBuilder stdout = executor.getStdout();
        StringBuilder stderr = executor.getStderr();

        // print the stdout and stderr
        System.out.println("The numeric result of the command was: " + result);
        System.out.println("STDOUT:");
        System.out.println(stdout);
        System.out.println("STDERR:");
        System.out.println(stderr);

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

        // get the stdout and stderr from the command that was run
        StringBuilder stdout = executor.getStdout();
        StringBuilder stderr = executor.getStderr();

        // print the stdout and stderr
        System.out.println("The numeric result of the command was: " + result);
        System.out.println("STDOUT:");
        System.out.println(stdout);
        System.out.println("STDERR:");
        System.out.println(stderr);

        commands = new ArrayList<>();
        commands.add("bash");
        commands.add("-c");
        commands.add("git clone https://github.com/networknt/light-4j.git");
        result = executor.execute(commands, new File(System.getProperty("java.io.tmpdir")));

        // get the stdout and stderr from the command that was run
        stdout = executor.getStdout();
        stderr = executor.getStderr();

        // print the stdout and stderr
        System.out.println("The numeric result of the command was: " + result);
        System.out.println("STDOUT:");
        System.out.println(stdout);
        System.out.println("STDERR:");
        System.out.println(stderr);

        commands = new ArrayList<>();
        // switch to branch and pull
        commands.add("bash");
        commands.add("-c");
        commands.add("git checkout develop");
        Path path = Paths.get(System.getProperty("java.io.tmpdir"),"light-4j");
        result = executor.execute(commands, path.toFile());

        // get the stdout and stderr from the command that was run
        stdout = executor.getStdout();
        stderr = executor.getStderr();

        System.out.println("The numeric result of the command was: " + result);
        System.out.println("STDOUT:");
        System.out.println(stdout);
        System.out.println("STDERR:");
        System.out.println(stderr);
    }
}
