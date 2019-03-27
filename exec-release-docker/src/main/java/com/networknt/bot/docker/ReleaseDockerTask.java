package com.networknt.bot.docker;

import com.networknt.bot.core.Command;
import com.networknt.bot.core.Constants;
import com.networknt.bot.core.RegexReplacement;
import com.networknt.bot.core.cmd.*;
import com.networknt.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReleaseDockerTask implements Command {
    private static final Logger logger = LoggerFactory.getLogger(ReleaseDockerTask.class);
    private static final String CONFIG_NAME = "release-docker";
    private Map<String, Object> config = Config.getInstance().getJsonMapConfig(CONFIG_NAME);
    private String workspace = (String)config.get(Constants.WORKSPACE);
    private String version = (String)config.get(Constants.VERSION);
    private String organization = (String)config.get(Constants.ORGANIZATION);
    private String prevTag = (String)config.get(Constants.PREV_TAG);
    private int last = (Integer)config.get(Constants.LAST);
    private boolean skipCheckout = (Boolean)config.get(Constants.SKIP_CHECKOUT);
    private boolean skipMaven = (Boolean)config.get(Constants.SKIP_MAVEN);
    private boolean skipChangeLog = (Boolean)config.get(Constants.SKIP_CHANGE_LOG);
    private boolean skipCheckin = (Boolean)config.get(Constants.SKIP_CHECKIN);
    private boolean skipReleaseNote = (Boolean)config.get(Constants.SKIP_RELEASE_NOTE);
    private boolean skipDocker = (Boolean)config.get(Constants.SKIP_DOCKER);
    private String branch = null; // this value is populated in the checkout step.

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> checkout = (List<Map<String, Object>>)config.get(Constants.CHECKOUT);
    @SuppressWarnings("unchecked")
    private List<String> mavens = (List<String>)config.get(Constants.MAVEN);
    @SuppressWarnings("unchecked")
    private List<String> releases = (List<String>)config.get(Constants.RELEASE);
    @SuppressWarnings("unchecked")
    private List<String> dockers = (List<String>)config.get(Constants.DOCKER);
    private String userHome = System.getProperty("user.home");

    @Override
    public String getName() {
        return "release-docker";
    }

    @Override
    public int execute() throws IOException, InterruptedException {
        int result = checkout();
        if(result != 0) return result;
        result = maven();
        if(result != 0) return result;
        result = changeLog();
        if(result != 0) return result;
        result = checkin();
        if(result != 0) return result;
        result = releaseNote();
        if(result != 0) return result;
        result = docker();
        return result;
    }

    private int checkout() throws IOException, InterruptedException {
        int result = 0;
        if(skipCheckout) return result;

        // check if there is a directory workspace in home directory.
        Path wPath = Paths.get(userHome, workspace);
        if(Files.notExists(wPath)) {
            Files.createDirectory(wPath);
        }

        // iterate over each group of repositories using the same branch name
        for(Map<String, Object> repoGroup : checkout) {
            // get the branch and the list of repositories
            branch = (String) repoGroup.get(Constants.BRANCH);
            List<String> repositories = (List<String>) repoGroup.get(Constants.REPOSITORY);

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
        }

        return result;
    }

    private int maven() throws IOException, InterruptedException {
        int result = 0;
        if(skipMaven) return result;

        for(String maven: mavens) {
            Path rPath = Paths.get(userHome, workspace, maven);
            MavenBuildCmd mavenBuildCmd = new MavenBuildCmd(rPath);
            result = mavenBuildCmd.execute();
            if(result != 0) break;
        }
        return result;
    }

    private int changeLog() throws IOException, InterruptedException {
        int result = 0;
        if(skipChangeLog) return result;

        for(String release: releases) {
            Path rPath = Paths.get(userHome, workspace, release);
            // generate changelog.md, check in
            ChangeLogCmd changeLogCmd = new ChangeLogCmd(organization, release, version, branch, prevTag, last, rPath);
            result = changeLogCmd.execute();
            if(result != 0) break;
        }
        return result;
    }

    private int checkin() throws IOException, InterruptedException {
        int result = 0;
        if(skipCheckin) return result;

        for(String release: releases) {
            Path rPath = Paths.get(userHome, workspace, release);
            // check in the generated CHANGELOG.md to the branch
            CheckinBranchCmd checkinBranchCmd = new CheckinBranchCmd(branch, rPath, "light-bot checkin CHANGELOG.md");
            result = checkinBranchCmd.execute();
            if(result != 0) break;
        }
        return result;
    }

    private int releaseNote() throws IOException, InterruptedException {
        int result = 0;
        if(skipReleaseNote) return result;

        for(String release: releases) {
            Path rPath = Paths.get(userHome, workspace, release);

            // read CHANGELOG.md for the current release body.
            Charset charset = Charset.forName("UTF-8");
            Path file = Paths.get(userHome, workspace, release, "CHANGELOG.md");
            StringBuffer stringBuffer = new StringBuffer();
            try (BufferedReader reader = Files.newBufferedReader(file, charset)) {
                String line;
                boolean tokenFound = false;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("## [" + version)) {
                        tokenFound = true;
                    } else if (line.startsWith("## [")) {
                        break;
                    }
                    if(tokenFound) {
                        stringBuffer.append(line);
                        stringBuffer.append("\n");
                    }
                }
            } catch (IOException e) {
                logger.error("IOException:", e);
            }

            // call github api to create a new release.
            GithubReleaseCmd githubReleaseCmd = new GithubReleaseCmd(organization, release, version, stringBuffer.toString(), rPath);
            result = githubReleaseCmd.execute();
            if(result != 0) break;
        }
        return result;
    }

    private int docker() throws IOException, InterruptedException {
        int result = 0;
        if(skipDocker) return result;

        for(String docker: dockers) {
            Path rPath = Paths.get(userHome, workspace, docker);
            DockerBuildCmd dockerBuildCmd = new DockerBuildCmd(version, rPath);
            result = dockerBuildCmd.execute();
            if(result != 0) break;
        }
        return result;
    }

}
