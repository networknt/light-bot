package com.networknt.bot.core.cmd;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

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
    @Disabled
    public void testLocalGitLog() throws IOException, InterruptedException {
        // run the 'ls -l' with GenericSingleCmd
        Path rPath = Paths.get(userHome, workspace, repository);
        ChangeLogCmd cmd = new ChangeLogCmd("networknt", "light-4j", "2.0.19", "master", "2.0.18", 100, rPath);
        String gitLog = cmd.getLocalGitLog();
        System.out.println(gitLog);
    }

    @Test
    @Disabled
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
        Assertions.assertEquals("428", cmd.getNumberFromTitle(title));
    }

    @Test
    @Disabled
    public void testGetPrNumber() throws IOException, InterruptedException {
        Path rPath = Paths.get(userHome, workspace, repository);
        ChangeLogCmd cmd = new ChangeLogCmd("networknt", "light-4j", "2.0.19", "master", "2.0.18", 100, rPath);
        List<String> prs = cmd.getPullRequestsNumbers(cmd.getLocalGitLog());
        System.out.println(prs);
    }

    @Test
    @Disabled
    public void testParsePullRequests() throws IOException, InterruptedException {
        Path rPath = Paths.get(userHome, workspace, repository);
        ChangeLogCmd cmd = new ChangeLogCmd("networknt", "light-4j", "2.0.19", "master", "2.0.18", 100, rPath);
        Map<String, ChangeLogCmd.PullRequest> map = cmd.parsePullRequests(cmd.getPullRequests());
        System.out.println(map);
    }

    @Test
    @Disabled
    public void testGenerateChangeLog() throws IOException, InterruptedException {
        Path rPath = Paths.get(userHome, workspace, repository);
        ChangeLogCmd cmd = new ChangeLogCmd("networknt", "light-4j", "2.0.19", "master", "2.0.18", 100, rPath);
        List<String> list = cmd.genChangelog();
        System.out.println(list);
    }

    @Test
    @Disabled
    public void testChangeLogCmd() throws IOException, InterruptedException {
        Path rPath = Paths.get(userHome, workspace, repository);
        ChangeLogCmd cmd = new ChangeLogCmd("networknt", "light-4j", "2.0.19", "master", "2.0.18", 100, rPath);
        int result = cmd.execute();
        Assertions.assertEquals(0, result);
    }

    @Test
    public void testGetTagRepo() {
        Path rPath = Paths.get(userHome, workspace, repository);
        ChangeLogCmd cmd = new ChangeLogCmd("networknt", "light-4j", "2.0.19", "master", "2.0.18", 100, rPath);
        String tagLine = "## [1.6.0](https://github.com/networknt/light-eventuate-4j/tree/1.6.0) (2019-04-05)";
        String tagRepo = cmd.getTagRepository(tagLine);
        Assertions.assertEquals("## [1.6.0](https://github.com/networknt/light-eventuate-4j/tree/1.6.0)", tagRepo);
    }
}
