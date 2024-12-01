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
 * This command is to delete a remote branch in the workspace.
 *
 */
public class DeleteRemoteBranchCmd implements Command {
    private static final Logger logger = LoggerFactory.getLogger(DeleteRemoteBranchCmd.class);
    private final Executor executor = SingletonServiceFactory.getBean(Executor.class);
    private final Path rPath;
    private final String branch;

    public DeleteRemoteBranchCmd(Path rPath, String branch) {
        this.rPath = rPath;
        this.branch = branch;
    }

    @Override
    public int execute() throws IOException, InterruptedException {
        int result;
        // create a branch from the current branch
        List<String> commands = new ArrayList<>();
        commands.add("bash");
        commands.add("-c");

        commands.add("git push -d origin " + branch);
        logger.info("git push -d origin {} for {}", branch, rPath);

        result = executor.execute(commands, rPath.toFile());
        String stdout = executor.getStdout();
        if(stdout != null && !stdout.isEmpty()) logger.debug(stdout);
        String stderr = executor.getStderr();
        if(stderr != null && !stderr.isEmpty()) logger.info(stderr);
        return result;
    }

    @Override
    public String getName() {
        return "DeleteRemoteBranch";
    }
}
