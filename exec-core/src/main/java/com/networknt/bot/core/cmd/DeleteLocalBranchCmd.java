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
 * This command is to delete a local branch in the workspace if it exists.
 */
public class DeleteLocalBranchCmd implements Command {
    private static final Logger logger = LoggerFactory.getLogger(DeleteLocalBranchCmd.class);
    private final Executor executor = SingletonServiceFactory.getBean(Executor.class);
    private final Path rPath;
    private final String branch;

    public DeleteLocalBranchCmd(Path rPath, String branch) {
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
        commands.add("if git show-ref --verify --quiet refs/heads/" + branch + "; then git branch -d " + branch + "; else echo 'Branch " + branch + " does not exist'; fi");
        logger.info("Checking if branch {} exists and deleting it for {}", branch, rPath);
        result = executor.execute(commands, rPath.toFile());
        String stdout = executor.getStdout();
        if(stdout != null && !stdout.isEmpty()) logger.debug(stdout);
        String stderr = executor.getStderr();
        if(stderr != null && !stderr.isEmpty()) logger.info(stderr);
        return result;
    }

    @Override
    public String getName() {
        return "DeleteLocalBranch";
    }
}
