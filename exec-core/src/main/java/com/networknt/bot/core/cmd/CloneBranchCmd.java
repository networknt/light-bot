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

public class CloneBranchCmd implements Command {
    private static final Logger logger = LoggerFactory.getLogger(CloneBranchCmd.class);
    private Executor executor = SingletonServiceFactory.getBean(Executor.class);
    private String repository;
    private String branch;
    private Path wPath;
    private Path rPath;

    public CloneBranchCmd(String repository, String branch, Path wPath, Path rPath) {
        this.repository = repository;
        this.branch = branch;
        this.wPath = wPath;
        this.rPath = rPath;
    }

    @Override
    public int execute() throws IOException, InterruptedException {
        int result;
        // clone it
        List<String> commands = new ArrayList<>();
        commands.add("bash");
        commands.add("-c");
        commands.add("git clone " + repository);
        logger.info("git clone " + repository);
        // execute the command
        result = executor.execute(commands, wPath.toFile());
        // get the stdout and stderr from the command that was run
        String stdout = executor.getStdout();
        if(stdout != null && stdout.length() > 0) logger.debug(stdout);
        String stderr = executor.getStderr();
        if(stderr != null && stderr.length() > 0) logger.error(stderr);
        if(result != 0) {
            return result;
        }
        // need to switch to develop
        commands = new ArrayList<>();
        commands.add("bash");
        commands.add("-c");
        commands.add("git checkout " + branch);
        logger.info("git checkout " + branch);
        result = executor.execute(commands, rPath.toFile());
        stdout = executor.getStdout();
        if(stdout != null && stdout.length() > 0) logger.debug(stdout);
        stderr = executor.getStderr();
        if(stderr != null && stderr.length() > 0) logger.error(stderr);
        return result;
    }

    @Override
    public String getName() {
        return "CloneBranch";
    }
}
