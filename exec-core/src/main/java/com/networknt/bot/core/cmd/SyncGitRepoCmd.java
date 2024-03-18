package com.networknt.bot.core.cmd;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.bot.core.Command;
import com.networknt.bot.core.Executor;
import com.networknt.service.SingletonServiceFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SyncGitRepoCmd implements Command {
    private static final Logger logger = LoggerFactory.getLogger(SyncGitRepoCmd.class);
    private Executor executor = SingletonServiceFactory.getBean(Executor.class);
    private ObjectMapper mapper = new ObjectMapper();
    private String source;
    private String target;
    private String organization;
    private String repository;
    private Path rPath;


    public SyncGitRepoCmd(Path rPath, String source, String target, String organization, String repository) {
        this.rPath = rPath;
        this.source = source;
        this.target = target;
        this.organization = organization;
        this.repository = repository;
    }

    @Override
    public int execute() throws IOException, InterruptedException {
        int result = 0;
        // make sure that the source repo exists

        // make sure that the target repo exists.
        //
        // Otherwise, create it.

        return result;
    }

    @Override
    public String getName() {
        return "SyncGitRepo";
    }

    public String getGithubRepos(String githubUrl, String organization, String githubToken) throws IOException, InterruptedException {
        String cmd = String.format("curl -k --location --request GET '%s/orgs/%s/repos' -H 'Accept: application/vnd.github.v3+json' -H 'authorization: token %s'", githubUrl, organization, githubToken);
        System.out.println(cmd);
        List<String> commands = new ArrayList<>();
        commands.add("bash");
        commands.add("-c");
        commands.add(cmd);
        logger.info("run " + cmd  + " in " + rPath);
        executor.execute(commands, rPath.toFile());
        String stdout = executor.getStdout();
        if(stdout != null && stdout.length() > 0) {
            logger.debug(stdout);
            return stdout;
        }
        String stderr = executor.getStderr();
        if(stderr != null && stderr.length() > 0) {
            logger.info(stderr);
        }
        return String.format("{\"error\":\"%s\"}", stderr);
    }

    public String getGogsRepos(String gogsUrl, String organization, String gogsToken) throws IOException, InterruptedException {
        String cmd = String.format("curl -k --location --request GET '%s/api/v1/orgs/%s/repos' --header 'Authorization: token %s'", gogsUrl, organization, gogsToken);
        System.out.println(cmd);
        // update project version with maven
        List<String> commands = new ArrayList<>();
        commands.add("bash");
        commands.add("-c");
        commands.add(cmd);
        logger.info("run " + cmd  + " in " + rPath);
        executor.execute(commands, rPath.toFile());
        String stdout = executor.getStdout();
        if(stdout != null && stdout.length() > 0) {
            logger.debug(stdout);
            return stdout;
        }
        String stderr = executor.getStderr();
        if(stderr != null && stderr.length() > 0) {
            logger.info(stderr);
        }
        return String.format("{\"error\":\"%s\"}", stderr);
    }

    public boolean repoExist(String reposStr, String repository) throws IOException {
        boolean result = false;
        List<Map<String, Object>> repos = mapper.readValue(reposStr, new TypeReference<List<Map<String, Object>>>() {});
        // iterate the entire list of repos.
        for (Map<String, Object> repo: repos) {
            String name = (String)repo.get("name");
            if(name.equals(repository)) {
                result = true;
            }
        }
        return result;
    }

    //public boolean createGithubRepo()
}
