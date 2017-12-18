package net.lightapi.bot.executor;

import com.networknt.config.Config;
import com.networknt.service.SingletonServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TestCommand implements Command {
    private static final Logger logger = LoggerFactory.getLogger(TestCommand.class);
    public static final String CONFIG_NAME = "light-bot";
    Executor executor = SingletonServiceFactory.getBean(Executor.class);
    Map<String, Object> config = Config.getInstance().getJsonMapConfig(CONFIG_NAME);
    String workspace = (String)config.get(Constants.WORKSPACE);
    Map<String, Object> test = (Map<String, Object>)config.get(Constants.TEST);
    String userHome = System.getProperty("user.home");

    @Override
    public int execute() throws IOException, InterruptedException {
        int result = 0;

        for(Map.Entry<String, Object> entry : test.entrySet()) {
            String testName = entry.getKey();
            Map<String, Object> testInfo = (Map<String, Object>)entry.getValue();

            // get server entry and start server one by one.
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
                    break;
                }
            }
            // execute test cases

            // shutdown servers
            executor.stopServers();
            if(result != 0) {
                break;
            }
        }
        return result;
    }
}
