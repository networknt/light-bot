package com.networknt.bot.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.networknt.bot.core.Command;
import com.networknt.bot.core.TaskRegistry;
import com.networknt.config.Config;
import com.networknt.email.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.MessagingException;
import java.io.File;
import java.util.Map;
import java.util.Set;

public class Cli {
    static final Logger logger = LoggerFactory.getLogger(Cli.class);
    private static final String CONFIG_NAME = "cli";
    private static Map<String, Object> config = Config.getInstance().getJsonMapConfig(CONFIG_NAME);

    private boolean skipEmail = false;

    @Parameter(names={"--task", "-t"})
    String task;

    public static void main(String ... argv) {
        Cli cli = new Cli();
        JCommander.newBuilder()
                .addObject(cli)
                .build()
                .parse(argv);
        cli.run();
        System.exit(0);
    }

    public void run() {
        logger.info("Cli starts with task = %s", task);
        TaskRegistry registry = TaskRegistry.getInstance();
        Set<String> tasks = registry.getTasks();
        if(tasks.contains(task)) {
            Command command = registry.getCommand(task);
            try {
                int result = command.execute();
                if(result == 11) {
                    logger.info("none of the repo has been changed, skip build!");
                } else if(result == 0) {
                    logger.info("build successfully!");
                } else {
                    logger.error("build or test failed!");
                    // send email here with the attachment bot.log
                    // send email to stevehu@gmail.com
                    if(config != null) skipEmail = config.get("skipEmail") == null?  false : ((Boolean)config.get("skipEmail")).booleanValue();
                    if(!skipEmail) {
                        EmailSender emailSender = new EmailSender();
                        try {
                            File file = new File("bot.log");
                            String absolutePath = file.getAbsolutePath();
                            emailSender.sendMailWithAttachment("stevehu@gmail.com", "Build Error", "Please check the build log", absolutePath);
                        } catch (MessagingException e) {
                            logger.error("Failed to send email ", e);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Exception", e);
            }
        } else {
            logger.error("Invalid task %s", task);
        }
    }
}
