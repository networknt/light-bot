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

public class DockerBuildCmd implements Command {
    private static final Logger logger = LoggerFactory.getLogger(DockerBuildCmd.class);
    private Executor executor = SingletonServiceFactory.getBean(Executor.class);
    private String version;
    private Path rPath;

    public DockerBuildCmd(String version, Path rPath) {
        this.version = version;
        this.rPath = rPath;
    }

    @Override
    public int execute() throws IOException, InterruptedException {
        int result;
        // maven build command
        List<String> commands = new ArrayList<>();
        commands.add("bash");
        commands.add("-c");
        commands.add("./build.sh " + version);
        logger.info("./build.sh " + version  + " in " + rPath);
        result = executor.execute(commands, rPath.toFile());
        String stdout = executor.getStdout();
        if(stdout != null && stdout.length() > 0) logger.debug(stdout);
        String stderr = executor.getStderr();
        if(stderr != null && stderr.length() > 0) logger.error(stderr);
        return result;
    }

    @Override
    public String getName() {
        return "DockerBuild";
    }
}
