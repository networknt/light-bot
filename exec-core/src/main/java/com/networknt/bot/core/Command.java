package com.networknt.bot.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public interface Command {
    int execute() throws IOException, InterruptedException;

    String getName();

    default String getDirFromRepo(String repository) {
        return repository.substring(repository.lastIndexOf("/") + 1, repository.lastIndexOf("."));
    }

    /**
     * Get the repository path which is normally the working path. If the workspace starts with
     * a leading "/", then it is absolute path. Otherwise, the workspace is in the user home directory.
     *
     * @param userHome user home directory
     * @param workspace workspace from the config file
     * @param repository the git repository from the config file
     * @return the working path
     */
    default Path getRepositoryPath(String userHome, String workspace, String... repository) {
        if(workspace.startsWith("/")) {
            return Paths.get(workspace, repository);
        } else {
            return Paths.get(userHome + File.separator + workspace, repository);
        }
    }

    /**
     * Get the workspace path. If the workspace is starting with a leading "/", then use it
     * as an absolute path, otherwise, get the path from the user home directory.
     *
     * @param userHome user home directory
     * @param workspace workspace from the config file
     * @return the workspace path
     */
    default Path getWorkspacePath(String userHome, String workspace) {
        if(workspace.startsWith("/")) {
            return Paths.get(workspace);
        } else {
            return Paths.get(userHome, workspace);
        }
    }
}
