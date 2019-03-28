package com.networknt.bot.core.cmd;

import com.networknt.bot.core.Command;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class ChangeLogCmdTest {
    private String userHome = System.getProperty("user.home");
    private String workspace = "releasemaven_1_5_x";
    private String repository = "light-4j";

    //@Test
    public void testLocalGitLog() throws IOException, InterruptedException {
        // run the 'ls -l' with GenericSingleCmd
        Path rPath = Paths.get(userHome, workspace, repository);
        ChangeLogCmd cmd = new ChangeLogCmd("networknt", "light-4j", "1.5.33", "1.5.x", "2.0.0-BETA2", 100, rPath);
        String gitLog = cmd.getLocalGitLog();
        System.out.println(gitLog);
    }

    //@Test
    public void testPullRequest() throws IOException, InterruptedException {
        // run the 'ls -l' with GenericSingleCmd
        Path rPath = Paths.get(userHome, workspace, repository);

        ChangeLogCmd cmd = new ChangeLogCmd("networknt", "light-4j", "1.5.33", "1.5.x", "2.0.0-BETA2", 100, rPath);
        String pullRequest = cmd.getPullRequests();
        System.out.println(pullRequest);
    }

    //@Test
    public void testGetNumberFromTitle() {
        String title = "    -fixed issue for consul's ttl check, now the ttl interval will be still the same as \"checkInterval\", but the heartbeat will be 2/3 of \"checkInterval\" (#428)";
        Path rPath = Paths.get(userHome, workspace, repository);
        ChangeLogCmd cmd = new ChangeLogCmd("networknt", "light-4j", "1.5.33", "1.5.x", "2.0.0-BETA2", 100, rPath);
        Assert.assertEquals("428", cmd.getNumberFromTitle(title));
    }

    //@Test
    public void testGetPrNumber() throws IOException, InterruptedException {
        Path rPath = Paths.get(userHome, workspace, repository);
        ChangeLogCmd cmd = new ChangeLogCmd("networknt", "light-4j", "1.5.33", "1.5.x", "2.0.0-BETA2", 100, rPath);
        List<String> prs = cmd.getPullRequestsNumbers(cmd.getLocalGitLog());
        System.out.println(prs);
    }

    //@Test
    public void testParsePullRequests() throws IOException, InterruptedException {
        Path rPath = Paths.get(userHome, workspace, repository);
        ChangeLogCmd cmd = new ChangeLogCmd("networknt", "light-4j", "1.5.33", "1.5.x", "2.0.0-BETA2", 100, rPath);
        Map<String, ChangeLogCmd.PullRequest> map = cmd.parsePullRequests(cmd.getPullRequests());
        System.out.println(map);
    }

    //@Test
    public void testGenerateChangeLog() throws IOException, InterruptedException {
        Path rPath = Paths.get(userHome, workspace, repository);
        ChangeLogCmd cmd = new ChangeLogCmd("networknt", "light-4j", "1.5.33", "1.5.x", "2.0.0-BETA2", 100, rPath);
        List<String> list = cmd.genChangelog();
        System.out.println(list);
    }

    //@Test
    public void testChangeLogCmd() throws IOException, InterruptedException {
        Path rPath = Paths.get(userHome, workspace, repository);
        ChangeLogCmd cmd = new ChangeLogCmd("networknt", "light-4j", "1.5.33", "1.5.x", "2.0.0-BETA2", 100, rPath);
        int result = cmd.execute();
        Assert.assertEquals(0, result);
    }

}
