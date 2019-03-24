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

public class MergeBranchCmd implements Command {
    private static final Logger logger = LoggerFactory.getLogger(MergeBranchCmd.class);
    private Executor executor = SingletonServiceFactory.getBean(Executor.class);
    private Path rPath;
    private String fromBranch;
    private String toBranch;

    public MergeBranchCmd(Path rPath, String fromBranch, String toBranch) {
        this.rPath = rPath;
        this.fromBranch = fromBranch;
        this.toBranch = toBranch;
    }

    @Override
    public int execute() throws IOException, InterruptedException {
        int result;
        // merge from fromBranch to toBranch
        List<String> commands = new ArrayList<>();
        commands.add("bash");
        commands.add("-c");
        String command = String.format("git checkout %s; git checkout %s; git merge %s", fromBranch, toBranch, fromBranch);
        commands.add(command);
        logger.info(command + " for " + rPath);
        result = executor.execute(commands, rPath.toFile());
        StringBuilder stdout = executor.getStdout();
        if(stdout != null && stdout.length() > 0) logger.debug(stdout.toString());
        StringBuilder stderr = executor.getStderr();
        if(stderr != null && stderr.length() > 0) logger.error(stderr.toString());
        return result;
    }

    @Override
    public String getName() {
        return "MergeBranch";
    }

}
