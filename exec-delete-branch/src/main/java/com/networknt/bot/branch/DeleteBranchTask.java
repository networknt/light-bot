package com.networknt.bot.branch;

import com.networknt.bot.core.Command;
import com.networknt.bot.core.Constants;
import com.networknt.bot.core.cmd.*;
import com.networknt.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Delete a branch locally and remotely from the workspace. This task is used to delete a branch after
 * it is used. For example, a patch branch is created from a particular tag for a patch release, then
 * it should be removed after the patch is released for another patch release from a new tag in the future.
 *
 * @author Steve Hu
 */
public class DeleteBranchTask implements Command {
    private static final Logger logger = LoggerFactory.getLogger(DeleteBranchTask.class);
    private static final String CONFIG_NAME = "delete-branch";
    private final Map<String, Object> config = Config.getInstance().getJsonMapConfig(CONFIG_NAME);
    private final String workspace = (String)config.get(Constants.WORKSPACE);
    private final String branch = (String)config.get(Constants.BRANCH);
    private final boolean skipCheckout = (Boolean)config.get(Constants.SKIP_CHECKOUT);
    private final boolean skipLocal = (Boolean)config.get(Constants.SKIP_LOCAL);
    private final boolean skipRemote = (Boolean)config.get(Constants.SKIP_REMOTE);

    @SuppressWarnings("unchecked")
    private final List<Map<String, Object>> checkout = (List<Map<String, Object>>)config.get(Constants.CHECKOUT);
    @SuppressWarnings("unchecked")
    private final String userHome = System.getProperty("user.home");

    @Override
    public String getName() {
        return "delete-branch";
    }

    @Override
    public int execute() throws IOException, InterruptedException {
        int result = checkout();
        if(result != 0) return result;
        result = deleteLocalBranch();
        if(result != 0) return result;
        result = deleteRemoteBranch();
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


    private int deleteLocalBranch() throws IOException, InterruptedException {
        int result = 0;
        if(skipLocal) return result;

        // iterate over each group of repositories using the same branch name
        for(Map<String, Object> repoGroup : checkout) {
            // get the branch and the list of repositories
            List<String> repositories = (List<String>) repoGroup.get(Constants.REPOSITORY);

            for(String repository: repositories) {
                Path rPath = getRepositoryPath(userHome, workspace, getDirFromRepo(repository));
                // delete local branch
                DeleteLocalBranchCmd deleteLocalBranchCmd = new DeleteLocalBranchCmd(rPath, branch);
                result = deleteLocalBranchCmd.execute();
                if(result != 0) break;
            }
        }
        return result;
    }

    private int deleteRemoteBranch() throws IOException, InterruptedException {
        int result = 0;
        if(skipRemote) return result;

        // iterate over each group of repositories using the same branch name
        for(Map<String, Object> repoGroup : checkout) {
            // get the branch and the list of repositories
            List<String> repositories = (List<String>) repoGroup.get(Constants.REPOSITORY);

            for(String repository: repositories) {
                Path rPath = getRepositoryPath(userHome, workspace, getDirFromRepo(repository));
                // delete remote branch from git server
                DeleteRemoteBranchCmd deleteRemoteBranchCmd = new DeleteRemoteBranchCmd(rPath, branch);
                result = deleteRemoteBranchCmd.execute();
                if(result != 0) break;
            }
        }
        return result;
    }
}
