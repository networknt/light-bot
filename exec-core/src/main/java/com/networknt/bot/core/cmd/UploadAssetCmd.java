package com.networknt.bot.core.cmd;

import com.networknt.bot.core.Command;
import com.networknt.bot.core.Executor;
import com.networknt.config.JsonMapper;
import com.networknt.service.SingletonServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This is the command to upload an asset to the github release. It leverage the GitHub V3 API
 *
 * @author Steve Hu
 */
public class UploadAssetCmd implements Command {
    private static final Logger logger = LoggerFactory.getLogger(GithubReleaseCmd.class);
    private Executor executor = SingletonServiceFactory.getBean(Executor.class);
    private Path rPath;
    private String token;
    private String organization;
    private String repository;
    private String label;
    private String filename;
    private String version;


    public UploadAssetCmd(String organization, String repository, String version, String filename, Path rPath) {
        this.organization = organization;
        this.repository = repository;
        this.version = version;
        this.filename = filename;
        // label is the name of the attachment on the release page. use the real name which is the last part of the file.
        this.label = filename.substring(filename.lastIndexOf("/") + 1);
        this.rPath = rPath;
        this.token = System.getenv("CHANGELOG_GITHUB_TOKEN");
    }

    @Override
    public int execute() throws IOException, InterruptedException {
        int result = 0;
        int releaseId = 0;
        // first we need to get the release id from the tag/version as the next step need it.
        String cmd = String.format("curl https://api.github.com/repos/%s/%s/releases/tags/%s", organization, repository, version);
        List<String> commands = new ArrayList<>();
        commands.add("bash");
        commands.add("-c");
        commands.add(cmd);
        logger.info(cmd);
        result = executor.execute(commands, rPath.toFile());
        StringBuilder stdout = executor.getStdout();
        if(stdout != null && stdout.length() > 0) {
            logger.debug(stdout.toString());
            // parse the release id from the response.
            Map<String, Object> map = JsonMapper.string2Map(stdout.toString());
            releaseId = (Integer)map.get("id");
        }
        StringBuilder stderr = executor.getStderr();
        if(stderr != null && stderr.length() > 0) logger.error(stderr.toString());
        if(result != 0) return result;

        // upload an asset to github.com release. note that --data-binary is the filename with @ as prefix to indicate that is the content of the file.
        cmd = String.format("curl -X POST https://uploads.github.com/repos/%s/%s/releases/%s/assets?name=%s -H 'authorization: token %s' -H 'content-type: application/zip' --data-binary '@%s' ", organization, repository, releaseId, label, token, filename);
        commands = new ArrayList<>();
        commands.add("bash");
        commands.add("-c");
        commands.add(cmd);
        logger.info(cmd);
        result = executor.execute(commands, rPath.toFile());
        stdout = executor.getStdout();
        if(stdout != null && stdout.length() > 0) logger.debug(stdout.toString());
        stderr = executor.getStderr();
        if(stderr != null && stderr.length() > 0) logger.error(stderr.toString());
        return result;
    }

    @Override
    public String getName() {
        return "UploadAsset";
    }
}
