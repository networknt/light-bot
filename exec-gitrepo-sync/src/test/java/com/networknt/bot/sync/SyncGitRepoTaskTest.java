package com.networknt.bot.sync;

import com.networknt.bot.core.Constants;
import com.networknt.bot.core.Executor;
import com.networknt.config.Config;
import com.networknt.service.SingletonServiceFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SyncGitRepoTaskTest {
    private Executor executor;
    private Path tempDir;
    private Path externalBare;
    private Path internalBare;
    private Path externalClone;
    private Path internalClone;
    private Map<String, Object> configMap;
    private Map<String, Object> originalConfigMap;

    @BeforeEach
    public void setup() throws Exception {
        executor = SingletonServiceFactory.getBean(Executor.class);
        tempDir = Files.createTempDirectory("sync-git-test");
        
        Path externalParent = tempDir.resolve("external");
        Path internalParent = tempDir.resolve("internal");
        Files.createDirectories(externalParent);
        Files.createDirectories(internalParent);
        
        externalBare = externalParent.resolve("test-repo.git");
        internalBare = internalParent.resolve("test-repo.git");
        externalClone = tempDir.resolve("external-clone");
        internalClone = tempDir.resolve("internal-clone");
        
        // Init bare repos
        runCommand("git init --bare " + externalBare.toString());
        runCommand("git init --bare " + internalBare.toString());
        
        // Setup external with initial master commit
        runCommand("git clone " + externalBare.toString() + " " + externalClone.toString());
        Files.writeString(externalClone.resolve("README.md"), "Initial Master");
        runCommand(externalClone.toFile(), "git config user.email 'test@example.com'; git config user.name 'Test'; git add .; git commit -m 'initial'; git push origin master");
        
        // Setup internal repo (customer)
        runCommand("git clone " + internalBare.toString() + " " + internalClone.toString());
        runCommand(internalClone.toFile(), "git config user.email 'test@example.com'; git config user.name 'Test'");
        
        // Override config map directly
        configMap = Config.getInstance().getJsonMapConfig("sync-gitrepo");
        originalConfigMap = new java.util.HashMap<>(configMap);
        configMap.put(Constants.EXTERNAL_REPO, Arrays.asList("file://" + externalBare.toAbsolutePath().toString()));
        configMap.put(Constants.INTERNAL_REPO, Arrays.asList("file://" + internalBare.toAbsolutePath().toString()));
        configMap.put(Constants.WORKSPACE, tempDir.resolve("workspace").toAbsolutePath().toString());
        configMap.put(Constants.SKIP_BRANCH_PRUNING, false);
    }

    @AfterEach
    public void cleanup() {
        if (configMap != null && originalConfigMap != null) {
            configMap.clear();
            configMap.putAll(originalConfigMap);
        }
        deleteDirectory(tempDir.toFile());
    }

    private void runCommand(String cmd) throws Exception {
        runCommand(tempDir.toFile(), cmd);
    }

    private void runCommand(File dir, String cmd) throws Exception {
        List<String> commands = Arrays.asList("bash", "-c", cmd);
        int result = executor.execute(commands, dir);
        if (result != 0) {
            throw new RuntimeException("Command failed: " + cmd + "\n" + executor.getStderr());
        }
    }

    private boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

    @Test
    public void testSyncGitRepoTask_FullLifecycle() throws Exception {
        // Step 1: Run bot. It should mirror master from external to internal
        SyncGitRepoTask cmd = new SyncGitRepoTask();
        int result = cmd.execute();
        Assertions.assertEquals(0, result);
        
        // Verify internal bare repo now has master
        runCommand(internalClone.toFile(), "git pull origin master");
        Assertions.assertTrue(Files.exists(internalClone.resolve("README.md")));

        // Step 2: Customer creates a sync branch on internal and pushes
        runCommand(internalClone.toFile(), "git checkout -b sync; echo 'Feature' > feature.txt; git add .; git commit -m 'feat'; git push origin sync");
        
        // Step 3: Run bot. It should push sync from internal to external
        result = cmd.execute();
        Assertions.assertEquals(0, result);
        
        // Verify external bare repo now has sync branch
        runCommand(externalClone.toFile(), "git fetch origin sync; git checkout sync; git pull origin sync");
        Assertions.assertTrue(Files.exists(externalClone.resolve("feature.txt")));

        // Step 4: Simulate PR merge on GitHub (create a merge commit on external)
        runCommand(externalClone.toFile(), "git checkout master; git merge --no-ff sync -m 'Merge PR'; git push origin master");
        
        // Step 5: Run bot. It should sync master down, detect sync is merged, and delete sync from internal
        result = cmd.execute();
        Assertions.assertEquals(0, result);
        
        // Verify sync branch is deleted from internal bare repo
        runCommand(internalClone.toFile(), "git fetch --prune origin");
        
        // The following command should fail because the branch doesn't exist remotely anymore
        List<String> commands = Arrays.asList("bash", "-c", "git ls-remote --heads origin sync | grep sync");
        int lsResult = executor.execute(commands, internalClone.toFile());
        Assertions.assertNotEquals(0, lsResult, "Sync branch should have been pruned from internal remote");
    }
}
