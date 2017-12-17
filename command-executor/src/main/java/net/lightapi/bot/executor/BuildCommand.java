package net.lightapi.bot.executor;

import com.networknt.config.Config;
import com.networknt.service.SingletonServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BuildCommand implements Command {
    private static final Logger logger = LoggerFactory.getLogger(BuildCommand.class);
    public static final String CONFIG_NAME = "light-bot";
    Executor executor = SingletonServiceFactory.getBean(Executor.class);
    Map<String, Object> config = Config.getInstance().getJsonMapConfig(CONFIG_NAME);
    String workspace = (String)config.get(Constants.WORKSPACE);
    List<String> builds = (List<String>)config.get(Constants.BUILD);
    String userHome = System.getProperty("user.home");

    @Override
    public int execute() throws IOException, InterruptedException {
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
                if(stdout.length() > 0) logger.debug(stdout.toString());
                StringBuilder stderr = executor.getStderr();
                if(stderr.length() > 0) logger.error(stderr.toString());
                if(result != 0) {
                    break;
                }
            }
        }
        return result;
    }
}
