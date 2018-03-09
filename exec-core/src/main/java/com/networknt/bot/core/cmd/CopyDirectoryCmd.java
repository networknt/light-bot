package com.networknt.bot.core.cmd;

import com.networknt.bot.core.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;

public class CopyDirectoryCmd implements Command {
    private static final Logger logger = LoggerFactory.getLogger(CopyDirectoryCmd.class);

    private Path sPath;
    private Path dPath;

    public CopyDirectoryCmd(String workspace, String src, String dst) {
        this.sPath = Paths.get(workspace, src);
        this.dPath = Paths.get(workspace, dst);
    }

    @Override
    public int execute() throws IOException, InterruptedException {
        CustomFileVisitor fileVisitor = new CustomFileVisitor(sPath, dPath);
        //You can specify your own FileVisitOption
        Files.walkFileTree(sPath, fileVisitor);
        logger.info("Copy directory from " + sPath + " to " + dPath);
        return 0;
    }

    @Override
    public String getName() {
        return "CopyDirectory";
    }

    class CustomFileVisitor extends SimpleFileVisitor<Path> {

        final Path source;
        final Path target;

        public CustomFileVisitor(Path source, Path target) {
            this.source = source;
            this.target = target;
        }



        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                throws IOException
        {

            Path newDirectory= target.resolve(source.relativize(dir));
            try{
                Files.copy(dir,newDirectory);
            }
            catch (FileAlreadyExistsException ioException){
                //log it and move
                return SKIP_SUBTREE; // skip processing
            }

            return CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

            Path newFile = target.resolve(source.relativize(file));

            try{
                Files.copy(file,newFile);
            }
            catch (IOException ioException){
                //log it and move
            }

            return CONTINUE;

        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {


            return CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            if (exc instanceof FileSystemLoopException) {
                //log error
            } else {
                //log error
            }
            return CONTINUE;
        }
    }
}
