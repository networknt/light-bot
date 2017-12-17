package net.lightapi.bot.executor;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface Executor {
     int execute(List<String> commands, File workingDir) throws IOException, InterruptedException;
     StringBuilder getStdout();
     StringBuilder getStderr();

}
