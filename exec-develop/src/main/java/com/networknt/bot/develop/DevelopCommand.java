package com.networknt.bot.develop;

import com.networknt.bot.core.*;
import com.networknt.client.Http2Client;
import com.networknt.config.Config;
import com.networknt.service.SingletonServiceFactory;
import io.undertow.UndertowOptions;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.util.HeaderMap;
import io.undertow.util.Methods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.IoUtils;
import org.xnio.OptionMap;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class DevelopCommand implements Command {
    private static final Logger logger = LoggerFactory.getLogger(DevelopCommand.class);
    public static final String CONFIG_NAME = "develop";
    Executor executor = SingletonServiceFactory.getBean(Executor.class);
    Map<String, Object> config = Config.getInstance().getJsonMapConfig(CONFIG_NAME);
    String workspace = (String)config.get(Constants.WORKSPACE);
    Map<String, Object> checkout = (Map<String, Object>)config.get(Constants.CHECKOUT);
    List<String> builds = (List<String>)config.get(Constants.BUILD);
    Map<String, Object> test = (Map<String, Object>)config.get(Constants.TEST);
    String branch = (String)checkout.get(Constants.BRANCH);
    List<String> repositories = (List<String>)checkout.get(Constants.REPOSITORY);
    String userHome = System.getProperty("user.home");

    @Override
    public String getName() {
        return "develop";
    }

    @Override
    public int execute() throws IOException, InterruptedException {
        int result = checkout();
        if(result != 0) return result;
        result = build();
        if(result != 0) return result;
        result = test();
        return result;
    }



    int checkout() throws IOException, InterruptedException {
        int result = 0;
        boolean changed = false;

        // check if there is a directory workspace in home directory.
        Path wPath = Paths.get(userHome, workspace);
        if(Files.notExists(wPath)) {
            Files.createDirectory(wPath);
        }

        for(String repository: repositories) {
            Path rPath = Paths.get(userHome, workspace, getDirFromRepo(repository));
            if(Files.notExists(rPath)) {
                // clone it
                List<String> commands = new ArrayList<>();
                commands.add("bash");
                commands.add("-c");
                commands.add("git clone " + repository);
                logger.info("git clone " + repository);
                // execute the command
                result = executor.execute(commands, wPath.toFile());
                // get the stdout and stderr from the command that was run
                StringBuilder stdout = executor.getStdout();
                if(stdout != null && stdout.length() > 0) logger.debug(stdout.toString());
                StringBuilder stderr = executor.getStderr();
                if(stderr != null && stderr.length() > 0) logger.error(stderr.toString());
                if(result != 0) {
                    break;
                }
                // need to switch to develop
                commands = new ArrayList<>();
                commands.add("bash");
                commands.add("-c");
                commands.add("git checkout " + branch);
                logger.info("git checkout " + branch);
                result = executor.execute(commands, rPath.toFile());
                stdout = executor.getStdout();
                if(stdout != null && stdout.length() > 0) logger.debug(stdout.toString());
                stderr = executor.getStderr();
                if(stderr != null && stderr.length() > 0) logger.error(stderr.toString());
                if(result != 0) {
                    break;
                }

            } else {
                // switch to branch and pull, if there is no change in the branch, return 1 to skip
                // the next build step. check how many errors against how many repositories.
                List<String> commands = new ArrayList<>();
                commands.add("bash");
                commands.add("-c");
                commands.add("git checkout " + branch + " ; git pull origin " + branch);
                logger.info("git checkout " + branch + " ; git pull origin " + branch);
                result = executor.execute(commands, rPath.toFile());
                StringBuilder stdout = executor.getStdout();
                if(stdout != null && stdout.length() > 0) logger.debug(stdout.toString());
                StringBuilder stderr = executor.getStderr();
                if(stderr != null && stderr.length() > 0) {
                    logger.error(stderr.toString());
                    if(!changed) changed = GitUtil.developBranchChanged(stderr.toString());
                }
                if(result != 0) {
                    break;
                }
            }
        }
        // there is no change for all of the repositories
        if(result == 0 && !changed) result = 1;
        return result;
    }

    int build() throws IOException, InterruptedException {
        int result = 0;
        for(String build: builds) {
            Path path = Paths.get(userHome, workspace, build);
            if(Files.notExists(path)) {
                logger.error("Path doesn't exist " + build);
                result = -1;
                break;
            } else {
                // switch to branch and pull
                List<String> commands = new ArrayList<>();
                commands.add("bash");
                commands.add("-c");
                commands.add("mvn clean install");
                logger.info("mvn clean install for " + build);
                result = executor.execute(commands, path.toFile());
                StringBuilder stdout = executor.getStdout();
                if(stdout != null && stdout.length() > 0) logger.debug(stdout.toString());
                StringBuilder stderr = executor.getStderr();
                if(stderr != null && stderr.length() > 0) logger.error(stderr.toString());
                if(result != 0) {
                    break;
                }
            }
        }
        return result;
    }

    int test() throws IOException, InterruptedException {
        int result = 0;

        for(Map.Entry<String, Object> entry : test.entrySet()) {
            String testName = entry.getKey();
            Map<String, Object> testInfo = (Map<String, Object>)entry.getValue();

            // get server entry and start servers one by one.
            List<Map<String, Object>> servers = (List<Map<String, Object>>)testInfo.get(Constants.SERVER);
            for(Map<String, Object> server: servers) {
                String path = (String)server.get(Constants.PATH);
                String cmd = (String)server.get(Constants.CMD);
                logger.info("start server at " + path + " with " + cmd);
                Path cmdPath = Paths.get(userHome, workspace, path);

                List<String> commands = new ArrayList<>();
                commands.add("nohup");
                commands.add("bash");
                commands.add("-c");
                String c = cmdPath.toString() + "/" + cmd;
                commands.add("java -jar " + c);
                result = executor.startServer(commands, cmdPath.toFile());
                StringBuilder stdout = executor.getStdout();
                if(stdout != null && stdout.length() > 0) logger.debug(stdout.toString());
                StringBuilder stderr = executor.getStderr();
                if(stderr != null && stderr.length() > 0) logger.error(stderr.toString());
                if(result != 0) {
                    logger.info("Start server failed for " + c);
                    break;
                }
            }


            // execute test cases
            logger.info("start testing...");
            // put a sleep 1 second in case the server is not ready.
            Thread.sleep(1000);

            // load tests and perform tests
            List<Map<String, Object>> requests = (List<Map<String, Object>>)testInfo.get(Constants.REQUEST);
            for(Map<String, Object> request: requests) {
                String host = (String)request.get(Constants.HOST);
                String path = (String)request.get(Constants.PATH);
                String method = (String)request.get(Constants.METHOD);
                logger.info("host = {}, path={}, method={}", host, path, method);
                Map<String, Object> requestHeader = (Map<String, Object>)request.get(Constants.HEADER);
                String requestBody = (String)request.get(Constants.BODY);
                logger.info("request header = " + requestHeader);
                logger.info("request body = " + requestBody);
                Map<String, Object> response = (Map<String, Object>)request.get(Constants.RESPONSE);
                int status = (Integer)response.get(Constants.STATUS);
                Map<String, Object> responseHeader = (Map<String, Object>)response.get(Constants.HEADER);
                Map<String, Object> responseBodyMap = (Map<String, Object>)response.get(Constants.BODY);
                logger.info("response header = " + responseHeader);
                logger.info("response body = " + responseBodyMap);
                ClientResponse cr = null;
                switch (method) {
                    case "get":
                    case "delete":
                    case "options":
                        cr = TestUtil.request(host, path, TestUtil.toHttpString(method), requestHeader);
                        break;
                    case "post":
                    case "put":
                    case "patch":
                        cr = TestUtil.requestWithBody(host, path, TestUtil.toHttpString(method), requestHeader, requestBody);
                        break;
                }

                int statusCode = cr.getResponseCode();
                HeaderMap responseHeaderMap = cr.getResponseHeaders();
                String responseBody = cr.getAttachment(Http2Client.RESPONSE_BODY);
                logger.info("statusCode = " + statusCode);
                logger.info("headerMap = " + responseHeaderMap);
                logger.info("responseBody = " + responseBody);
                // check response with the config.
                if(status != statusCode) {
                    result = -1;
                    logger.error("config status {} doesn't match the response statusCode {}", status, statusCode);
                    break;
                }

                if(!TestUtil.matchHeader(responseHeader, responseHeaderMap)) {
                    result = -1;
                    logger.error("config header {} doesn't match the response headerMap {}", responseHeader, responseHeaderMap);
                    break;
                }

                if(!TestUtil.matchBody(responseBodyMap, responseBody)) {
                    result = -1;
                    logger.error("config body {} doesn't match the response body {}", responseBodyMap, responseBody);
                    break;
                }
            }

            // shutdown servers
            executor.stopServers();
            if(result != 0) {
                break;
            }
        }
        return result;
    }


}
