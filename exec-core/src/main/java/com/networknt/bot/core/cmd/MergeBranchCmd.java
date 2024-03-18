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
    private final Executor executor = SingletonServiceFactory.getBean(Executor.class);
    private final Path rPath;
    private final String fromBranch;
    private final String toBranch;

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
        String command = String.format("if git show-ref --verify --quiet refs/heads/%s; then git checkout %s; else git checkout -b %s; fi; git merge %s;", toBranch, toBranch, toBranch, fromBranch);
        // [bash, -c, if git show-ref --verify --quiet refs/heads/sync; then git checkout sync; else git checkout -b sync; fi; git merge master;]
        logger.info(command + " for " + rPath);
        commands.add(command);
        result = executor.execute(commands, rPath.toFile());
        String stdout = executor.getStdout();
        if(stdout != null && stdout.length() > 0) logger.debug(stdout);
        String stderr = executor.getStderr();
        if(stderr != null && stderr.length() > 0) logger.info(stderr);
        return result;
    }

    @Override
    public String getName() {
        return "MergeBranch";
    }

}
