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
    private final String masterBranch = (String)config.get(Constants.MASTER_BRANCH);
    private final String syncBranch = (String)config.get(Constants.SYNC_BRANCH);
    private final boolean skipExternalMasterCheckout = (Boolean)config.get(Constants.SKIP_EXTERNAL_MASTER_CHECKOUT);
    private final boolean skipInternalMasterPush = (Boolean)config.get(Constants.SKIP_INTERNAL_MASTER_PUSH);
    private final boolean skipInternalSyncPush = (Boolean)config.get(Constants.SKIP_INTERNAL_SYNC_PUSH);
    private final boolean skipExternalSyncPush = (Boolean)config.get(Constants.SKIP_EXTERNAL_SYNC_PUSH);
    private final boolean skipSyncMasterMerge = (Boolean)config.get(Constants.SKIP_SYNC_MASTER_MERGE);
    private final boolean skipExternalMasterPush = (Boolean)config.get(Constants.SKIP_EXTERNAL_MASTER_PUSH);
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
        // Create workspace if it doesn't exist.
        // Clone from the GitHub with externalOrigin (origin) and switch to masterBranch (master).
        // If repository already exists, switch to masterBranch and pull from the externalOrigin(origin).
        // This will make sure that the local repository is up to date with the remote repository in master branch.
        int result = externalMasterCheckout();
        if(result != 0) return result;

        // Create remote internalOrigin (internal) if not exist.
        // Push to the internal Git server with internalOrigin (internal) and masterBranch (master).
        // This will make sure that the internal master branch is synced from the external master branch.
        result = internalMasterPush();
        if(result != 0) return result;

        // Create remote internalOrigin (internal) if not exist.
        // Pull and Push to the internal Git server with internalOrigin (internal) and syncBranch (sync).
        result = internalSyncPush();
        if(result != 0) return result;

        // Pull and push syncBranch (sync) to externalOrigin (origin).
        result = externalSyncPush();
        if(result != 0) return result;

        // Merge from the syncBranch (sync) to masterBranch (master) before pushing the external master branch.
        result = syncMasterMerge();
        if(result != 0) return result;

        // Push masterBranch to the external Git server with externalOrigin (origin).
        result = externalMasterPush();
        return result;
    }

    // clone or pull from the GitHub repository master branch to the workspace directory.
    private int externalMasterCheckout() throws IOException, InterruptedException {
        int result = 0;
        if(skipExternalMasterCheckout) return result;

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
                // clone and switch to the external branch.
                CloneBranchCmd cloneBranchCmd = new CloneBranchCmd(repository, masterBranch, wPath, rPath);
                result = cloneBranchCmd.execute();
            } else {
                if(logger.isTraceEnabled()) logger.trace("rPath exists, pull the repository");
                // switch to masterBranch (master) and pull from the remote.
                CheckoutPullCmd checkoutPullCmd = new CheckoutPullCmd(externalOrigin, masterBranch, rPath);
                result = checkoutPullCmd.execute();
            }
            if(result != 0) break;
        }
        return result;
    }

    /*
    private int externalSyncCheckout() throws IOException, InterruptedException {
        int result = 0;
        if(skipExternalSyncCheckout) return result;

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
                // clone and switch to the external branch.
                CloneBranchCmd cloneBranchCmd = new CloneBranchCmd(repository, syncBranch, wPath, rPath);
                result = cloneBranchCmd.execute();
            } else {
                if(logger.isTraceEnabled()) logger.trace("rPath exists, pull the repository");
                // switch to masterBranch (master) and pull from the remote.
                CheckoutPullCmd checkoutPullCmd = new CheckoutPullCmd(externalOrigin, syncBranch, rPath);
                result = checkoutPullCmd.execute();
            }
            if(result != 0) break;
        }
        return result;
    }
    */

    // merge from the sync branch to master branch
    private int syncMasterMerge() throws IOException, InterruptedException {
        int result = 0;
        if(skipSyncMasterMerge) return result;

        // iterate over each internal repository to merge from the master to sync branch
        // if the sync branch doesn't exist, create it.
        for(String repository: internalRepo) {
            Path rPath = getRepositoryPath(userHome, workspace, getDirFromRepo(repository));
            if(logger.isTraceEnabled()) logger.trace("Merge from external master branch {} to internal master branch {}.", syncBranch, masterBranch);
            MergeBranchCmd mergeBranchCmd = new MergeBranchCmd(rPath, syncBranch, masterBranch);
            result = mergeBranchCmd.execute();
            if(result != 0) break;
        }
        return result;
    }

    // Create another remote to the internal Git server.
    // And push to the internal server with internalOrigin (internal) and syncBranch (sync).
    private int internalSyncPush() throws IOException, InterruptedException {
        int result = 0;
        if(skipInternalSyncPush) return result;

        // iterate over each internal repository
        for(String repository: internalRepo) {
            Path rPath = getRepositoryPath(userHome, workspace, getDirFromRepo(repository));
            // check if remote to the internal and create it if it doesn't exist.
            RemotePushCmd remotePushCmd = new RemotePushCmd(rPath, internalOrigin, syncBranch, repository);
            result = remotePushCmd.execute();
            if(result != 0) break;
        }
        return result;
    }

    // create another remote to the internal Git server and push to the internal server with internalOrigin and masterBranch.
    private int internalMasterPush() throws IOException, InterruptedException {
        int result = 0;
        if(skipInternalMasterPush) return result;

        // iterate over each internal repository
        for(String repository: internalRepo) {
            Path rPath = getRepositoryPath(userHome, workspace, getDirFromRepo(repository));
            // check if remote to the internal and create it if it doesn't exist.
            RemotePushCmd remotePushCmd = new RemotePushCmd(rPath, internalOrigin, masterBranch, repository);
            result = remotePushCmd.execute();
            if(result != 0) break;
        }
        return result;
    }

    // Push to the internal server with externalOrigin and syncBranch.
    private int externalSyncPush() throws IOException, InterruptedException {
        int result = 0;
        if(skipExternalSyncPush) return result;

        // iterate over each internal repository
        for(String repository: externalRepo) {
            Path rPath = getRepositoryPath(userHome, workspace, getDirFromRepo(repository));
            // check if remote to the internal and create it if it doesn't exist.
            RemotePushCmd remotePushCmd = new RemotePushCmd(rPath, externalOrigin, syncBranch, repository);
            result = remotePushCmd.execute();
            if(result != 0) break;
        }
        return result;
    }

    // Push masterBranch (master) to the externalOrigin (origin) after merging from the sync branch.
    private int externalMasterPush() throws IOException, InterruptedException {
        int result = 0;
        if(skipExternalMasterPush) return result;

        // iterate over each internal repository
        for(String repository: externalRepo) {
            Path rPath = getRepositoryPath(userHome, workspace, getDirFromRepo(repository));
            // check if remote to the internal and create it if it doesn't exist.
            RemotePushCmd remotePushCmd = new RemotePushCmd(rPath, externalOrigin, masterBranch, repository);
            result = remotePushCmd.execute();
            if(result != 0) break;
        }
        return result;
    }

}
