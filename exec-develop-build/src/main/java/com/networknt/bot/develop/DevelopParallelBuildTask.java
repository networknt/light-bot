package com.networknt.bot.develop;

import com.networknt.bot.core.Constants;
import com.networknt.bot.core.LoggingOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class DevelopParallelBuildTask extends DevelopBuildTask {
    private static final Logger logger = LoggerFactory.getLogger(DevelopParallelBuildTask.class);
    private static final PrintStream errorPrintStream = new PrintStream(new LoggingOutputStream(logger,
            LoggingOutputStream.LogLevel.ERROR));

    @Override
    int build(String namedTask) throws IOException, InterruptedException {
        int result = 0;
        if (skipBuild || build == null)
            return result;

        // get the build tasks for the named task
        Map<String, Object> namedBuild = (Map<String, Object>) build.get(namedTask);

        // check whether this task must be skipped
        if ((Boolean) namedBuild.get(Constants.SKIP))
            return result;

        // check whether this build task must be executed with running tests or not
        final boolean skipNamedTests = (Boolean) Optional.ofNullable(namedBuild.get(Constants.SKIP_TEST))
                .orElse(Boolean.FALSE);


        // iterate through the list of projects to build
        List<String> builds = (List<String>) namedBuild.get(Constants.PROJECT);

        Function<String, Integer> individualBuild = build -> {
            int exitValue = -1;
            Path path = getRepositoryPath(userHome, workspace, build);
            if (Files.notExists(path)) {
                logger.error("Path doesn't exist " + build);
            } else {
                // switch to branch and pull
                List<String> commands = new ArrayList<>();
                commands.add("bash");
                commands.add("-c");

                String mavenCmd = "mvn clean install";
                if(skipNamedTests || (!skipNamedTests && skipTest))
                    mavenCmd += " -Dmaven.test.skip=true";

                if (!skipGenerateEclipseProject)
                    mavenCmd += " eclipse:eclipse";

                commands.add(mavenCmd);

                logger.info("mvn clean install for " + build);
                try {
                    exitValue = executor.execute(commands, path.toFile());
                    String stdout = executor.getStdout();
                    if (stdout != null && stdout.length() > 0)
                        logger.debug(stdout);
                    String stderr = executor.getStderr();
                    if (stderr != null && stderr.length() > 0)
                        logger.error(stderr);
                } catch (Exception e) {
                    e.printStackTrace(errorPrintStream);
                }
            }

            if (exitValue != 0) {
                logger.error("build {} failed with exitValue {}", build, exitValue);
            } else {
                logger.debug("build {} succeeded with exitValue {}", build, exitValue);
            }

            return exitValue;
        };

        boolean parallel = (Boolean) Optional.ofNullable(namedBuild.get(Constants.PARALLEL)).orElse(Boolean.FALSE);
        Stream<String> buildStream;
        if (parallel) {
            logger.debug("parallel build enabled for {}", builds);
            buildStream = builds.parallelStream();
        } else {
            logger.debug("parallel build not enabled for {}; will run them serially", builds);
            buildStream = builds.stream();
        }
        if (buildStream.map(individualBuild).anyMatch(exitValue -> exitValue < 0)){
            result = -1;
            logger.debug("at least one of the builds {} has failed.", builds);
        } else {
            logger.debug("builds {} are all good.", builds);
        }

        return result;
    }

}
