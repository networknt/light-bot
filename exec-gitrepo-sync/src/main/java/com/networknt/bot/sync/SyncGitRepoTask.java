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
    private final boolean skipExternalMasterCheckout = config.get(Constants.SKIP_EXTERNAL_MASTER_CHECKOUT) != null ? (Boolean)config.get(Constants.SKIP_EXTERNAL_MASTER_CHECKOUT) : false;
    private final boolean skipInternalMasterPush = config.get(Constants.SKIP_INTERNAL_MASTER_PUSH) != null ? (Boolean)config.get(Constants.SKIP_INTERNAL_MASTER_PUSH) : false;
    private final boolean skipExternalSyncPush = config.get(Constants.SKIP_EXTERNAL_SYNC_PUSH) != null ? (Boolean)config.get(Constants.SKIP_EXTERNAL_SYNC_PUSH) : false;
    private final boolean skipBranchPruning = config.get(Constants.SKIP_BRANCH_PRUNING) != null ? (Boolean)config.get(Constants.SKIP_BRANCH_PRUNING) : false;
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
        int result = externalMasterCheckout();
        if(result != 0) return result;

        // Create remote internalOrigin (internal) if not exist.
        // Push to the internal Git server with internalOrigin (internal) and masterBranch (master).
        result = internalMasterPush();
        if(result != 0) return result;

        // Prune sync branch if it is fully merged into master
        result = pruneMergedSyncBranch();
        if(result != 0) return result;

        // Sync sync branch from internal Git server to external Git server (GitHub)
        result = externalSyncPush();
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

    // Prune syncBranch on internalOrigin if it is already merged into externalOrigin's master
    private int pruneMergedSyncBranch() throws IOException, InterruptedException {
        int result = 0;
        if(skipBranchPruning || syncBranch == null || syncBranch.isEmpty()) return result;

        for(String repository: internalRepo) {
            Path rPath = getRepositoryPath(userHome, workspace, getDirFromRepo(repository));
            
            GetRemoteBranchesCmd getBranchesCmd = new GetRemoteBranchesCmd(rPath, internalOrigin, "");
            result = getBranchesCmd.execute();
            if(result != 0) break;

            List<String> branches = getBranchesCmd.getBranches();
            if (branches.contains(syncBranch)) {
                // Need to fetch both remotes to make sure we have the latest commits locally for merge-base check
                FetchCmd fetchInternal = new FetchCmd(rPath, internalOrigin);
                result = fetchInternal.execute();
                if (result != 0) break;

                FetchCmd fetchExternal = new FetchCmd(rPath, externalOrigin);
                result = fetchExternal.execute();
                if (result != 0) break;

                // Check if branch is merged into externalOrigin/masterBranch
                String featureRef = "refs/remotes/" + internalOrigin + "/" + syncBranch;
                String targetRef = "refs/remotes/" + externalOrigin + "/" + masterBranch;
                IsBranchMergedCmd isMergedCmd = new IsBranchMergedCmd(rPath, featureRef, targetRef);
                int isMergedResult = isMergedCmd.execute();
                
                if (isMergedResult == 0) {
                    logger.info("Branch {} is merged into master. Pruning from internal origin...", syncBranch);
                    DeleteRemoteBranchCmd deleteCmd = new DeleteRemoteBranchCmd(rPath, internalOrigin, syncBranch);
                    result = deleteCmd.execute();
                    if (result != 0) break;
                }
            }
            if (result != 0) break;
        }
        return result;
    }

    // Push sync branch from internal server to the externalOrigin
    private int externalSyncPush() throws IOException, InterruptedException {
        int result = 0;
        if(skipExternalSyncPush || syncBranch == null || syncBranch.isEmpty()) return result;

        // iterate over each external repository to push the matching branch from internal
        for(String repository: externalRepo) {
            Path rPath = getRepositoryPath(userHome, workspace, getDirFromRepo(repository));
            
            GetRemoteBranchesCmd getBranchesCmd = new GetRemoteBranchesCmd(rPath, internalOrigin, "");
            result = getBranchesCmd.execute();
            if(result != 0) break;

            List<String> branches = getBranchesCmd.getBranches();
            if (branches.contains(syncBranch)) {
                // Fetch latest from internal
                FetchCmd fetchInternal = new FetchCmd(rPath, internalOrigin);
                result = fetchInternal.execute();
                if (result != 0) break;

                // Checkout and pull the sync branch from internal
                CheckoutPullCmd checkoutPullCmd = new CheckoutPullCmd(internalOrigin, syncBranch, rPath);
                result = checkoutPullCmd.execute();
                if (result != 0) break;
                
                // Push to external
                RemotePushCmd remotePushCmd = new RemotePushCmd(rPath, externalOrigin, syncBranch, repository);
                result = remotePushCmd.execute();
                if(result != 0) break;
            }
            
            // Switch back to master after processing sync branch
            CheckoutPullCmd checkoutMaster = new CheckoutPullCmd(externalOrigin, masterBranch, rPath);
            checkoutMaster.execute();
            
            if(result != 0) break;
        }
        return result;
    }
}
