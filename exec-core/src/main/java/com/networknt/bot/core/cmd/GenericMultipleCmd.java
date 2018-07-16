package com.networknt.bot.core.cmd;

import com.networknt.bot.core.Command;
import com.networknt.bot.core.Executor;
import com.networknt.service.SingletonServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * This is very similar with GenericSingleCmd with multiple commands to be executed at the same time.
 *
 * @author Steve Hu
 */
public class GenericMultipleCmd implements Command {
    private static final Logger logger = LoggerFactory.getLogger(GenericMultipleCmd.class);
    private Executor executor = SingletonServiceFactory.getBean(Executor.class);
    private List<String> cmds;
    private Path rPath;

    public GenericMultipleCmd(List<String> cmds, Path rPath) {
        this.cmds = cmds;
        this.rPath = rPath;
    }

    @Override
    public int execute() throws IOException, InterruptedException {
        int result;
        List<String> commands = new ArrayList<>();
        commands.add("bash");
        commands.add("-c");
        String command = "";
        for(String cmd : cmds){
            command += cmd + " ; ";
        }
        commands.add(command);
        logger.info(command  + " in " + rPath);
        result = executor.execute(commands, rPath.toFile());
        StringBuilder stdout = executor.getStdout();
        if(stdout != null && stdout.length() > 0) logger.debug(stdout.toString());
        StringBuilder stderr = executor.getStderr();
        if(stderr != null && stderr.length() > 0) logger.error(stderr.toString());
        return result;
    }

    @Override
    public String getName() {
        return "GenericMultiple";
    }

}
