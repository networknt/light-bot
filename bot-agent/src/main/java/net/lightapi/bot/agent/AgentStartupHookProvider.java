package net.lightapi.bot.agent;

import com.networknt.config.Config;
import com.networknt.server.StartupHookProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AgentStartupHookProvider implements StartupHookProvider {
    final static Logger logger = LoggerFactory.getLogger(AgentStartupHookProvider.class);
    AgentConfig config = (AgentConfig) Config.getInstance().getJsonObjectConfig(Constants.CONFIG_NAME, AgentConfig.class);

    @Override
    public void onStartup() {
        // create the workspace if it doesn't exist
        String workspace = config.getWorkspace();
        String userHome = System.getProperty("user.home");
        Path path = Paths.get(userHome, workspace);
        if(!Files.exists(path)) {
            logger.debug("workspace doesn't exist. create one");
            try {
                Files.createDirectory(path);
            } catch (IOException e) {
                logger.error("IOException", e);
            }
        }
    }
}
