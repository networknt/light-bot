package com.networknt.bot.version;

import com.networknt.bot.core.Command;
import com.networknt.bot.core.Constants;
import com.networknt.bot.core.RegexReplacement;
import com.networknt.bot.core.cmd.CheckinBranchCmd;
import com.networknt.bot.core.cmd.CheckoutPullCmd;
import com.networknt.bot.core.cmd.CloneBranchCmd;
import com.networknt.bot.core.cmd.MavenVersionCmd;
import com.networknt.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VersionUpgradeTask implements Command {
    private static final Logger logger = LoggerFactory.getLogger(VersionUpgradeTask.class);
    private static final String CONFIG_NAME = "version-upgrade";
    private Map<String, Object> config = Config.getInstance().getJsonMapConfig(CONFIG_NAME);
    private String workspace = (String)config.get(Constants.WORKSPACE);
    private String comment = (String)config.get(Constants.COMMENT);
    private String oldVersion = (String)config.get(Constants.OLD_VERSION);
    private String newVersion = (String)config.get(Constants.NEW_VERSION);
    private boolean skipCheckout = (Boolean)config.get(Constants.SKIP_CHECKOUT);
    private boolean skipMaven = (Boolean)config.get(Constants.SKIP_MAVEN);
    private boolean skipVersion = (Boolean)config.get(Constants.SKIP_VERSION);
    private boolean skipCheckin = (Boolean)config.get(Constants.SKIP_CHECKIN);
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> checkout = (List<Map<String, Object>>)config.get(Constants.CHECKOUT);
    @SuppressWarnings("unchecked")
    private List<String> mavens = (List<String>)config.get(Constants.MAVEN);
    @SuppressWarnings("unchecked")
    private Map<String, Object> version = (Map<String, Object>)config.get(Constants.VERSION);
    private String userHome = System.getProperty("user.home");

    @Override
    public String getName() {
        return "version-upgrade";
    }

    @Override
    public int execute() throws IOException, InterruptedException {
        int result = checkout();
        if(result != 0) return result;
        result = maven();
        if(result != 0) return result;
        result = version();
        if(result != 0) return result;
        result = checkin();
        return result;
    }

    private int checkout() throws IOException, InterruptedException {
        int result = 0;
        if(skipCheckout) return result;

        // check if there is a directory workspace in home directory.
        Path wPath = Paths.get(userHome, workspace);
        if(Files.notExists(wPath)) {
            Files.createDirectory(wPath);
        }

        // iterate over each group of repositories using the same branch name
        for(Map<String, Object> repoGroup : checkout) {
            // get the branch and the list of repositories
            String branch = (String)repoGroup.get(Constants.BRANCH);
            List<String> repositories = (List<String>)repoGroup.get(Constants.REPOSITORY);

            for(String repository: repositories) {
                Path rPath = Paths.get(userHome, workspace, getDirFromRepo(repository));
                if(Files.notExists(rPath)) {
                    // clone and switch to branch.
                    CloneBranchCmd cloneBranchCmd = new CloneBranchCmd(repository, branch, wPath, rPath);
                    result = cloneBranchCmd.execute();
                    if(result != 0) break;
                } else {
                    // switch to branch and pull
                    CheckoutPullCmd checkoutPullCmd = new CheckoutPullCmd(branch, rPath);
                    result = checkoutPullCmd.execute();
                    if(result != 0) break;
                }
            }
        }
        return result;
    }

    private int maven() throws IOException, InterruptedException {
        int result = 0;
        if(skipMaven) return result;

        for(String maven: mavens) {
            Path rPath = Paths.get(userHome, workspace, maven);
            MavenVersionCmd mavenVersionCmd = new MavenVersionCmd(newVersion, rPath);
            result = mavenVersionCmd.execute();
            if(result != 0) break;
        }
        return result;
    }

    private int version() throws IOException {
        int result = 0;
        if(skipVersion) return result;

        for(Map.Entry<String, Object> entry : version.entrySet()) {
            String repoName = entry.getKey();
            logger.info("repoName = " + repoName);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> files = (List<Map<String, Object>>)entry.getValue();
            for(Map<String, Object> file: files) {
                String path = (String)file.get(Constants.PATH);
                String match = (String)file.get(Constants.MATCH);
                logger.info("upgrade path {} on match {}", path, match);
                RegexReplacement rr = new RegexReplacement(match, oldVersion, newVersion);
                Path upgradePath = Paths.get(userHome, workspace, repoName, path);
                // replace old version with new version here.
                try (Stream<String> lines = Files.lines(upgradePath)) {
                    List<String> replaced = lines
                            .map(rr::replace)
                            .collect(Collectors.toList());
                    Files.write(upgradePath, replaced);
                }
            }
        }
        return result;
    }

    private int checkin() throws IOException, InterruptedException {
        int result = 0;
        if(skipCheckin) return result;

        for(Map<String, Object> repoGroup : checkout) {
            // get the branch and the list of repositories
            String branch = (String) repoGroup.get(Constants.BRANCH);
            List<String> repositories = (List<String>) repoGroup.get(Constants.REPOSITORY);

            for(String repository: repositories) {
                Path rPath = Paths.get(userHome, workspace, getDirFromRepo(repository));
                // switch to branch and check in
                CheckinBranchCmd checkinBranchCmd = new CheckinBranchCmd(branch, rPath, comment);
                result = checkinBranchCmd.execute();
                if(result != 0) break;
            }
        }
        return result;
    }
}
