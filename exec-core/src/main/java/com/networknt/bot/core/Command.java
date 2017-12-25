package com.networknt.bot.core;

import java.io.IOException;

public interface Command {
    int execute() throws IOException, InterruptedException;

    default String getDirFromRepo(String repository) {
        return repository.substring(repository.lastIndexOf("/") + 1, repository.lastIndexOf("."));
    }

}
