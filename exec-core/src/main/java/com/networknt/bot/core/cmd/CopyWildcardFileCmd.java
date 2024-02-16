package com.networknt.bot.core.cmd;

import com.networknt.bot.core.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import static java.nio.file.FileVisitResult.CONTINUE;

public class CopyWildcardFileCmd implements Command {
    private static final Logger logger = LoggerFactory.getLogger(CopyFileCmd.class);

    String userHome;
    String workspace;
    String src;
    String dst;
    String glob;
    List<Path> files;

    public CopyWildcardFileCmd(String userHome, String workspace, String src, String dst, String glob) {
        this.userHome = userHome;
        this.workspace = workspace;
        this.src = src;
        this.dst = dst;
        this.glob = glob;

    }

    @Override
    public int execute() throws IOException, InterruptedException {
        files = new ArrayList<>();
        Finder finder = new Finder(glob);
        Files.walkFileTree(getRepositoryPath(userHome, workspace, src), finder);
        for(Path file : files) {
            Path s = getRepositoryPath(userHome, workspace, src, file.toString());
            Path d = getRepositoryPath(userHome, workspace, dst, file.toString());
            Files.copy(s, d, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Copy file from " + s + " to " + d);
        }
        return 0;
    }

    @Override
    public String getName() {
        return "CopyWildcardFile";
    }


    public class Finder extends SimpleFileVisitor<Path> {

        private final PathMatcher matcher;
        private int numMatches = 0;

        Finder(String pattern) {
            matcher = FileSystems.getDefault()
                    .getPathMatcher("glob:" + pattern);
        }

        // Compares the glob pattern against
        // the file or directory name.
        void find(Path file) {
            Path name = file.getFileName();
            if (name != null && matcher.matches(name)) {
                numMatches++;
                files.add(name);
            }
        }

        // Invoke the pattern matching
        // method on each file.
        @Override
        public FileVisitResult visitFile(Path file,
                                         BasicFileAttributes attrs) {
            find(file);
            return CONTINUE;
        }

        // Invoke the pattern matching
        // method on each directory.
        @Override
        public FileVisitResult preVisitDirectory(Path dir,
                                                 BasicFileAttributes attrs) {
            find(dir);
            return CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file,
                                               IOException exc) {
            System.err.println(exc);
            return CONTINUE;
        }
    }
}
