package com.networknt.bot.develop;

import com.networknt.bot.core.*;
import com.networknt.bot.core.cmd.CloneBranchCmd;
import com.networknt.bot.core.cmd.CopyFileCmd;
import com.networknt.bot.core.cmd.CopyWildcardFileCmd;
import com.networknt.client.Http2Client;
import com.networknt.config.Config;
import com.networknt.service.SingletonServiceFactory;
import io.undertow.client.ClientResponse;
import io.undertow.util.HeaderMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class DevelopBuildTask implements Command {
	private static final Logger logger = LoggerFactory.getLogger(DevelopBuildTask.class);

	public static final String CONFIG_NAME = "develop-build";
	Executor executor = SingletonServiceFactory.getBean(Executor.class);

	// bot config file
	Map<String, Object> config = Config.getInstance().getJsonMapConfig(CONFIG_NAME);

	// destination of the cloned/built/tested artifacts
	String workspace = (String) config.get(Constants.WORKSPACE);

	// global control flags
	boolean skipCheckout = (Boolean) config.get(Constants.SKIP_CHECKOUT);
	boolean skipBuild = (Boolean) config.get(Constants.SKIP_BUILD);
	boolean skipTest = config.get(Constants.SKIP_TEST) != null ? (Boolean) config.get(Constants.SKIP_TEST) : true;
	boolean skipCopyFile = config.get(Constants.SKIP_COPYFILE) != null ? (Boolean) config.get(Constants.SKIP_COPYFILE)
			: true;
	boolean skipCopyWildcardFile = config.get(Constants.SKIP_COPYWILDCARDFILE) != null
			? (Boolean) config.get(Constants.SKIP_COPYWILDCARDFILE)
			: true;
	boolean skipStart = config.get(Constants.SKIP_START) != null ? (Boolean) config.get(Constants.SKIP_START) : true;
	boolean skipMergeConfig = config.get(Constants.SKIP_MERGECONFIG) != null
			? (Boolean) config.get(Constants.SKIP_MERGECONFIG)
			: true;
	boolean skipGenerateEclipseProject = config.get(Constants.SKIP_GENERATE_ECLIPSE_PROJECT) != null
			? (Boolean) config.get(Constants.SKIP_GENERATE_ECLIPSE_PROJECT)
			: true;

	// tasks to be executed
	Map<String, String> tasks = (Map<String, String>) config.get(Constants.TASKS);
	Map<String, Object> checkout = (Map<String, Object>) config.get(Constants.CHECKOUT);
	Map<String, Object> build = (Map<String, Object>) config.get(Constants.BUILD);

	Map<String, Object> test = (Map<String, Object>) config.get(Constants.TEST);
	List<Map<String, String>> copyFiles = (List<Map<String, String>>) config.get(Constants.COPYFILE);
	List<Map<String, String>> copyWildcardFiles = (List<Map<String, String>>) config.get(Constants.COPYWILDCARDFILE);
	Map<String, Object> start = (Map<String, Object>) config.get(Constants.START);
	Map<String, Object> mergeConfig = config.get(Constants.MERGE_CONFIG) != null
			? (Map<String, Object>) config.get(Constants.MERGE_CONFIG)
			: new HashMap<String, Object>();

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

			for (Entry<String, String> task : tasks.entrySet()) {
				// find the operation
				Method anyMethod = DevelopBuildTask.class.getDeclaredMethod(task.getValue(), String.class);
				// invoke the task
				if ((result = (Integer) anyMethod.invoke(this, task.getKey())) != 0)
					return result;
			}
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			logger.error("Task could not be executed and failed with:", e);
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
		if (skipCheckout || checkout == null)
			return result;

		boolean changed = false;

		// check if there is a directory workspace in home directory.
		Path wPath = getWorkspacePath(userHome, workspace);
		if (Files.notExists(wPath)) {
			Files.createDirectory(wPath);
		}

		// get the checkout tasks for the named task
		List<Map<String, Object>> namedCheckout = (List<Map<String, Object>>) checkout.get(namedTask);

		// iterate over each group of repositories using the same branch name
		for (Map<String, Object> repoGroup : namedCheckout) {
			// get the branch and the list of repositories
			String branch = (String) repoGroup.get(Constants.BRANCH);

			// check whether this task must be skipped
			if ((Boolean) repoGroup.get(Constants.SKIP))
				break;

			// iterate through the list of repositories
			List<String> repositories = (List<String>) repoGroup.get(Constants.REPOSITORY);
			for (String repository : repositories) {
				Path rPath = getRepositoryPath(userHome, workspace, getDirFromRepo(repository));
				if (Files.notExists(rPath)) {
					// clone and switch to branch.
					CloneBranchCmd cloneBranchCmd = new CloneBranchCmd(repository, branch, wPath, rPath);
					result = cloneBranchCmd.execute();
					if (!changed)
						changed = true;
					if (result != 0)
						break;
				} else {
					// switch to branch and pull, if there is no change in the branch, return 1 to
					// skip
					// the next build step. check how many errors against how many repositories.
					List<String> commands = new ArrayList<>();
					commands.add("bash");
					commands.add("-c");
					commands.add("git fetch ; git checkout " + branch + " ; git pull origin " + branch);
					logger.info("git fetch ; git checkout " + branch + " ; git pull origin " + branch + " for " + rPath);
					result = executor.execute(commands, rPath.toFile());
					StringBuilder stdout = executor.getStdout();
					if (stdout != null && stdout.length() > 0)
						logger.debug(stdout.toString());
					StringBuilder stderr = executor.getStderr();
					if (stderr != null && stderr.length() > 0) {
						logger.error(stderr.toString());
						if (!changed)
							changed = GitUtil.branchChanged(branch, stderr.toString());
					}
					if (result != 0) {
						break;
					}
				}
			}
		}

		return result;
	}

	int mergeConfig(String namedTask) throws IOException, InterruptedException {
		int result = 0;
		if (skipMergeConfig || mergeConfig == null)
			return result;

		// get the checkout tasks for the named task
		List<Map<String, Object>> namedMergeList = (List<Map<String, Object>>) mergeConfig.get(namedTask);

		// iterate over each group of repositories where merge config is required
		for (Map<String, Object> namedMerge : namedMergeList) {
			// check whether this task must be skipped
			if ((Boolean) namedMerge.get(Constants.SKIP))
				continue;

			// get the type of config file to merge
			String fileType = (String) namedMerge.get(Constants.FILE);

			// repositories to merge from
			List<String> repositories = (List<String>) namedMerge.get(Constants.REPOSITORY);

			if (logger.isInfoEnabled()) {
				logger.info(String.format("Merging configurations for project %s in file of type %s", (String) namedMerge.get(Constants.PROJECT), fileType));
			}
			
			// final merged map
			SortedMap<String, Object> mergedMap = new TreeMap<String, Object>();
			Set<String> mergedSet = null;
			// iterate through all repos and load the respective file
			// it is assumed that configs are maps and merging them is predicated on map
			// usage
			for (String repo : repositories) {
				// get the path of the file to be merged
				File filePath = getRepositoryPath(userHome, workspace, repo).toFile();

				// load the configuration
				Map<String, Object> loadedMap = ConfigUtils.getInstance().loadMapConfig(fileType, filePath);
				if (loadedMap == null) {
					logger.error("File could not be loaded: " + repo);
					return -1;
				}
				// check for intersection of maps
				mergedSet = new HashSet<String>(mergedMap.keySet());
				mergedSet.retainAll(loadedMap.keySet());

				if (mergedSet.size() > 0) {
					logger.error("Failure: There are duplicates between the keys of the maps to be merged: "
							+ mergedSet.toString());
					return -1;
				}

				// add to the merged map
				mergedMap.putAll(loadedMap);
			}

			// dump the file to the destination folder
			String outputFormat = (String) namedMerge.get(Constants.OUTPUT_FORMAT);
			String output = (String) namedMerge.get(Constants.OUTPUT);

			// pretty print the output
			File file = getRepositoryPath(userHome, workspace, output, fileType + "." + outputFormat).toFile();
			DumperOptions options = new DumperOptions();
			options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
			options.setPrettyFlow(true);

			Yaml yaml = new Yaml(options);
			yaml.dump(mergedMap, new PrintWriter(file));
			if (logger.isInfoEnabled()) {
				logger.info(String.format("Merged configurations for project %s in file of type %s into file %s", 
						(String) namedMerge.get(Constants.PROJECT), fileType, file));
			}
		}

		return result;
	}

	int build(String namedTask) throws IOException, InterruptedException {
		int result = 0;
		if (skipBuild || build == null)
			return result;

		// get the build tasks for the named task
		Map<String, Object> namedBuild = (Map<String, Object>) build.get(namedTask);

		// check whether this task must be skipped
		if ((Boolean) namedBuild.get(Constants.SKIP))
			return result;
		
		// check whether this build should build a FatJar or not
		boolean buildFatJar = true;
		if(namedBuild.get(Constants.BUILD_FAT_JAR) != null)
			buildFatJar = (Boolean)namedBuild.get(Constants.BUILD_FAT_JAR);
		
		// check whether this build task must be executed with running tests or not
		boolean skipNamedTests = false;
		Object skip_test = namedBuild.get(Constants.SKIP_TEST);
		if(skip_test != null)
			skipNamedTests = (Boolean)skip_test;

		// iterate through the list of projects to build
		List<String> builds = (List<String>) namedBuild.get(Constants.PROJECT);
		for (String build : builds) {
			Path path = getRepositoryPath(userHome, workspace, build);
			if (Files.notExists(path)) {
				logger.error("Path doesn't exist " + build);
				result = -1;
				break;
			} else {
				// switch to branch and pull
				List<String> commands = new ArrayList<>();
				commands.add("bash");
				commands.add("-c");

				String mavenCmd = "mvn clean install";
				if(skipNamedTests || (!skipNamedTests && skipTest))
					mavenCmd += " -Dmaven.test.skip=true";

				if(!skipGenerateEclipseProject)
					mavenCmd += " eclipse:eclipse";
				
				if(buildFatJar)
					mavenCmd += " -Prelease";

				commands.add(mavenCmd);

				logger.info(commands.toString());
				logger.info("mvn clean install for " + build + " build FatJar set to: " + buildFatJar);
				result = executor.execute(commands, path.toFile());
				StringBuilder stdout = executor.getStdout();
				if (stdout != null && stdout.length() > 0)
					logger.debug(stdout.toString());
				StringBuilder stderr = executor.getStderr();
				if (stderr != null && stderr.length() > 0)
					logger.error(stderr.toString());
				if (result != 0) {
					break;
				}
			}
		}
		return result;
	}

	int test(String namedTask) throws IOException, InterruptedException {
		int result = 0;

		// check if test case execution is enabled
		if (skipTest)
			return result;

		// check if any test cases have been provided
		if (test == null)
			return result;

		for (Map.Entry<String, Object> entry : test.entrySet()) {
			String testName = entry.getKey();
			logger.info("testName = " + testName);
			Map<String, Object> testInfo = (Map<String, Object>) entry.getValue();
			// get server entry and start servers one by one.
			List<Map<String, Object>> servers = (List<Map<String, Object>>) testInfo.get(Constants.SERVER);
			for (Map<String, Object> server : servers) {
				String path = (String) server.get(Constants.PATH);
				String cmd = (String) server.get(Constants.CMD);
				logger.info("start server at " + path + " with " + cmd);
				Path cmdPath = getRepositoryPath(userHome, workspace, path);

				List<String> commands = new ArrayList<>();
				commands.add("nohup");
				commands.add("bash");
				commands.add("-c");
				String c = cmdPath.toString() + "/" + cmd;
				commands.add("java -jar " + c);
				result = executor.startServer(commands, cmdPath.toFile());
				StringBuilder stdout = executor.getStdout();
				if (stdout != null && stdout.length() > 0)
					logger.debug(stdout.toString());
				StringBuilder stderr = executor.getStderr();
				if (stderr != null && stderr.length() > 0)
					logger.error(stderr.toString());
				if (result != 0) {
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
				List<Map<String, Object>> requests = (List<Map<String, Object>>) testInfo.get(Constants.REQUEST);
				for (Map<String, Object> request : requests) {
					String host = (String) request.get(Constants.HOST);
					String path = (String) request.get(Constants.PATH);
					String method = (String) request.get(Constants.METHOD);
					Map<String, Object> requestHeader = (Map<String, Object>) request.get(Constants.HEADER);
					String requestBody = (String) request.get(Constants.BODY);
					logger.info("host = {}, path={}, method={}, request header={}, request body={}", host, path, method,
							requestHeader, requestBody);
					Map<String, Object> response = (Map<String, Object>) request.get(Constants.RESPONSE);
					int status = (Integer) response.get(Constants.STATUS);
					Map<String, Object> responseHeader = (Map<String, Object>) response.get(Constants.HEADER);
					Map<String, Object> responseBodyMap = (Map<String, Object>) response.get(Constants.BODY);
					logger.info("expected status = " + status + " response header = " + responseHeader
							+ " response body = " + responseBodyMap);
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
						cr = TestUtil.requestWithBody(host, path, TestUtil.toHttpString(method), requestHeader,
								requestBody);
						break;
					}

					int statusCode = cr.getResponseCode();
					HeaderMap responseHeaderMap = cr.getResponseHeaders();
					String responseBody = cr.getAttachment(Http2Client.RESPONSE_BODY);
					logger.info("returned statusCode = " + statusCode + " headerMap = " + responseHeaderMap
							+ " responseBody = " + responseBody);
					// check response with the config.
					if (status != statusCode) {
						result = -1;
						logger.error("config status {} doesn't match the response statusCode {}", status, statusCode);
						break;
					}

					if (!TestUtil.matchHeader(responseHeader, responseHeaderMap)) {
						result = -1;
						logger.error("config header {} doesn't match the response headerMap {}", responseHeader,
								responseHeaderMap);
						break;
					}

					if (!TestUtil.matchBody(responseBodyMap, responseBody)) {
						result = -1;
						logger.error("config body {} doesn't match the response body {}", responseBodyMap,
								responseBody);
						break;
					}
				}
			} finally {
				// shutdown servers, this needs to be called even if there are exceptions during
				// tests.
				logger.info("shutdown servers");
				executor.stopServers();
			}

			if (result != 0) {
				break;
			}
		}
		return result;
	}

	int copyFile(String namedTask) throws IOException, InterruptedException {
		int result = 0;
		if (skipCopyFile)
			return result;
		for (Map<String, String> copyFile : copyFiles) {
			String src = copyFile.get("src");
			String dst = copyFile.get("dst");
			logger.info("Copying: FROM - " + src + " TO - " + dst);
			CopyFileCmd copyFileCmd = new CopyFileCmd(userHome, workspace, src, dst);
			copyFileCmd.execute();
		}
		return result;
	}

	int copyWildcardFile(String namedTask) throws IOException, InterruptedException {
		int result = 0;
		if (skipCopyWildcardFile)
			return result;
		for (Map<String, String> copyFile : copyWildcardFiles) {
			String src = copyFile.get("src");
			String dst = copyFile.get("dst");
			String pattern = copyFile.get("pattern");
			logger.info("Copying from " + src + " to " + dst + " for pattern " + pattern);
			CopyWildcardFileCmd copyWildcardFileCmd = new CopyWildcardFileCmd(userHome, workspace, src, dst, pattern);
			copyWildcardFileCmd.execute();
		}
		return result;
	}

	int start(String namedTask) throws IOException, InterruptedException {
		int result = 0;

		// check if start service execution is enabled
		if (skipStart || start == null)
			return result;

		// get the start tasks for the named task
		Map<String, Object> namedStart = (Map<String, Object>) start.get(namedTask);

		// check whether this task must be skipped
		if ((Boolean) namedStart.get(Constants.SKIP))
			return result;

		for (Map.Entry<String, Object> entry : ((Map<String, Object>) namedStart.get(Constants.SERVICES)).entrySet()) {
			String startName = entry.getKey();
			logger.info("ServiceName = " + startName);
			Map<String, Object> startInfo = (Map<String, Object>) entry.getValue();
			// get server entry and start servers one by one.
			List<Map<String, Object>> servers = (List<Map<String, Object>>) startInfo.get(Constants.SERVER);
			for (Map<String, Object> server : servers) {
				String path = (String) server.get(Constants.PATH);
				String cmd = (String) server.get(Constants.CMD);
				String host = (String) server.get(Constants.HOST);
				int port = (Integer) server.get(Constants.PORT);
				int timeout = (Integer) server.get(Constants.TIMEOUT);
				
				Object dir = server.get(Constants.CONFIG_DIR);
				String configDir = "config";
				if(server.get(Constants.CONFIG_DIR)!=null)
					configDir = (String)server.get(Constants.CONFIG_DIR);

				logger.info("*** start server process ***");
				logger.info("start server in project: " + path + " with target:" + cmd + " and light-4j config directory: " + configDir);
				Path cmdPath = getRepositoryPath(userHome, workspace, path);

				List<String> commands = new ArrayList<>();
				commands.add("nohup");
				commands.add("bash");
				commands.add("-c");

				// add Java start-up command
				String c = cmdPath.toString() + "/" + cmd;
				commands.add("java " + "-D" + Constants.LIGHT_4J_CONFIG_DIR + "=" + cmdPath.toString() + "/" + configDir + " -jar " + c);
				
				// start the server with env variables set
				result = executor.startServer(commands, cmdPath.toFile());
				
				StringBuilder stdout = executor.getStdout();
				if (stdout != null && stdout.length() > 0)
					logger.debug(stdout.toString());
				StringBuilder stderr = executor.getStderr();
				if (stderr != null && stderr.length() > 0)
					logger.error(stderr.toString());
				if (result != 0) {
					logger.info("Start server failed for " + c);
					break;
				}

				// put a sleep in case the server is not ready.
				logger.info("wait time - allow server to initialize fully: " + timeout + " ms");
				Thread.sleep(timeout);
			}

			// execute test cases
			logger.info("start testing...");
			logger.info("*** start process completed ***");
		}

		return result;
	}

	void stop() throws IOException, InterruptedException {
		// shutdown servers, this needs to be called even if there are exceptions during
		// tests.
		logger.info("shutdown servers");
		executor.stopServers();
	}
}
