package com.networknt.bot.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;

import java.io.*;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class FasterRollingCommandExecutor extends CommandExecutor implements InOutErrStreamProvider {
    private static final Logger logger = LoggerFactory.getLogger(FasterRollingCommandExecutor.class);

    InputStream inputStream = System.in;
    // OutputStream logStream = new LoggingOutputStream(logger, LoggingOutputStream.LogLevel.INFO);
    // OutputStream errorLogStream = new LoggingOutputStream(logger, LoggingOutputStream.LogLevel.ERROR);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    ByteArrayOutputStream errorStream = new ByteArrayOutputStream();

    @Override
    public int execute(List<String> commands, File workingDir) throws IOException, InterruptedException {
        logger.info("about to execute this concat command {} in this working directory {} ", commands, workingDir);
        outputStream = new ByteArrayOutputStream();
        errorStream = new ByteArrayOutputStream();
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
        return this.inputStream;
    }

    @Override
    public OutputStream getOutputStream() {
        return this.outputStream;
    }

    @Override
    public OutputStream getErrorStream() {
        return this.errorStream;
    }

    /**
     * Get the standard output (stdout) from the command you just exec'd.
     */
    @Override
    public String getStdout() {
        return new String(outputStream.toByteArray());
    }

    /**
     * Get the standard error (stderr) from the command you just exec'd.
     */
    @Override
    public String getStderr() {
        return new String(errorStream.toByteArray());
    }
}
