package com.networknt.bot.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;

import java.io.*;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class FasterRollingCommandExecutor extends CommandExecutor implements InOutErrStreamProvider {
    private static final Logger logger = LoggerFactory.getLogger(FasterRollingCommandExecutor.class);

    InputStream userInputStream = System.in;
    OutputStream logStream = new LoggingOutputStream(logger, LoggingOutputStream.LogLevel.INFO);
    OutputStream errorLogStream = new LoggingOutputStream(logger, LoggingOutputStream.LogLevel.ERROR);

    @Override
    public int execute(List<String> commands, File workingDir) throws IOException, InterruptedException {
        logger.info("about to execute this concat command {} in this working directory {} ", commands, workingDir);
        try {
            return new ProcessExecutor().directory(workingDir).command(commands)
                    .redirectInput(getInputStream())
                    .redirectOutput(getOutputStream())
                    .redirectError(getErrorStream())
                    .execute().getExitValue();
        } catch (TimeoutException e) {
            e.printStackTrace(new PrintStream(getErrorStream()));
            throw new InterruptedException("command timed out");
        }
    }

    @Override
    public InputStream getInputStream() {
        return this.userInputStream;
    }

    @Override
    public OutputStream getOutputStream() {
        return this.logStream;
    }

    @Override
    public OutputStream getErrorStream() {
        return this.errorLogStream;
    }
}
