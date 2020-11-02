package com.networknt.bot.core.cmd;

import com.networknt.bot.core.Command;
import com.networknt.bot.core.Executor;
import com.networknt.service.SingletonServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * This is a generic command that execute a command line in the OS in a certain directory. This
 * can be used in the develop-build to start docker-compose or other dependencies.
 *
 * This command needs a command line statement and a path that indicate the current directory to
 * execute the command.
 *
 * @author Steve Hu
 */
public class GenericSingleCmd implements Command {
    private static final Logger logger = LoggerFactory.getLogger(GenericSingleCmd.class);
    private Executor executor = SingletonServiceFactory.getBean(Executor.class);
    private String cmd;
    private Path rPath;

    public GenericSingleCmd(String cmd, Path rPath) {
        this.cmd = cmd;
        this.rPath = rPath;
    }

    @Override
    public int execute() throws IOException, InterruptedException {
        int result;
        // maven build command
        List<String> commands = new ArrayList<>();
        commands.add("bash");
        commands.add("-c");
        commands.add(cmd);
        logger.info(cmd  + " in " + rPath);
        result = executor.execute(commands, rPath.toFile());
        String stdout = executor.getStdout();
        if(stdout != null && stdout.length() > 0) logger.debug(stdout);
        String stderr = executor.getStderr();
        if(stderr != null && stderr.length() > 0) logger.error(stderr);
        return result;
    }

    @Override
    public String getName() {
        return "GenericSingleCmd";
    }

}
