package com.networknt.bot.core;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface Executor {
     int execute(List<String> commands, File workingDir) throws IOException, InterruptedException;
     int startServer(List<String> commands, File workingDir) throws IOException, InterruptedException;
     int startServer(List<String> commands, Map<String,String> envVars, File workingDir) throws IOException, InterruptedException;
     void stopServers();
     StringBuilder getStdout();
     StringBuilder getStderr();
}
