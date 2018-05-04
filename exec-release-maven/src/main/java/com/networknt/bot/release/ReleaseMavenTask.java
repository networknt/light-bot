package com.networknt.bot.release;

import com.networknt.bot.core.Command;
import com.networknt.bot.core.Constants;
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

public class ReleaseMavenTask implements Command {
    private static final Logger logger = LoggerFactory.getLogger(ReleaseMavenTask.class);
    private static final String CONFIG_NAME = "release-maven";
    private Map<String, Object> config = Config.getInstance().getJsonMapConfig(CONFIG_NAME);
    private String workspace = (String)config.get(Constants.WORKSPACE);
    private String version = (String)config.get(Constants.VERSION);
    private String organization = (String)config.get(Constants.ORGANIZATION);
    private boolean skipCheckout = (Boolean)config.get(Constants.SKIP_CHECKOUT);
    private boolean skipMerge = (Boolean)config.get(Constants.SKIP_MERGE);
    private boolean skipRelease = (Boolean)config.get(Constants.SKIP_RELEASE);

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> checkout = (List<Map<String, Object>>)config.get(Constants.CHECKOUT);
    @SuppressWarnings("unchecked")
    private List<String> releases = (List<String>)config.get(Constants.RELEASE);
    private String userHome = System.getProperty("user.home");

    @Override
    public String getName() {
        return "release-maven";
    }

    @Override
    public int execute() throws IOException, InterruptedException {
        int result = checkout();
        if(result != 0) return result;
        result = merge();
        if(result != 0) return result;
        result = release();
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
            String branch = (String) repoGroup.get(Constants.BRANCH);
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


    private int merge() throws IOException, InterruptedException {
        int result = 0;
        if(skipMerge) return result;

        // iterate over each group of repositories using the same branch name
        for(Map<String, Object> repoGroup : checkout) {
            // get the branch and the list of repositories
            List<String> repositories = (List<String>) repoGroup.get(Constants.REPOSITORY);

            for(String repository: repositories) {
                Path rPath = Paths.get(userHome, workspace, getDirFromRepo(repository));
                // merge current branch to master and check in
                MergeMasterCmd mergeMasterCmd = new MergeMasterCmd(rPath);
                result = mergeMasterCmd.execute();
                if(result != 0) break;
            }
        }
        return result;
    }

    private int release() throws IOException, InterruptedException {
        int result = 0;
        if(skipRelease) return result;

        for(String release: releases) {
            Path rPath = Paths.get(userHome, workspace, release);

            // generate changelog.md, check in
            GenChangeLogCmd genChangeLogCmd = new GenChangeLogCmd(organization, release, version, rPath);
            result = genChangeLogCmd.execute();
            if(result != 0) break;

            // run maven release plugin to release to maven central
            MavenReleaseCmd mavenReleaseCmd = new MavenReleaseCmd(rPath);
            result = mavenReleaseCmd.execute();
            if(result != 0) break;

            // merge the changelog.md to develop and push
            MergeDevelopCmd mergeDevelopCmd = new MergeDevelopCmd(rPath);
            result = mergeDevelopCmd.execute();
            if(result != 0) break;

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
                        line = line.replace("'", "\\'");
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
}
