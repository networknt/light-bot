package com.networknt.bot.regex.replace;

import com.networknt.bot.core.Command;
import com.networknt.bot.core.Constants;
import com.networknt.bot.core.RegexReplacement;
import com.networknt.bot.core.cmd.CheckinBranchCmd;
import com.networknt.bot.core.cmd.CheckoutPullCmd;
import com.networknt.bot.core.cmd.CloneBranchCmd;
import com.networknt.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * This is a similar task like version update, but this one is more generic. The most important
 * use case for this task is to update a dependency version in hundreds projects scattered in
 * several github organizations.
 *
 * For example replace json-schema-validator from 0.1.10 to 0.1.15 due to a defect is fixed.
 *
 * Here is the workflow.
 *
 * 1. Checkout all defined repositories. clone or pull if repository exists.
 * 2. Full text search and regex match to a pattern and replace if match with a new value
 * 3. Iterate every repository to see if there are any changes. create and push a new branch if yes
 * 4. Merge the new branch to develop. normally skipped first and run separately upon confirmation
 *
 * @author Steve Hu
 */
public class RegexReplaceTask implements Command {
    private static final Logger logger = LoggerFactory.getLogger(RegexReplaceTask.class);
    private static final String CONFIG_NAME = "regex-replace";
    private Map<String, Object> config = Config.getInstance().getJsonMapConfig(CONFIG_NAME);
    private String workspace = (String)config.get(Constants.WORKSPACE);
    private String comment = (String)config.get(Constants.COMMENT);
    private boolean skipCheckout = (Boolean)config.get(Constants.SKIP_CHECKOUT);
    private boolean skipReplace = (Boolean)config.get(Constants.SKIP_REPLACE);
    private boolean skipCheckin = (Boolean)config.get(Constants.SKIP_CHECKIN);
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> checkout = (List<Map<String, Object>>)config.get(Constants.CHECKOUT);
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> replaces = (List<Map<String, Object>>)config.get(Constants.REPLACE);

    private String userHome = System.getProperty("user.home");

    @Override
    public String getName() {
        return "regex-replace";
    }

    @Override
    public int execute() throws IOException, InterruptedException {
        int result = checkout();
        if(result != 0) return result;
        result = replace();
        if(result != 0) return result;

        result = checkin();

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
                logger.info("Checkout or pull for " + repository);
                Path rPath = getRepositoryPath(userHome, workspace, getDirFromRepo(repository));
                if(Files.notExists(rPath)) {
                    logger.info("Clone repository to: " + rPath);
                    // clone and switch to branch.
                    CloneBranchCmd cloneBranchCmd = new CloneBranchCmd(repository, branch, wPath, rPath);
                    result = cloneBranchCmd.execute();
                    if(result != 0) break;
                } else {
                    logger.info("Switch to {} and pull from git", branch);
                    // switch to branch and pull
                    CheckoutPullCmd checkoutPullCmd = new CheckoutPullCmd(branch, rPath);
                    result = checkoutPullCmd.execute();
                    if(result != 0) break;
                }
            }
        }
        return result;
    }


    private int replace() throws IOException {
        int result = 0;
        if(skipReplace) return result;

        for(Map<String, Object> replace: replaces) {
            String glob = (String)replace.get(Constants.GLOB);
            String match = (String)replace.get(Constants.MATCH);
            String oldValue = (String)replace.get(Constants.OLD_VALUE);
            String newValue = (String)replace.get(Constants.NEW_VALUE);
            logger.info("replace glob: {} match: {} old: {} new: {}", glob, match, oldValue, newValue);
            // now iterate all files in the folder to find the pattern and handle the file
            if(!glob.startsWith("glob:")) glob = "glob:" + glob;
            match(glob, userHome, workspace, match, oldValue, newValue);

        }
        return result;
    }

    private int checkin() throws IOException, InterruptedException {
        int result = 0;
        if(skipCheckin) return result;

        // iterate over each group of repositories using the same branch name
        for(Map<String, Object> repoGroup : checkout) {
            // get the branch and the list of repositories
            String branch = (String) repoGroup.get(Constants.BRANCH);
            List<String> repositories = (List<String>) repoGroup.get(Constants.REPOSITORY);

            for(String repository: repositories) {
                Path rPath = getRepositoryPath(userHome, workspace, getDirFromRepo(repository));
                // switch to branch and check in
                CheckinBranchCmd checkinBranchCmd = new CheckinBranchCmd(branch, rPath, comment);
                result = checkinBranchCmd.execute();
                if(result != 0) break;
            }
        }
        return result;
    }

    private void match(String glob, String userHome, String workspace, String match, String oldValue, String newValue) throws IOException {

        final PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher(glob);
        final RegexReplacement rr = new RegexReplacement(match, oldValue, newValue);

        Files.walkFileTree(getWorkspacePath(userHome, workspace), new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                if (pathMatcher.matches(path)) {
                    logger.info(path.toString());
                    // replace old version with new version here.
                    try (Stream<String> lines = Files.lines(path)) {
                        List<String> replaced = new ArrayList<>();
                        boolean matched = false;
                        Iterable<String> iterable = lines::iterator;
                        for (String s : iterable) {
                            if(rr.match(s)) {
                                matched = true;
                                logger.info("Matched with pattern: " + match);
                                String t = rr.replace(s);
                                if(!t.equals(s)) {
                                    logger.info("Replaced old: {} new: {}", s, t);
                                    s = t;
                                } else {
                                    logger.info("!!!!!!!!!!Couldn't find old value in line: " + s);
                                }
                            }
                            replaced.add(s);
                        }
                        if(matched) {
                            Files.write(path, replaced);
                        }
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc)
                    throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
