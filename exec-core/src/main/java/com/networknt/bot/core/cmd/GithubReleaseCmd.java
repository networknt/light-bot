package com.networknt.bot.core.cmd;

import com.networknt.bot.core.Command;
import com.networknt.bot.core.Executor;
import com.networknt.bot.core.GitUtil;
import com.networknt.client.Http2Client;
import com.networknt.config.Config;
import com.networknt.service.SingletonServiceFactory;
import io.undertow.client.ClientResponse;
import io.undertow.util.Methods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GithubReleaseCmd implements Command {
    private static final Logger logger = LoggerFactory.getLogger(GithubReleaseCmd.class);
    private Executor executor = SingletonServiceFactory.getBean(Executor.class);
    private Path rPath;
    private String token;
    private String organization;
    private String repository;
    private String version;
    private String body;


    public GithubReleaseCmd(String organization, String repository, String version, String body, Path rPath) {
        this.organization = organization;
        this.repository = repository;
        this.version = version;
        this.body = body;
        this.rPath = rPath;
        this.token = System.getenv("CHANGELOG_GITHUB_TOKEN");
    }

    @Override
    public int execute() throws IOException, InterruptedException {
        int result = 0;
        // release project to github.com
        Map<String, Object> headerMap = new HashMap<String, Object>();
        headerMap.put("authorization", "token " + token);
        headerMap.put("content-type", "application/json");

        Map<String, Object> bodyMap = new HashMap<String, Object>();
        bodyMap.put("tag_name", version);
        bodyMap.put("target_commitish", "master");
        bodyMap.put("name", version);
        bodyMap.put("body", body);
        bodyMap.put("draft", false);
        bodyMap.put("prerelease", false);

        String path = String.format("repos/%s/%s/releases", organization, repository);
        ClientResponse cr = GitUtil.requestWithBody(GitUtil.HOST, path, Methods.POST, headerMap, Config.getInstance().getMapper().writeValueAsString(bodyMap));
        result = cr.getResponseCode();
        String responseBody = cr.getAttachment(Http2Client.RESPONSE_BODY);
        logger.info("statusCode = " + result);
        logger.info("responseBody = " + responseBody);
        return result;
    }

    @Override
    public String getName() {
        return "GithubRelease";
    }
}
