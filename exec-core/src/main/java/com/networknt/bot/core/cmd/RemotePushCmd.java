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
 * Push the master and sync branches to the internal Git server
 */
public class RemotePushCmd implements Command {
    private static final Logger logger = LoggerFactory.getLogger(RemotePushCmd.class);
    private final Executor executor = SingletonServiceFactory.getBean(Executor.class);
    private final Path rPath;
    private final String toOrigin;
    private final String toBranch;
    private final String remoteUrl;

    public RemotePushCmd(Path rPath, String toOrigin, String toBranch, String remoteUrl) {
        this.rPath = rPath;
        this.toOrigin = toOrigin;
        this.toBranch = toBranch;
        this.remoteUrl = remoteUrl;
    }

    @Override
    public int execute() throws IOException, InterruptedException {
        int result;
        // merge from fromBranch to toBranch
        List<String> commands = new ArrayList<>();
        commands.add("bash");
        commands.add("-c");
        // first check if the remote is already added to the local repo. If not, add it.
        String command = String.format("if ! git remote get-url %1$s &>/dev/null; then git remote add %1$s %2$s; fi; git checkout %3$s; if git ls-remote --exit-code --heads %1$s %3$s >/dev/null 2>&1; then git pull %1$s %3$s; fi; git push -u %1$s %3$s;", toOrigin, remoteUrl, toBranch);
        commands.add(command);
        logger.info(command + " for " + rPath);
        result = executor.execute(commands, rPath.toFile());
        String stdout = executor.getStdout();
        if(stdout != null && stdout.length() > 0) logger.debug(stdout);
        String stderr = executor.getStderr();
        if(stderr != null && stderr.length() > 0) logger.info(stderr);
        return result;
    }

    @Override
    public String getName() {
        return "RemotePush";
    }

}
