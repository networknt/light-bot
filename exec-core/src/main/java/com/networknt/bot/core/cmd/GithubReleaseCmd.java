package com.networknt.bot.core.cmd;

import com.networknt.bot.core.Command;
import com.networknt.bot.core.Executor;
import com.networknt.config.Config;
import com.networknt.service.SingletonServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GithubReleaseCmd implements Command {
    private static final Logger logger = LoggerFactory.getLogger(GithubReleaseCmd.class);
    private final Executor executor;
    private Path rPath;
    private String token;
    private String organization;
    private String repository;
    private String branch;
    private String version;
    private String body;


    public GithubReleaseCmd(String organization, String repository, String branch, String version, String body, Path rPath) {
        this(organization, repository, branch, version, body, rPath,
                System.getenv("CHANGELOG_GITHUB_TOKEN"), SingletonServiceFactory.getBean(Executor.class));
    }

    GithubReleaseCmd(String organization, String repository, String branch, String version, String body,
                     Path rPath, String token, Executor executor) {
        this.organization = organization;
        this.repository = repository;
        this.branch = branch;
        this.version = version;
        this.body = body;
        this.rPath = rPath;
        this.token = token;
        this.executor = executor;
    }

    @Override
    public int execute() throws IOException, InterruptedException {
        Map<String, Object> bodyMap = new HashMap<String, Object>();
        bodyMap.put("tag_name", version);
        bodyMap.put("target_commitish", branch);
        bodyMap.put("name", version);
        bodyMap.put("body", body);
        bodyMap.put("draft", false);
        bodyMap.put("prerelease", false);

        String json = Config.getInstance().getMapper().writeValueAsString(bodyMap);
        // Pass JSON as one argument. Release text and credentials never become shell code.
        List<String> commands = Arrays.asList("curl", "--fail", "--silent", "--show-error",
                "-X", "POST", "https://api.github.com/repos/" + organization + "/" + repository + "/releases",
                "-H", "authorization: token " + token, "-H", "content-type: application/json",
                "--data-raw", json);
        logger.info("Creating GitHub release {}/{} {}", organization, repository, version);
        int result = executor.execute(commands, rPath.toFile());
        String stdout = executor.getStdout();
        if(stdout != null && stdout.length() > 0) logger.debug(stdout);
        String stderr = executor.getStderr();
        if(stderr != null && stderr.length() > 0) logger.info(stderr);
        return result;
    }

    @Override
    public String getName() {
        return "GithubRelease";
    }
}
