package com.networknt.bot.release;

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

public class ReleaseCommand implements Command {
    private static final Logger logger = LoggerFactory.getLogger(ReleaseCommand.class);
    private static final String CONFIG_NAME = "release";
    private Map<String, Object> config = Config.getInstance().getJsonMapConfig(CONFIG_NAME);
    private String workspace = (String)config.get(Constants.WORKSPACE);

    @SuppressWarnings("unchecked")
    private Map<String, Object> checkout = (Map<String, Object>)config.get(Constants.CHECKOUT);
    private String branch = (String)checkout.get(Constants.BRANCH);
    @SuppressWarnings("unchecked")
    private List<String> repositories = (List<String>)checkout.get(Constants.REPOSITORY);
    @SuppressWarnings("unchecked")
    private List<String> releases = (List<String>)config.get(Constants.RELEASE);
    private String userHome = System.getProperty("user.home");

    @Override
    public String getName() {
        return "release";
    }

    @Override
    public int execute() throws IOException, InterruptedException {
        int result = checkout();
        if(result != 0) return result;
        result = release();
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

    private int release() throws IOException, InterruptedException {
        int result = 0;
        for(String release: releases) {
            Path rPath = Paths.get(userHome, workspace, release);
            MergeMasterCmd mergeMasterCmd = new MergeMasterCmd(rPath);
            result = mergeMasterCmd.execute();
            if(result != 0) break;

            MavenReleaseCmd mavenReleaseCmd = new MavenReleaseCmd(rPath);
            result = mavenReleaseCmd.execute();
            if(result != 0) break;
        }
        return result;
    }
}
