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

public class CheckinBranchCmd implements Command {

    private static final Logger logger = LoggerFactory.getLogger(CheckinBranchCmd.class);
    private Executor executor = SingletonServiceFactory.getBean(Executor.class);
    private String branch;
    private Path rPath;
    private String comment;

    public CheckinBranchCmd(String branch, Path rPath, String comment) {
        this.branch = branch;
        this.rPath = rPath;
        this.comment = comment;
    }

    @Override
    public int execute() throws IOException, InterruptedException {
        int result;
        // checkout branch and check in
        List<String> commands = new ArrayList<>();
        commands.add("bash");
        commands.add("-c");
        String command = String.format("git checkout %s ; git add . ; git commit -m \"%s\" ; git push origin %s", branch, comment, branch);
        commands.add(command);
        logger.info(command);
        result = executor.execute(commands, rPath.toFile());
        StringBuilder stdout = executor.getStdout();
        if(stdout != null && stdout.length() > 0) logger.debug(stdout.toString());
        StringBuilder stderr = executor.getStderr();
        if(stderr != null && stderr.length() > 0) logger.error(stderr.toString());
        return result;
    }

    @Override
    public String getName() {
        return "CheckinBranch";
    }
}
