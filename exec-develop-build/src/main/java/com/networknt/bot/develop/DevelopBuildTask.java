package com.networknt.bot.develop;

import com.networknt.bot.core.*;
import com.networknt.bot.core.cmd.CloneBranchCmd;
import com.networknt.bot.core.cmd.CopyFileCmd;
import com.networknt.client.Http2Client;
import com.networknt.config.Config;
import com.networknt.service.SingletonServiceFactory;
import io.undertow.client.ClientResponse;
import io.undertow.util.HeaderMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class DevelopBuildTask implements Command {
    private static final Logger logger = LoggerFactory.getLogger(DevelopBuildTask.class);
    
    public static final String CONFIG_NAME = "develop-build";
    Executor executor = SingletonServiceFactory.getBean(Executor.class);
    
    // bot config file
    Map<String, Object> config = Config.getInstance().getJsonMapConfig(CONFIG_NAME);
    
    // destination of the cloned/built/tested artifacts
    String workspace = (String)config.get(Constants.WORKSPACE);
    
    // global control flags
    boolean skipCheckout = (Boolean)config.get(Constants.SKIP_CHECKOUT);
    boolean skipBuild = (Boolean)config.get(Constants.SKIP_BUILD);
    boolean skipTest = (Boolean)config.get(Constants.SKIP_TEST);
    boolean skipCopyFile = (Boolean)config.get(Constants.SKIP_COPYFILE);
    boolean skipStart = (Boolean)config.get(Constants.SKIP_START);

    // tasks to be executed
    Map<String, String> tasks = (Map<String, String>)config.get(Constants.TASKS);    		
    Map<String, Object> checkout = (Map<String, Object>)config.get(Constants.CHECKOUT);
    Map<String, Object> build = (Map<String, Object>)config.get(Constants.BUILD);

    Map<String, Object> test = (Map<String, Object>)config.get(Constants.TEST);
    List<Map<String, String>> copyFiles = (List<Map<String, String>>)config.get(Constants.COPYFILE);

    Map<String, Object> start = (Map<String, Object>)config.get(Constants.START);
    
    String userHome = System.getProperty("user.home");

    @Override
    public String getName() {
        return "develop-build";
    }

    @Override
    public int execute() throws IOException, InterruptedException {
    	int result = 0;
    	
    	// navigate through the tasks list and execute them in the specified order
    	try {
    		// set data up, to start; tearDown is called automatically
    		// do not forget to implement the tear down operation
    		setup();
    		
    		for(Entry<String,String> task : tasks.entrySet()) {
    			// find the operation
    			Method anyMethod = DevelopBuildTask.class.getDeclaredMethod(task.getValue(), String.class );
    			// invoke the task
    			if( (result = (Integer)anyMethod.invoke(this, task.getKey())) != 0)
    				return result;
    		}
    	} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			logger.error("Task could not be executed and failed with: " + e.getMessage());
    	} finally {
    		// teardown data, even in case some of the tasks finished abnormally
    		tearDown();
        	// stop any started servers, even in case some of the tasks finished abnormally
        	stop();
        }
        return result;
    }

    private void setup() {
		// TODO 
		
	}

	private void tearDown() {
		// TODO 
		
	}

	int checkout(String namedTask) throws IOException, InterruptedException {
        int result = 0;
        if(skipCheckout || checkout == null) return result;

        boolean changed = false;

        // check if there is a directory workspace in home directory.
        Path wPath = Paths.get(userHome, workspace);
        if(Files.notExists(wPath)) {
            Files.createDirectory(wPath);
        }

        // get the checkout tasks for the named task
        List<Map<String, Object>> namedCheckout = (List<Map<String, Object>>)checkout.get(namedTask);
        
        // iterate over each group of repositories using the same branch name
        for(Map<String, Object> repoGroup : namedCheckout) {
        	// get the branch and the list of repositories
        	String branch = (String)repoGroup.get(Constants.BRANCH);
        	
        	// check whether this task must be skipped
        	if((Boolean)repoGroup.get(Constants.SKIP))
        		break;
        	
        	// iterate through the list of repositories 
        	List<String> repositories = (List<String>)repoGroup.get(Constants.REPOSITORY);
        	for(String repository: repositories) {
	            Path rPath = Paths.get(userHome, workspace, getDirFromRepo(repository));
	            if(Files.notExists(rPath)) {
	                // clone and switch to branch.
	                CloneBranchCmd cloneBranchCmd = new CloneBranchCmd(repository, branch, wPath, rPath);
	                result = cloneBranchCmd.execute();
	                if(!changed) changed = true;
	                if(result != 0) break;
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
	                    if(!changed) changed = GitUtil.branchChanged(branch, stderr.toString());
	                }
	                if(result != 0) {
	                    break;
	                }
	            }
        	}
        }

        return result;
    }

    int build(String namedTask) throws IOException, InterruptedException {
        int result = 0;
        if(skipBuild || build == null) return result;

        // get the build tasks for the named task
        Map<String, Object> namedBuild = (Map<String, Object>)build.get(namedTask);
        
        // check whether this task must be skipped
    	if((Boolean)namedBuild.get(Constants.SKIP))
    		return result;
    	
    	// iterate through the list of projects to build
        List<String> builds = (List<String>)namedBuild.get(Constants.PROJECT);
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
                if (skipTest) {
                    commands.add("mvn clean install -Dmaven.test.skip=true");
                } else {
                    commands.add("mvn clean install");
                }

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
    
    int test(String namedTask) throws IOException, InterruptedException {
        int result = 0;

        // check if test case execution is enabled
        if(skipTest) return result;

        // check if any test cases have been provided
        if(test == null) return result;

        for(Map.Entry<String, Object> entry : test.entrySet()) {
            String testName = entry.getKey();
            logger.info("testName = " + testName);
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
                Thread.sleep(1000);
            }


            // execute test cases
            logger.info("start testing...");
            // put a sleep 1 second in case the server is not ready.
            Thread.sleep(1000);

            try {
                // load tests and perform tests
                List<Map<String, Object>> requests = (List<Map<String, Object>>)testInfo.get(Constants.REQUEST);
                for(Map<String, Object> request: requests) {
                    String host = (String)request.get(Constants.HOST);
                    String path = (String)request.get(Constants.PATH);
                    String method = (String)request.get(Constants.METHOD);
                    Map<String, Object> requestHeader = (Map<String, Object>)request.get(Constants.HEADER);
                    String requestBody = (String)request.get(Constants.BODY);
                    logger.info("host = {}, path={}, method={}, request header={}, request body={}", host, path, method, requestHeader, requestBody);
                    Map<String, Object> response = (Map<String, Object>)request.get(Constants.RESPONSE);
                    int status = (Integer)response.get(Constants.STATUS);
                    Map<String, Object> responseHeader = (Map<String, Object>)response.get(Constants.HEADER);
                    Map<String, Object> responseBodyMap = (Map<String, Object>)response.get(Constants.BODY);
                    logger.info("expected status = " + status + " response header = " + responseHeader + " response body = " + responseBodyMap);
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
                    logger.info("returned statusCode = " + statusCode + " headerMap = " + responseHeaderMap + " responseBody = " + responseBody);
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
            } finally {
                // shutdown servers, this needs to be called even if there are exceptions during tests.
                logger.info("shutdown servers");
                executor.stopServers();
            }

            if(result != 0) {
                break;
            }
        }
        return result;
    }

    int copyFile(String namedTask) throws IOException, InterruptedException {
        int result = 0;
        if(skipCopyFile) return result;
        for(Map<String, String> copyFile: copyFiles) {
            String src = copyFile.get("src");
            String dst = copyFile.get("dst");
            logger.info("Copying from " + src + " to " + dst);
            CopyFileCmd copyFileCmd = new CopyFileCmd(userHome, workspace, src, dst);
            copyFileCmd.execute();
        }
        return result;
    }
    
    int start(String namedTask) throws IOException, InterruptedException {
        int result = 0;

        // check if start service execution is enabled
        if(skipStart || start == null) return result;

        // get the start tasks for the named task
        Map<String, Object> namedStart = (Map<String, Object>)start.get(namedTask);
        
        // check whether this task must be skipped
    	if((Boolean)namedStart.get(Constants.SKIP))
    		return result;
        
        for(Map.Entry<String, Object> entry : ((Map<String, Object>)namedStart.get(Constants.SERVICES)).entrySet()) {
            String startName = entry.getKey();
            logger.info("ServiceName = " + startName);
            Map<String, Object> startInfo = (Map<String, Object>)entry.getValue();
            // get server entry and start servers one by one.
            List<Map<String, Object>> servers = (List<Map<String, Object>>)startInfo.get(Constants.SERVER);
            for(Map<String, Object> server: servers) {
                String path = (String)server.get(Constants.PATH);
                String cmd = (String)server.get(Constants.CMD);
                String host = (String)server.get(Constants.HOST);
                int port = (Integer)server.get(Constants.PORT);
                int timeout = (Integer)server.get(Constants.TIMEOUT);
                
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
                
                // put a sleep in case the server is not ready.
                Thread.sleep(timeout);
            }


            // execute test cases
            logger.info("start testing...");
        }
    	
    	return result;
    }
    
    void stop() throws IOException, InterruptedException {
        // shutdown servers, this needs to be called even if there are exceptions during tests.
        logger.info("shutdown servers");
        executor.stopServers();
    }
}
