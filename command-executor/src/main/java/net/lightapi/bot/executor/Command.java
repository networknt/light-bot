package net.lightapi.bot.executor;

import java.io.IOException;

public interface Command {
    int execute() throws IOException, InterruptedException;
}
