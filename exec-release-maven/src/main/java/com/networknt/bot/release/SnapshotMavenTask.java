package com.networknt.bot.release;

import com.networknt.bot.core.Command;
import com.networknt.bot.core.Constants;
import com.networknt.bot.core.cmd.*;
import com.networknt.config.Config;
import com.networknt.utility.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class SnapshotMavenTask implements Command {
    private static final Logger logger = LoggerFactory.getLogger(SnapshotMavenTask.class);
    private static final String CONFIG_NAME = "release-maven";
    private Map<String, Object> config = Config.getInstance().getJsonMapConfig(CONFIG_NAME);
    private String workspace = (String)config.get(Constants.WORKSPACE);
    private String version = (String)config.get(Constants.VERSION);
    private String prevTag = (String)config.get(Constants.PREV_TAG);
    private int last = (Integer)config.get(Constants.LAST);
    private boolean skipCheckout = (Boolean)config.get(Constants.SKIP_CHECKOUT);
    private boolean skipChangeLog = (Boolean)config.get(Constants.SKIP_CHANGE_LOG);
    private boolean skipCheckin = (Boolean)config.get(Constants.SKIP_CHECKIN);
    private boolean skipRelease = (Boolean)config.get(Constants.SKIP_RELEASE);
    private boolean skipReleaseNote = (Boolean)config.get(Constants.SKIP_RELEASE_NOTE);
    private boolean skipDeploy = (Boolean)config.get(Constants.SKIP_DEPLOY);
    private boolean skipUpload = (Boolean)config.get(Constants.SKIP_UPLOAD);

    private String branch = null;  // this variable is populated in the checkout method

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> checkout = (List<Map<String, Object>>)config.get(Constants.CHECKOUT);
    @SuppressWarnings("unchecked")
    private List<String> releases = (List<String>)config.get(Constants.RELEASE);
    private List<Map<String, List<String>>> deploys = (List<Map<String, List<String>>>) config.get(Constants.DEPLOY);
    private List<Map<String, List<String>>> uploads = (List<Map<String, List<String>>>) config.get(Constants.UPLOAD);

    private String userHome = System.getProperty("user.home");

    @Override
    public String getName() {
        return "release-maven";
    }

    @Override
    public int execute() throws IOException, InterruptedException {
        int result = checkout();
        if(result != 0) return result;
        result = changeLog();
        if(result != 0) return result;
        result = checkin();
        if(result != 0) return result;
        result = release();
        if(result != 0) return result;
        result = releaseNote();
        if(result != 0) return result;
        result = deploy();
        if(result != 0) return result;
        result = upload();
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
            branch = (String) repoGroup.get(Constants.BRANCH);
            List<String> repositories = (List<String>) repoGroup.get(Constants.REPOSITORY);
            for(String repository: repositories) {
                Path rPath = getRepositoryPath(userHome, workspace, getDirFromRepo(repository));
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

    private int changeLog() throws IOException, InterruptedException {
        int result = 0;
        if(skipChangeLog) return result;

        for(String release: releases) {
            // the release is formatted as organization/repository
            String[] parts = StringUtils.split(release, "/");
            String organization = parts[0];
            String repository = parts[1];
            Path rPath = getRepositoryPath(userHome, workspace, repository);

            // generate changelog.md, check in the current branch
            ChangeLogCmd changeLogCmd = new ChangeLogCmd(organization, repository, version, branch, prevTag, last, rPath);
            result = changeLogCmd.execute();
            if (result != 0) break;
        }
        return result;
    }

    private int checkin() throws IOException, InterruptedException {
        int result = 0;
        if(skipCheckin) return result;

        for(String release: releases) {
            // the release is formatted as organization/repository
            String[] parts = StringUtils.split(release, "/");
            String organization = parts[0];
            String repository = parts[1];

            Path rPath = getRepositoryPath(userHome, workspace, repository);

            // checkin the generated changelog.md to the branch.
            CheckinBranchCmd checkinBranchCmd = new CheckinBranchCmd(branch, rPath, "light-bot checkin CHANGELOG.md");
            result = checkinBranchCmd.execute();
            if (result != 0) break;
        }
        return result;
    }

    private int release() throws IOException, InterruptedException {
        int result = 0;
        if(skipRelease) return result;

        for(String release: releases) {
            String[] parts = StringUtils.split(release, "/");
            String organization = parts[0];
            String repository = parts[1];

            Path rPath = getRepositoryPath(userHome, workspace, repository);

            // run maven release plugin to release to maven central
            MavenSnapshotCmd mavenSnapshotCmd = new MavenSnapshotCmd(rPath);
            result = mavenSnapshotCmd.execute();
            if(result != 0) break;
        }
        return result;
    }

    private int releaseNote() throws IOException, InterruptedException {
        int result = 0;
        if(skipReleaseNote) return result;

        for(String release: releases) {
            String[] parts = StringUtils.split(release, "/");
            String organization = parts[0];
            String repository = parts[1];

            Path rPath = getRepositoryPath(userHome, workspace, repository);
            // read CHANGELOG.md for the current release body.
            Charset charset = Charset.forName("UTF-8");
            Path file = getRepositoryPath(userHome, workspace, repository, "CHANGELOG.md");
            StringBuffer stringBuffer = new StringBuffer();
            try (BufferedReader reader = Files.newBufferedReader(file, charset)) {
                String line;
                boolean tokenFound = false;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("## [" + version)) {
                        tokenFound = true;
                    } else if (line.startsWith("## [")) {
                        break;
                    }
                    if(tokenFound) {
                        line = line.replace("'", "\\'");
                        stringBuffer.append(line);
                        stringBuffer.append("\n");
                    }
                }
            } catch (IOException e) {
                logger.error("IOException:", e);
            }

            // call github api to create a new release.
            GithubReleaseCmd githubReleaseCmd = new GithubReleaseCmd(organization, repository, branch, version, stringBuffer.toString(), rPath);
            result = githubReleaseCmd.execute();
            if(result != 0) break;
        }
        return result;
    }

    public int deploy() throws IOException, InterruptedException {
        int result = 0;
        if(skipDeploy) return result;
        for(Map<String, List<String>> deploy: deploys) {
            for(Map.Entry<String, List<String>> entry : deploy.entrySet()) {
                Path rPath = getRepositoryPath(userHome, workspace, entry.getKey());
                List<String> cmds = entry.getValue();
                for(String cmd: cmds) {
                    GenericSingleCmd genericSingleCmd = new GenericSingleCmd(cmd, rPath);
                    result = genericSingleCmd.execute();
                    if(result != 0) return result;
                }
            }
        }
        return result;
    }

    public int upload() throws IOException, InterruptedException {
        int result = 0;
        if(skipUpload) return result;
        for(Map<String, List<String>> upload: uploads) {
            for(Map.Entry<String, List<String>> entry : upload.entrySet()) {
                String[] parts = StringUtils.split(entry.getKey(), "/");
                String organization = parts[0];
                String repository = parts[1];
                Path rPath = getRepositoryPath(userHome, workspace, repository);
                List<String> files = entry.getValue();
                for(String file: files) {
                    UploadAssetCmd uploadAssetCmd = new UploadAssetCmd(organization, entry.getKey(), version,  file, rPath);
                    result = uploadAssetCmd.execute();
                    if(result != 0) return result;
                }
            }
        }
        return result;
    }
}
