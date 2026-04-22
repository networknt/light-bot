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

public class GetRemoteBranchesCmd implements Command {
    private static final Logger logger = LoggerFactory.getLogger(GetRemoteBranchesCmd.class);
    private final Executor executor = SingletonServiceFactory.getBean(Executor.class);
    private final Path rPath;
    private final String remote;
    private final String prefix;
    private List<String> branches = new ArrayList<>();

    public GetRemoteBranchesCmd(Path rPath, String remote, String prefix) {
        this.rPath = rPath;
        this.remote = remote;
        this.prefix = prefix;
    }

    @Override
    public int execute() throws IOException, InterruptedException {
        int result;
        List<String> commands = new ArrayList<>();
        commands.add("bash");
        commands.add("-c");
        
        String gitCmd = "git ls-remote --heads " + remote;
        if (prefix != null && !prefix.isEmpty()) {
            gitCmd += " " + prefix + "*";
        }
        commands.add(gitCmd);
        
        result = executor.execute(commands, rPath.toFile());
        String stdout = executor.getStdout();
        if(stdout != null && !stdout.isEmpty()) {
            String[] lines = stdout.split("\\r?\\n");
            for(String line : lines) {
                // Example line: "c5b2a0c... refs/heads/feature/login"
                String[] parts = line.split("\\s+");
                if (parts.length == 2) {
                    String ref = parts[1];
                    if (ref.startsWith("refs/heads/")) {
                        branches.add(ref.substring("refs/heads/".length()));
                    }
                }
            }
        }
        String stderr = executor.getStderr();
        if(stderr != null && !stderr.isEmpty()) logger.info(stderr);
        return result;
    }

    @Override
    public String getName() {
        return "GetRemoteBranches";
    }

    public List<String> getBranches() {
        return branches;
    }
}
