package com.networknt.bot.core.cmd;

import com.networknt.bot.core.Command;
import com.networknt.bot.core.Executor;
import com.networknt.config.JsonMapper;
import com.networknt.service.SingletonServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * This is a command that is used to generate change log before releasing.
 *
 * @author Steve Hu
 */
public class ChangeLogCmd implements Command {
    private static final Logger logger = LoggerFactory.getLogger(ChangeLogCmd.class);
    private Executor executor = SingletonServiceFactory.getBean(Executor.class);
    private String organization;
    private String repository;
    private String version;
    private String branch;
    private String prevTag;
    private int last;
    private Path rPath;
    private String token;

    public ChangeLogCmd(String organization, String repository, String version, String branch, String prevTag, int last, Path rPath) {
        this.organization = organization;
        this.repository = repository;
        this.version = version;
        this.branch = branch;
        this.prevTag = prevTag;
        this.last = last;
        this.rPath = rPath;
        // get github token from environment variable
        this.token = System.getenv("CHANGELOG_GITHUB_TOKEN");
    }

    @Override
    public int execute() throws IOException, InterruptedException {
        List<String> genLog = genChangelog();
        List<String> fileContent = new ArrayList<>(Files.readAllLines(Paths.get(rPath.toString(), "CHANGELOG.md"), StandardCharsets.UTF_8));
        fileContent.addAll(2, genLog);
        Files.write(Paths.get(rPath.toString(), "CHANGELOG.md"), fileContent, StandardCharsets.UTF_8);
        return 0;
    }

    public List<String> genChangelog() throws IOException, InterruptedException {
        List<String> logs = new ArrayList<>();
        Date date = new Date();
        String modifiedDate= new SimpleDateFormat("yyyy-MM-dd").format(date);

        String tagLine = String.format("## [%s](https://github.com/networknt/%s/tree/%s) (%s)", version, repository, version, modifiedDate);
        logs.add(tagLine);
        logs.add("\n");
        logs.add("**Merged pull requests:**");
        logs.add("\n");

        List<String> numbers = getPullRequestsNumbers(getLocalGitLog());
        Map<String, PullRequest> prs = parsePullRequests(getPullRequests());
        for(String number: numbers) {
            PullRequest pr = prs.get(number);
            String s = String.format("- %s [\\#%s](%s) ([%s](%s))", pr.title.replace("#", "\\#"), number, pr.issureUrl, pr.author, pr.authorUrl);
            logs.add(s);
        }
        return logs;
    }

    public String getPrevTagDate() throws IOException, InterruptedException {
        String cmd = "git log -1 --format=%aI " + prevTag;
        List<String> commands = new ArrayList<>();
        commands.add("bash");
        commands.add("-c");
        commands.add(cmd);
        logger.info(cmd + " for " + rPath);
        int result = executor.execute(commands, rPath.toFile());
        StringBuilder stdout = executor.getStdout();

        if(stdout != null && stdout.length() > 0) {
            return stdout.toString();
        } else {
            StringBuilder stderr = executor.getStderr();
            if(stderr != null && stderr.length() > 0) logger.error(stderr.toString());
            return null;
        }
    }


    public String getLocalGitLog() throws IOException, InterruptedException {
        String cmd = String.format("git log --since=\"%s\"", getPrevTagDate());
        List<String> commands = new ArrayList<>();
        commands.add("bash");
        commands.add("-c");
        commands.add(cmd);
        logger.info(cmd + " for " + rPath);
        int result = executor.execute(commands, rPath.toFile());
        StringBuilder stdout = executor.getStdout();

        if(stdout != null && stdout.length() > 0) {
            return stdout.toString();
        } else {
            StringBuilder stderr = executor.getStderr();
            if(stderr != null && stderr.length() > 0) logger.error(stderr.toString());
            return null;
        }
    }

    public List<String> getPullRequestsNumbers(String log) {
        List<String> prs = new ArrayList<>();
        Scanner scanner = new Scanner(log);
        while(scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if(line.startsWith("commit ")) {
                scanner.nextLine(); // Author
                scanner.nextLine(); // Date
                scanner.nextLine(); // empty line
                String title = scanner.nextLine();
                if(title.endsWith(")")) {
                    prs.add(getNumberFromTitle(title));
                }
            }
        }
        scanner.close();
        return prs;
    }

    public String getNumberFromTitle(String title) {
        int index = title.indexOf("(#");
        return title.substring(index + 2, title.length() - 1);
    }

    public String getPullRequests() throws IOException, InterruptedException {
        String cmd = String.format("curl -X POST https://api.github.com/graphql -H 'authorization: bearer %s' -H 'content-type: application/json' -d '{\"query\": \"query {repository(owner:\\\"%s\\\",name:\\\"%s\\\"){pullRequests(last:%s,states:MERGED){edges{node{number,title,url,author{login,url}}}}}}\"}'", token, organization, repository, last);
        List<String> commands = new ArrayList<>();
        commands.add("bash");
        commands.add("-c");
        commands.add(cmd);
        logger.info(cmd + " for " + rPath);
        int result = executor.execute(commands, rPath.toFile());
        StringBuilder stdout = executor.getStdout();
        if(stdout != null && stdout.length() > 0) {
            return stdout.toString();
        } else {
            StringBuilder stderr = executor.getStderr();
            if(stderr != null && stderr.length() > 0) logger.error(stderr.toString());
            return null;
        }
    }

    public Map<String, PullRequest> parsePullRequests(String pullRequests) {
        Map<String, PullRequest> result = new HashMap<>();

        Map<String, Object> map = JsonMapper.string2Map(pullRequests);
        Map<String, Object> data = (Map<String, Object>)map.get("data");
        Map<String, Object> repository = (Map<String, Object>)data.get("repository");
        Map<String, Object> pRequests = (Map<String, Object>)repository.get("pullRequests");
        List<Map<String, Object>> edges = (List<Map<String, Object>>)pRequests.get("edges");

        for (Map<String, Object> edge : edges) {
            for (Map.Entry<String, Object> entry : edge.entrySet()) {
                Map<String, Object> node = (Map<String, Object>)entry.getValue();
                PullRequest pr = new PullRequest();
                Integer number = (Integer)node.get("number");
                pr.title = (String)node.get("title");
                pr.issureUrl = (String)node.get("url");
                Map<String, String> author = (Map<String, String>)node.get("author");
                pr.author = author.get("login");
                pr.authorUrl = author.get("url");
                result.put(number.toString(), pr);
            }
        }
        return result;
    }

    @Override
    public String getName() {
        return "ChangeLog";
    }

    class PullRequest {
        String title;
        String issureUrl;
        String author;
        String authorUrl;

        public PullRequest() {
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getIssureUrl() {
            return issureUrl;
        }

        public void setIssureUrl(String issureUrl) {
            this.issureUrl = issureUrl;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public String getAuthorUrl() {
            return authorUrl;
        }

        public void setAuthorUrl(String authorUrl) {
            this.authorUrl = authorUrl;
        }
    }

}
