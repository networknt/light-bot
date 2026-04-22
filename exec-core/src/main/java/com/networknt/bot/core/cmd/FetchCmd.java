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

public class FetchCmd implements Command {
    private static final Logger logger = LoggerFactory.getLogger(FetchCmd.class);
    private final Executor executor = SingletonServiceFactory.getBean(Executor.class);
    private final Path rPath;
    private final String remote;

    public FetchCmd(Path rPath, String remote) {
        this.rPath = rPath;
        this.remote = remote;
    }

    @Override
    public int execute() throws IOException, InterruptedException {
        int result;
        List<String> commands = new ArrayList<>();
        commands.add("bash");
        commands.add("-c");

        commands.add("git fetch " + remote);
        logger.info("git fetch {} for {}", remote, rPath);

        result = executor.execute(commands, rPath.toFile());
        String stdout = executor.getStdout();
        if(stdout != null && !stdout.isEmpty()) logger.debug(stdout);
        String stderr = executor.getStderr();
        if(stderr != null && !stderr.isEmpty()) logger.info(stderr);
        return result;
    }

    @Override
    public String getName() {
        return "Fetch";
    }
}
