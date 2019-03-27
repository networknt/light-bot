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

@Deprecated
public class GenChangeLogCmd implements Command {
    private static final Logger logger = LoggerFactory.getLogger(GenChangeLogCmd.class);
    private Executor executor = SingletonServiceFactory.getBean(Executor.class);
    private String organization;
    private String repository;
    private String version;
    private String branch;
    private Path rPath;
    private String token;

    public GenChangeLogCmd(String organization, String repository, String version, String branch, Path rPath) {
        this.organization = organization;
        this.repository = repository;
        this.version = version;
        this.branch = branch;
        this.rPath = rPath;
        // get github token from environment variable
        this.token = System.getenv("CHANGELOG_GITHUB_TOKEN");
    }

    @Override
    public int execute() throws IOException, InterruptedException {
        int result;
        String cmd = String.format("docker run --rm -v %s:/usr/local/src/your-app networknt/github-changelog-generator %s/%s --token %s --future-release %s ; git add . ; git commit -m \"bot checkin\" ; git push origin %s", rPath, organization, repository, token, version, branch);
        // generate changelog with github-changelog-generator docker
        List<String> commands = new ArrayList<>();
        commands.add("bash");
        commands.add("-c");
        commands.add(cmd);
        logger.info(cmd + " for " + rPath);
        result = executor.execute(commands, rPath.toFile());
        StringBuilder stdout = executor.getStdout();
        if(stdout != null && stdout.length() > 0) logger.debug(stdout.toString());
        StringBuilder stderr = executor.getStderr();
        if(stderr != null && stderr.length() > 0) logger.error(stderr.toString());
        return result;
    }

    @Override
    public String getName() {
        return "GenChangeLog";
    }
}
