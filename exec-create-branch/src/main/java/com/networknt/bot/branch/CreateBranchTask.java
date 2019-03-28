package com.networknt.bot.branch;

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

public class CreateBranchTask implements Command {
    private static final Logger logger = LoggerFactory.getLogger(CreateBranchTask.class);
    private static final String CONFIG_NAME = "create-branch";
    private Map<String, Object> config = Config.getInstance().getJsonMapConfig(CONFIG_NAME);
    private String workspace = (String)config.get(Constants.WORKSPACE);
    private String branch = (String)config.get(Constants.BRANCH);
    private boolean fromTag = (Boolean)config.get(Constants.FROM_TAG);
    private String tag = (String)config.get(Constants.TAG);
    private boolean skipCheckout = (Boolean)config.get(Constants.SKIP_CHECKOUT);
    private boolean skipBranch = (Boolean)config.get(Constants.SKIP_BRANCH);
    private boolean skipPush = (Boolean)config.get(Constants.SKIP_PUSH);

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> checkout = (List<Map<String, Object>>)config.get(Constants.CHECKOUT);
    @SuppressWarnings("unchecked")
    private String userHome = System.getProperty("user.home");

    @Override
    public String getName() {
        return "create-branch";
    }

    @Override
    public int execute() throws IOException, InterruptedException {
        int result = checkout();
        if(result != 0) return result;
        result = createBranch();
        if(result != 0) return result;
        result = pushBranch();
        return result;
    }

    private int checkout() throws IOException, InterruptedException {
        int result = 0;
        if(skipCheckout) return result;

        // check if there is a directory workspace in home directory.
        Path wPath = getWorkspacePath(userHome, workspace);
        if(Files.notExists(wPath)) {
            Files.createDirectory(wPath);
        }

        // iterate over each group of repositories using the same branch name
        for(Map<String, Object> repoGroup : checkout) {
            // get the branch and the list of repositories
            String branch = (String) repoGroup.get(Constants.BRANCH);
            List<String> repositories = (List<String>) repoGroup.get(Constants.REPOSITORY);
            for(String repository: repositories) {
                Path rPath = getRepositoryPath(userHome, workspace, getDirFromRepo(repository));
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


    private int createBranch() throws IOException, InterruptedException {
        int result = 0;
        if(skipBranch) return result;

        // iterate over each group of repositories using the same branch name
        for(Map<String, Object> repoGroup : checkout) {
            // get the branch and the list of repositories
            List<String> repositories = (List<String>) repoGroup.get(Constants.REPOSITORY);

            for(String repository: repositories) {
                Path rPath = getRepositoryPath(userHome, workspace, getDirFromRepo(repository));
                // create branch
                CreateBranchCmd createBranchCmd = new CreateBranchCmd(rPath, branch, fromTag? tag : null);
                result = createBranchCmd.execute();
                if(result != 0) break;
            }
        }
        return result;
    }

    private int pushBranch() throws IOException, InterruptedException {
        int result = 0;
        if(skipPush) return result;

        // iterate over each group of repositories using the same branch name
        for(Map<String, Object> repoGroup : checkout) {
            // get the branch and the list of repositories
            List<String> repositories = (List<String>) repoGroup.get(Constants.REPOSITORY);

            for(String repository: repositories) {
                Path rPath = getRepositoryPath(userHome, workspace, getDirFromRepo(repository));
                // push current branch to git
                PushBranchCmd pushBranchCmd = new PushBranchCmd(rPath, branch);
                result = pushBranchCmd.execute();
                if(result != 0) break;
            }
        }
        return result;
    }
}
