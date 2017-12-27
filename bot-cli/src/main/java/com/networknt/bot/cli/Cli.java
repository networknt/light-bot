package com.networknt.bot.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.networknt.bot.core.Command;
import com.networknt.bot.core.TaskRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class Cli {
    static final Logger logger = LoggerFactory.getLogger(Cli.class);

    @Parameter(names={"--task", "-t"})
    String task;

    public static void main(String ... argv) {
        Cli cli = new Cli();
        JCommander.newBuilder()
                .addObject(cli)
                .build()
                .parse(argv);
        cli.run();
    }

    public void run() {
        logger.info("Cli starts with task = %s", task);
        TaskRegistry registry = TaskRegistry.getInstance();
        Set<String> tasks = registry.getTasks();
        if(tasks.contains(task)) {
            Command command = registry.getCommand(task);
            try {
                int result = command.execute();
                if(result == 1) {
                    System.out.println("none of the repo has been changed");
                } else {
                    System.out.println("at least one repo is changed");
                }
            } catch (Exception e) {
                logger.error("Exception", e);
            }
        } else {
            logger.error("Invalid task %s", task);
        }
    }
}
