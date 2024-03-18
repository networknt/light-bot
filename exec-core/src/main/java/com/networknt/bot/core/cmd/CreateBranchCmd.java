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

public class CreateBranchCmd implements Command {
    private static final Logger logger = LoggerFactory.getLogger(CreateBranchCmd.class);
    private Executor executor = SingletonServiceFactory.getBean(Executor.class);
    private Path rPath;
    private String branch;
    private String fromTag;

    public CreateBranchCmd(Path rPath, String branch, String fromTag) {
        this.rPath = rPath;
        this.branch = branch;
        this.fromTag = fromTag;
    }

    @Override
    public int execute() throws IOException, InterruptedException {
        int result;
        // create a branch from the current branch
        List<String> commands = new ArrayList<>();
        commands.add("bash");
        commands.add("-c");
        if(fromTag == null) {
            commands.add("git checkout -b " + branch);
            logger.info("git checkout -b " + branch + " for " + rPath);
        } else {
            commands.add("git checkout -b " + branch + " " + fromTag);
            logger.info("git checkout -b " + branch + " " + fromTag + " for " + rPath);
        }
        result = executor.execute(commands, rPath.toFile());
        String stdout = executor.getStdout();
        if(stdout != null && stdout.length() > 0) logger.debug(stdout);
        String stderr = executor.getStderr();
        if(stderr != null && stderr.length() > 0) logger.info(stderr);
        return result;
    }

    @Override
    public String getName() {
        return "CreateBranch";
    }

}
