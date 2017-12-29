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

public class MergeMasterCmd implements Command {
    private static final Logger logger = LoggerFactory.getLogger(MergeMasterCmd.class);
    private Executor executor = SingletonServiceFactory.getBean(Executor.class);
    private Path rPath;

    public MergeMasterCmd(Path rPath) {
        this.rPath = rPath;
    }

    @Override
    public int execute() throws IOException, InterruptedException {
        int result;
        // merge from develop to master
        List<String> commands = new ArrayList<>();
        commands.add("bash");
        commands.add("-c");
        commands.add("git checkout master; git merge develop ; git add . ; git commit -m \"merge from develop\" ; git push origin master");
        logger.info("git checkout master; git merge develop ; git add . ; git commit -m \"merge from develop\" ; git push origin master");
        result = executor.execute(commands, rPath.toFile());
        StringBuilder stdout = executor.getStdout();
        if(stdout != null && stdout.length() > 0) logger.debug(stdout.toString());
        StringBuilder stderr = executor.getStderr();
        if(stderr != null && stderr.length() > 0) logger.error(stderr.toString());
        return result;
    }

    @Override
    public String getName() {
        return "MergeMaster";
    }
}
