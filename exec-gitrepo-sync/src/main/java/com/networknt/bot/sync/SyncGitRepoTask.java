package com.networknt.bot.sync;

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

public class SyncGitRepoTask implements Command {
    private static final Logger logger = LoggerFactory.getLogger(SyncGitRepoTask.class);
    private static final String CONFIG_NAME = "sync-gitrepo";
    private final Map<String, Object> config = Config.getInstance().getJsonMapConfig(CONFIG_NAME);
    private final String workspace = (String)config.get(Constants.WORKSPACE);
    private final String externalOrigin = (String)config.get(Constants.EXTERNAL_ORIGIN);
    private final String internalOrigin = (String)config.get(Constants.INTERNAL_ORIGIN);
    private final String externalBranch = (String)config.get(Constants.EXTERNAL_BRANCH);
    private final String internalBranch = (String)config.get(Constants.INTERNAL_BRANCH);
    private final boolean skipExternalCheckout = (Boolean)config.get(Constants.SKIP_EXTERNAL_CHECKOUT);
    private final boolean skipInternalPush = (Boolean)config.get(Constants.SKIP_INTERNAL_PUSH);
    private final boolean skipInternalMerge = (Boolean)config.get(Constants.SKIP_INTERNAL_MERGE);
    private final boolean skipInternalPull = (Boolean)config.get(Constants.SKIP_INTERNAL_PULL);
    private final boolean skipExternalPush = (Boolean)config.get(Constants.SKIP_EXTERNAL_PUSH);
    @SuppressWarnings("unchecked")
    private final List<String> externalRepo = (List<String>)config.get(Constants.EXTERNAL_REPO);
    @SuppressWarnings("unchecked")
    private final List<String> internalRepo = (List<String>)config.get(Constants.INTERNAL_REPO);
    @SuppressWarnings("unchecked")
    private final String userHome = System.getProperty("user.home");

    @Override
    public String getName() {
        return "sync-gitrepo";
    }

    @Override
    public int execute() throws IOException, InterruptedException {
        int result = externalCheckout();  // create workspace if it doesn't exist. clone or pull from the GitHub with externalOrigin (origin) and externalBranch (master).
        if(result != 0) return result;
        result = internalMerge(); // merge from the externalBranch (master) to the internalBranch (sync) on the internal Git server.
        if(result != 0) return result;
        result = internalPushInternalBranch(); // create remote internalOrigin if not exist. Push to the internal Git server with internalOrigin (internal) and internalBranch (sync).
        if(result != 0) return result;
        result = internalPushExternalBranch(); // create remote internalOrigin if not exist. Push to the internal Git server with internalOrigin (internal) and externalBranch (master).
        if(result != 0) return result;
        result = internalPullInternalBranch(); // Pull from the internal Git server with internalOrigin (internal) and internalBranch (sync).
        if(result != 0) return result;
        result = externalPushInternalBranch(); // Push to the internal Git server with externalOrigin (origin) and externalBranch (sync).
        return result;
    }

    // clone or pull from the GitHub repository
    private int externalCheckout() throws IOException, InterruptedException {
        int result = 0;
        if(skipExternalCheckout) return result;

        // check if there is a directory workspace in home directory.
        Path wPath = getWorkspacePath(userHome, workspace);
        if(Files.notExists(wPath)) {
            Files.createDirectory(wPath);
            if(logger.isTraceEnabled()) logger.trace("Create workspace directory " + wPath);
        }

        // iterate all the repos from the list of externalRepo.
        for(String repository : externalRepo) {
            Path rPath = getRepositoryPath(userHome, workspace, getDirFromRepo(repository));
            if (logger.isTraceEnabled()) logger.trace("rPath = " + rPath);
            if (Files.notExists(rPath)) {
                if(logger.isTraceEnabled()) logger.trace("rPath does not exist, clone the repository");
                // clone and switch to branch.
                CloneBranchCmd cloneBranchCmd = new CloneBranchCmd(repository, externalBranch, wPath, rPath);
                result = cloneBranchCmd.execute();
            } else {
                if(logger.isTraceEnabled()) logger.trace("rPath exists, pull the repository");
                // switch to externalBranch and pull from the remote.
                CheckoutPullCmd checkoutPullCmd = new CheckoutPullCmd(externalOrigin, externalBranch, rPath);
                result = checkoutPullCmd.execute();
            }
            if(result != 0) break;
        }
        return result;
    }

    // merge from the external branch to the internal branch on the internal Git server
    private int internalMerge() throws IOException, InterruptedException {
        int result = 0;
        if(skipInternalMerge) return result;

        // iterate over each internal repository to merge from the master to sync branch
        // if the sync branch doesn't exist, create it.
        for(String repository: internalRepo) {
            Path rPath = getRepositoryPath(userHome, workspace, getDirFromRepo(repository));
            if(logger.isTraceEnabled()) logger.trace("Merge from external branch {} to internal branch {}.", externalBranch, internalBranch);
            MergeBranchCmd mergeBranchCmd = new MergeBranchCmd(rPath, externalBranch, internalBranch);
            result = mergeBranchCmd.execute();
            if(result != 0) break;
        }
        return result;
    }

    // create another remote to the internal Git server and push to the internal server with internalOrigin and internalBranch.
    private int internalPushInternalBranch() throws IOException, InterruptedException {
        int result = 0;
        if(skipInternalPush) return result;

        // iterate over each internal repository
        for(String repository: internalRepo) {
            Path rPath = getRepositoryPath(userHome, workspace, getDirFromRepo(repository));
            // check if remote to the internal and create it if it doesn't exist.
            RemotePushCmd remotePushCmd = new RemotePushCmd(rPath, internalOrigin, internalBranch, repository);
            result = remotePushCmd.execute();
            if(result != 0) break;
        }
        return result;
    }

    // create another remote to the internal Git server and push to the internal server with internalOrigin and externalBranch.
    private int internalPushExternalBranch() throws IOException, InterruptedException {
        int result = 0;
        if(skipInternalPush) return result;

        // iterate over each internal repository
        for(String repository: internalRepo) {
            Path rPath = getRepositoryPath(userHome, workspace, getDirFromRepo(repository));
            // check if remote to the internal and create it if it doesn't exist.
            RemotePushCmd remotePushCmd = new RemotePushCmd(rPath, internalOrigin, externalBranch, repository);
            result = remotePushCmd.execute();
            if(result != 0) break;
        }
        return result;
    }

    // Pull from the internal Git server with internalOrigin (internal) and internalBranch (sync).
    private int internalPullInternalBranch() throws IOException, InterruptedException {
        int result = 0;
        if(skipInternalPull) return result;

        // iterate over each internal repository
        for(String repository: internalRepo) {
            Path rPath = getRepositoryPath(userHome, workspace, getDirFromRepo(repository));
            // check if remote to the internal and create it if it doesn't exist.
            CheckoutPullCmd checkoutPullCmd = new CheckoutPullCmd(internalOrigin, internalBranch, rPath);
            result = checkoutPullCmd.execute();
            if(result != 0) break;
        }
        return result;
    }

    // Push to the internal server with externalOrigin and internalBranch.
    private int externalPushInternalBranch() throws IOException, InterruptedException {
        int result = 0;
        if(skipExternalPush) return result;

        // iterate over each internal repository
        for(String repository: externalRepo) {
            Path rPath = getRepositoryPath(userHome, workspace, getDirFromRepo(repository));
            // check if remote to the internal and create it if it doesn't exist.
            RemotePushCmd remotePushCmd = new RemotePushCmd(rPath, externalOrigin, internalBranch, repository);
            result = remotePushCmd.execute();
            if(result != 0) break;
        }
        return result;
    }

}
