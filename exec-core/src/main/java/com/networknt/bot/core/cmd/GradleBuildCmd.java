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

public class GradleBuildCmd implements Command {
    private static final Logger logger = LoggerFactory.getLogger(GradleBuildCmd.class);
    private Executor executor = SingletonServiceFactory.getBean(Executor.class);
    private Path rPath;

    public GradleBuildCmd(Path rPath) {
        this.rPath = rPath;
    }

    @Override
    public int execute() throws IOException, InterruptedException {
        int result;
        // maven build command
        List<String> commands = new ArrayList<>();
        commands.add("bash");
        commands.add("-c");
        commands.add("./gradlew clean build install");
        logger.info("gradlew clean build install in " + rPath);
        result = executor.execute(commands, rPath.toFile());
        String stdout = executor.getStdout();
        if(stdout != null && stdout.length() > 0) logger.debug(stdout);
        String stderr = executor.getStderr();
        if(stderr != null && stderr.length() > 0) logger.error(stderr);
        return result;
    }

    @Override
    public String getName() {
        return "GradleBuild";
    }

}
