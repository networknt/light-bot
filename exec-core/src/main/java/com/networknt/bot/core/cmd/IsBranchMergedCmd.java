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

public class IsBranchMergedCmd implements Command {
    private static final Logger logger = LoggerFactory.getLogger(IsBranchMergedCmd.class);
    private final Executor executor = SingletonServiceFactory.getBean(Executor.class);
    private final Path rPath;
    private final String featureBranchRef;
    private final String targetBranchRef;

    public IsBranchMergedCmd(Path rPath, String featureBranchRef, String targetBranchRef) {
        this.rPath = rPath;
        this.featureBranchRef = featureBranchRef;
        this.targetBranchRef = targetBranchRef;
    }

    @Override
    public int execute() throws IOException, InterruptedException {
        int result;
        List<String> commands = new ArrayList<>();
        commands.add("bash");
        commands.add("-c");

        commands.add("git merge-base --is-ancestor " + featureBranchRef + " " + targetBranchRef);
        
        result = executor.execute(commands, rPath.toFile());
        // Do not log stdout/stderr as errors for this, as a non-zero exit simply means false.
        return result;
    }

    @Override
    public String getName() {
        return "IsBranchMerged";
    }
}
