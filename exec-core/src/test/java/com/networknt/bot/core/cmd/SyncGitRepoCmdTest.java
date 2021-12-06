package com.networknt.bot.core.cmd;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SyncGitRepoCmdTest {
    private String userHome = System.getProperty("user.home");
    private String workspace = "syncgitrepo";

    @Test
    public void testCheckGogsRepoCmd() throws IOException, InterruptedException {
        Path rPath = Paths.get(userHome, workspace);
        String source = "https://git.lightapi.net";
        String target = "https://api.github.com";
        String organization = "networknt";
        String repository = "light-mq.git";
        String gogsToken = "c18cf5ed6c8466c35e5f5f8827912743b9880dff";
        SyncGitRepoCmd cmd = new SyncGitRepoCmd(rPath, source, target, organization, repository);
        String repos = cmd.getGogsRepos(source, organization, gogsToken);
        Assert.assertNotNull(repos);
        boolean exist = cmd.repoExist(repos, repository.substring(0, repository.indexOf(".")));
        Assert.assertTrue(exist);
    }

    @Test
    public void testCheckGithubRepoCmd() throws IOException, InterruptedException {
        Path rPath = Paths.get(userHome, workspace);
        String source = "https://git.lightapi.net";
        String target = "https://api.github.com";
        String organization = "networknt";
        String repository = "light-mq.git";
        String githubToken = "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";
        SyncGitRepoCmd cmd = new SyncGitRepoCmd(rPath, source, target, organization, repository);
        String repos = cmd.getGithubRepos(target, organization, githubToken);
        Assert.assertNotNull(repos);
        boolean exist = cmd.repoExist(repos, repository.substring(0, repository.indexOf(".")));
        Assert.assertFalse(exist);
    }

}
