package com.networknt.bot.core.cmd;

import com.networknt.bot.core.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class CopyFileCmd implements Command {
    private static final Logger logger = LoggerFactory.getLogger(CopyFileCmd.class);

    private Path sPath;
    private Path dPath;

    public CopyFileCmd(String userHome, String workspace, String src, String dst) {
        // check if the sPath is Glob pattern, if yes, handle it accordingly.
        this.sPath = getRepositoryPath(userHome, workspace, src);
        this.dPath = getRepositoryPath(userHome, workspace, dst);
    }

    @Override
    public int execute() throws IOException, InterruptedException {
        Files.copy(sPath, dPath, StandardCopyOption.REPLACE_EXISTING);
        logger.info("Copy file from " + sPath + " to " + dPath);
        return 0;
    }

    @Override
    public String getName() {
        return "CopyFile";
    }
}
