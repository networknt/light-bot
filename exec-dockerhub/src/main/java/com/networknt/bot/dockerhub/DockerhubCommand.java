package com.networknt.bot.dockerhub;

import com.networknt.bot.core.Command;
import com.networknt.bot.core.Constants;
import com.networknt.bot.core.RegexReplacement;
import com.networknt.bot.core.cmd.*;
import com.networknt.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DockerhubCommand implements Command {
    private static final Logger logger = LoggerFactory.getLogger(DockerhubCommand.class);
    private static final String CONFIG_NAME = "dockerhub";
    private Map<String, Object> config = Config.getInstance().getJsonMapConfig(CONFIG_NAME);
    private String workspace = (String)config.get(Constants.WORKSPACE);
    private String version = (String)config.get(Constants.VERSION);

    @SuppressWarnings("unchecked")
    private Map<String, Object> checkout = (Map<String, Object>)config.get(Constants.CHECKOUT);
    private String branch = (String)checkout.get(Constants.BRANCH);
    @SuppressWarnings("unchecked")
    private List<String> repositories = (List<String>)checkout.get(Constants.REPOSITORY);

    @SuppressWarnings("unchecked")
    private List<String> merges = (List<String>)config.get(Constants.MERGE);
    @SuppressWarnings("unchecked")
    private List<String> mavens = (List<String>)config.get(Constants.MAVEN);
    @SuppressWarnings("unchecked")
    //private Map<String, Object> docker = (Map<String, Object>)config.get(Constants.DOCKER);
    private String userHome = System.getProperty("user.home");

    @Override
    public String getName() {
        return "dockerhub";
    }

    @Override
    public int execute() throws IOException, InterruptedException {
        int result = checkout();
        if(result != 0) return result;
        result = merge();
        if(result != 0) return result;
        result = maven();
        if(result != 0) return result;
        //result = docker();
        return result;
    }

    private int checkout() throws IOException, InterruptedException {
        int result = 0;

        // check if there is a directory workspace in home directory.
        Path wPath = Paths.get(userHome, workspace);
        if(Files.notExists(wPath)) {
            Files.createDirectory(wPath);
        }

        for(String repository: repositories) {
            Path rPath = Paths.get(userHome, workspace, getDirFromRepo(repository));
            if(Files.notExists(rPath)) {
                // clone and switch to branch.
                CloneBranchCmd cloneBranchCmd = new CloneBranchCmd(repository, branch, wPath, rPath);
                result = cloneBranchCmd.execute();
                if(result != 0) break;
            } else {
                // switch to branch and pull
                CheckoutPullCmd checkoutPullCmd = new CheckoutPullCmd(branch, rPath);
                result = checkoutPullCmd.execute();
                if(result != 0) break;
            }
        }
        return result;
    }

    private int merge() throws IOException, InterruptedException {
        int result = 0;
        for(String merge: merges) {
            Path rPath = Paths.get(userHome, workspace, merge);
            MergeMasterCmd mergeMasterCmd = new MergeMasterCmd(rPath);
            result = mergeMasterCmd.execute();
            if(result != 0) break;
        }
        return result;
    }

    private int maven() throws IOException, InterruptedException {
        int result = 0;
        for(String maven: mavens) {
            Path rPath = Paths.get(userHome, workspace, maven);
            MavenBuildCmd mavenBuildCmd = new MavenBuildCmd(rPath);
            result = mavenBuildCmd.execute();
            if(result != 0) break;
        }
        return result;
    }

    /*
    private int version() throws IOException {
        int result = 0;
        for(Map.Entry<String, Object> entry : version.entrySet()) {
            String repoName = entry.getKey();
            logger.info("repoName = " + repoName);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> files = (List<Map<String, Object>>)entry.getValue();
            for(Map<String, Object> file: files) {
                String path = (String)file.get(Constants.PATH);
                String match = (String)file.get(Constants.MATCH);
                logger.info("upgrade path {} on match {}", path, match);
                RegexReplacement rr = new RegexReplacement(match, oldVersion, newVersion);
                Path upgradePath = Paths.get(userHome, workspace, repoName, path);
                // replace old version with new version here.
                try (Stream<String> lines = Files.lines(upgradePath)) {
                    List<String> replaced = lines
                            .map(rr::replace)
                            .collect(Collectors.toList());
                    Files.write(upgradePath, replaced);
                }
            }
        }
        return result;
    }
    */
}
