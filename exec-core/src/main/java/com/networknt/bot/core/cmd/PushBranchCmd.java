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

public class PushBranchCmd implements Command {
    private static final Logger logger = LoggerFactory.getLogger(CheckinBranchCmd.class);
    private Executor executor = SingletonServiceFactory.getBean(Executor.class);
    private String branch;
    private Path rPath;

    public PushBranchCmd(Path rPath, String branch) {
        this.rPath = rPath;
        this.branch = branch;
    }

    @Override
    public int execute() throws IOException, InterruptedException {
        int result;
        List<String> commands = new ArrayList<>();
        commands.add("bash");
        commands.add("-c");
        String command = String.format("git push origin %s", branch);
        commands.add(command);
        logger.info(command);
        result = executor.execute(commands, rPath.toFile());
        String stdout = executor.getStdout();
        if(stdout != null && stdout.length() > 0) logger.debug(stdout);
        String stderr = executor.getStderr();
        if(stderr != null && stderr.length() > 0) logger.info(stderr);
        return result;
    }

    @Override
    public String getName() {
        return "PushBranch";
    }

}
