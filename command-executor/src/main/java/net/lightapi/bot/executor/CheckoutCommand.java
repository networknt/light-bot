package net.lightapi.bot.executor;

import com.networknt.config.Config;
import com.networknt.service.SingletonServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Multiple commands are defined in a map and executed within current directory.
 * Command are grouped within a working directory which is a sub folder from
 * parent.
 */
public class CheckoutCommand implements Command {
    private static final Logger logger = LoggerFactory.getLogger(CheckoutCommand.class);
    public static final String CONFIG_NAME = "checkout";
    Executor executor = SingletonServiceFactory.getBean(Executor.class);
    Map<String, Object> config = Config.getInstance().getJsonMapConfig(CONFIG_NAME);
    String workspace = (String)config.get(Constants.WORKSPACE);
    Map<String, Object> checkout = (Map<String, Object>)config.get(Constants.CHECKOUT);
    String branch = (String)checkout.get(Constants.BRANCH);
    List<String> repositories = (List<String>)checkout.get(Constants.REPOSITORY);
    String userHome = System.getProperty("user.home");

    @Override
    public int execute() throws IOException, InterruptedException {
        // check if there is a directory workspace in home directory.
        Path wPath = Paths.get(userHome, workspace);
        if(Files.notExists(wPath)) {
            Files.createDirectory(wPath);
        }

        int result = 0;

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
                logger.debug(stdout.toString());
                StringBuilder stderr = executor.getStderr();
                logger.error(stderr.toString());
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
                logger.debug(stdout.toString());
                stderr = executor.getStderr();
                logger.error(stderr.toString());
                if(result != 0) {
                    break;
                }

            } else {
                // switch to branch and pull
                List<String> commands = new ArrayList<>();
                commands.add("bash");
                commands.add("-c");
                commands.add("git checkout " + branch + " ; git pull origin " + branch);
                logger.info("git checkout " + branch + " ; git pull origin " + branch);
                result = executor.execute(commands, rPath.toFile());
                StringBuilder stdout = executor.getStdout();
                logger.debug(stdout.toString());
                StringBuilder stderr = executor.getStderr();
                logger.error(stderr.toString());
                if(result != 0) {
                    break;
                }
            }
        }
        return result;
    }

    public static String getDirFromRepo(String repository) {
        return repository.substring(repository.lastIndexOf("/") + 1, repository.lastIndexOf("."));
    }
}
