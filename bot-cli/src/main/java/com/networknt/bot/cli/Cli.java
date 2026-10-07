package com.networknt.bot.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.networknt.bot.core.Command;
import com.networknt.bot.core.Constants;
import com.networknt.bot.core.TaskRegistry;
import com.networknt.config.Config;
import com.networknt.email.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.mail.*;
import java.io.File;
import java.util.Map;
import java.util.Set;

public class Cli {
    static final Logger logger = LoggerFactory.getLogger(Cli.class);
    private static final String CONFIG_NAME = "cli";
    private static final Map<String, Object> config = Config.getInstance().getJsonMapConfig(CONFIG_NAME);

    private boolean skipEmail = false;
    private String email = "steve.hu@gmail.com";

    @Parameter(names={"--task", "-t"})
    String task;

    public static void main(String ... argv) {
        Cli cli = new Cli();
        JCommander.newBuilder()
                .addObject(cli)
                .build()
                .parse(argv);
        System.exit(cli.run());
    }

    public int run() {
        logger.info("Cli starts with task = " + task);
        TaskRegistry registry = TaskRegistry.getInstance();
        Set<String> tasks = registry.getTasks();
        if(tasks.contains(task)) {
            Command command = registry.getCommand(task);
            return runCommand(command);
        } else {
            logger.error("Invalid task " + task);
            return 1;
        }
    }

    int runCommand(Command command) {
        int result;
        try {
            result = command.execute();
        } catch (Exception e) {
            logger.error("Task failed", e);
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            result = 1;
        }
        if (result == Constants.NO_REPO_CHANGE) {
            logger.info("none of the repo has been changed, skip build!");
            return 0;
        }
        if (result == 0) {
            logger.info("build successfully!");
        } else {
            logger.error("build or test failed!");
            try {
                notifyFailure();
            } catch (Exception e) {
                logger.error("Failed to send failure notification", e);
            }
        }
        return result;
    }

    void notifyFailure() throws MessagingException {
        if (config != null) {
            skipEmail = Boolean.TRUE.equals(config.get("skipEmail"));
            email = (String)config.getOrDefault("email", "steve.hu@gmail.com");
        }
        if (!skipEmail) {
            new EmailSender().sendMailWithAttachment(email, "Build Error", "Please check the build log",
                    new File("bot.log").getAbsolutePath());
        }
    }
}
