package com.networknt.bot.core.cmd;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class ChangeLogCmdTest {
    private String userHome = System.getProperty("user.home");
    private String workspace = "releasemaven_2_0_x";
    private String repository = "light-4j";

    @Test
    @Ignore
    public void testLocalGitLog() throws IOException, InterruptedException {
        // run the 'ls -l' with GenericSingleCmd
        Path rPath = Paths.get(userHome, workspace, repository);
        ChangeLogCmd cmd = new ChangeLogCmd("networknt", "light-4j", "2.0.19", "master", "2.0.18", 100, rPath);
        String gitLog = cmd.getLocalGitLog();
        System.out.println(gitLog);
    }

    @Test
    @Ignore
    public void testPullRequest() throws IOException, InterruptedException {
        // run the 'ls -l' with GenericSingleCmd
        Path rPath = Paths.get(userHome, workspace, repository);
        ChangeLogCmd cmd = new ChangeLogCmd("networknt", "light-4j", "2.0.19", "master", "2.0.18", 100, rPath);
        String pullRequest = cmd.getPullRequests();
        System.out.println(pullRequest);
    }

    @Test
    public void testGetNumberFromTitle() {
        String title = "    -fixed issue for consul's ttl check, now the ttl interval will be still the same as \"checkInterval\", but the heartbeat will be 2/3 of \"checkInterval\" (#428)";
        Path rPath = Paths.get(userHome, workspace, repository);
        ChangeLogCmd cmd = new ChangeLogCmd("networknt", "light-4j", "2.0.19", "master", "2.0.18", 100, rPath);
        Assert.assertEquals("428", cmd.getNumberFromTitle(title));
    }

    @Test
    @Ignore
    public void testGetPrNumber() throws IOException, InterruptedException {
        Path rPath = Paths.get(userHome, workspace, repository);
        ChangeLogCmd cmd = new ChangeLogCmd("networknt", "light-4j", "2.0.19", "master", "2.0.18", 100, rPath);
        List<String> prs = cmd.getPullRequestsNumbers(cmd.getLocalGitLog());
        System.out.println(prs);
    }

    @Test
    @Ignore
    public void testParsePullRequests() throws IOException, InterruptedException {
        Path rPath = Paths.get(userHome, workspace, repository);
        ChangeLogCmd cmd = new ChangeLogCmd("networknt", "light-4j", "2.0.19", "master", "2.0.18", 100, rPath);
        Map<String, ChangeLogCmd.PullRequest> map = cmd.parsePullRequests(cmd.getPullRequests());
        System.out.println(map);
    }

    @Test
    @Ignore
    public void testGenerateChangeLog() throws IOException, InterruptedException {
        Path rPath = Paths.get(userHome, workspace, repository);
        ChangeLogCmd cmd = new ChangeLogCmd("networknt", "light-4j", "2.0.19", "master", "2.0.18", 100, rPath);
        List<String> list = cmd.genChangelog();
        System.out.println(list);
    }

    @Test
    @Ignore
    public void testChangeLogCmd() throws IOException, InterruptedException {
        Path rPath = Paths.get(userHome, workspace, repository);
        ChangeLogCmd cmd = new ChangeLogCmd("networknt", "light-4j", "2.0.19", "master", "2.0.18", 100, rPath);
        int result = cmd.execute();
        Assert.assertEquals(0, result);
    }

    @Test
    public void testGetTagRepo() {
        Path rPath = Paths.get(userHome, workspace, repository);
        ChangeLogCmd cmd = new ChangeLogCmd("networknt", "light-4j", "2.0.19", "master", "2.0.18", 100, rPath);
        String tagLine = "## [1.6.0](https://github.com/networknt/light-eventuate-4j/tree/1.6.0) (2019-04-05)";
        String tagRepo = cmd.getTagRepository(tagLine);
        Assert.assertEquals("## [1.6.0](https://github.com/networknt/light-eventuate-4j/tree/1.6.0)", tagRepo);
    }
}
