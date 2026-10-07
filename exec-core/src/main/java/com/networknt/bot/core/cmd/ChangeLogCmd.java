package com.networknt.bot.core.cmd;

import com.networknt.bot.core.Command;
import com.networknt.bot.core.Executor;
import com.networknt.bot.core.Constants;
import com.networknt.service.SingletonServiceFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

/**
 * This is a command that is used to generate change log before releasing. There are two releases in the light platform
 * release-maven to build jar and push the jar to the maven central
 * release-docker to build docker image and push it to the docker hub
 *
 * For some repositories like light-proxy and light-router, we have to release both. This will cause the changelog cmd
 * twice, we need to check if the changelog has already generated before adding a new release entry.
 *
 * @author Steve Hu
 */
public class ChangeLogCmd implements Command {
    private final Executor executor;
    private final String organization;
    private final String repository;
    private final String version;
    private final String prevTag;
    private final Path rPath;

    public ChangeLogCmd(String organization, String repository, String version, String prevTag, Path rPath) {
        this(organization, repository, version, prevTag, rPath, SingletonServiceFactory.getBean(Executor.class));
    }

    ChangeLogCmd(String organization, String repository, String version, String prevTag, Path rPath, Executor executor) {
        this.organization = organization;
        this.repository = repository;
        this.version = version;
        this.prevTag = prevTag;
        this.rPath = rPath;
        this.executor = executor;
    }

    @Override
    public int execute() throws IOException, InterruptedException {
        List<String> genLog = genChangelog();
        Path file = rPath.resolve("CHANGELOG.md");
        List<String> fileContent = Files.exists(file)
                ? new ArrayList<>(Files.readAllLines(file, StandardCharsets.UTF_8))
                : new ArrayList<>(Arrays.asList("# Changelog", ""));
        // Preserve the preamble and older releases; replace the current version wherever it occurs.
        int insertion = fileContent.size();
        for (int i = 0; i < fileContent.size(); i++) {
            if (fileContent.get(i).startsWith("## [")) {
                insertion = i;
                break;
            }
        }
        Pattern currentRelease = Pattern.compile("^## \\[" + Pattern.quote(version) + "\\](?:\\(|\\s|$)");
        for (int i = fileContent.size() - 1; i >= insertion; i--) {
            if (currentRelease.matcher(fileContent.get(i)).find()) {
                int end = i + 1;
                while (end < fileContent.size() && !fileContent.get(end).startsWith("## [")) end++;
                fileContent.subList(i, end).clear();
            }
        }
        fileContent.addAll(insertion, genLog);
        Files.write(file, fileContent, StandardCharsets.UTF_8);
        return 0;
    }

    public List<String> genChangelog() throws IOException, InterruptedException {
        List<String> logs = new ArrayList<>();
        Date date = new Date();
        String modifiedDate= new SimpleDateFormat("yyyy-MM-dd").format(date);

        String tagLine = String.format("## [%s](https://github.com/%s/%s/tree/%s) (%s)", version, organization, repository, version, modifiedDate);
        logs.add(tagLine);
        logs.add("");
        logs.add("**Commits:**");
        logs.add("");
        logs.addAll(genCommitEntries());
        logs.add("");
        return logs;
    }

    private List<String> genCommitEntries() throws IOException, InterruptedException {
        if (prevTag == null || prevTag.trim().isEmpty()) {
            throw new IOException("prev_tag is required for commit changelogs");
        }
        // Resolve an actual tag to a commit before building the range; never interpret config as shell code.
        String previous = runGit("rev-parse", "--verify",
                "refs/tags/" + prevTag + "^{commit}").trim();
        String head = runGit("rev-parse", "--verify", "HEAD^{commit}").trim();
        int ancestry = executor.execute(Arrays.asList("git", "merge-base", "--is-ancestor", previous, head), rPath.toFile());
        if (ancestry == 1) throw new IOException("prev_tag '" + prevTag + "' is not an ancestor of HEAD");
        if (ancestry != 0) throw gitFailure("git merge-base --is-ancestor", ancestry);
        String output = runGit("log", "--no-merges", "--no-show-signature", "--format=%H%x09%an%x09%s", previous + ".." + head, "--");
        List<String> entries = new ArrayList<>();
        for (String line : output.split("\\R")) {
            if (line.isEmpty()) continue;
            String[] commit = line.split("\t", 3);
            if (commit.length != 3 || !commit[0].matches("[0-9a-f]{40,64}")) {
                throw new IOException("Unexpected git log record");
            }
            String subject = commit[2];
            if (subject.equals(Constants.CHANGELOG_CHECKIN_MESSAGE) || subject.equals("bot checkin")
                    || subject.startsWith("[maven-release-plugin] prepare release ")
                    || subject.equals("[maven-release-plugin] prepare for next development iteration")) continue;
            entries.add(String.format("- %s ([%s](https://github.com/%s/%s/commit/%s)) (by %s)",
                    escapeMarkdown(subject), commit[0].substring(0, 7), organization, repository, commit[0], escapeMarkdown(commit[1])));
        }
        return entries;
    }

    private String runGit(String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(Arrays.asList(args));
        int result = executor.execute(command, rPath.toFile());
        if (result != 0) throw gitFailure(String.join(" ", command), result);
        String output = executor.getStdout();
        return output == null ? "" : output;
    }

    private IOException gitFailure(String command, int result) {
        String stderr = executor.getStderr();
        String detail = stderr == null ? "" : stderr.trim();
        return new IOException("Git changelog command failed (" + result + "): " + command
                + (detail.isEmpty() ? "" : ": " + detail));
    }

    private static String escapeMarkdown(String text) {
        return text.replace("\\", "\\\\").replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("*", "\\*").replace("_", "\\_").replace("[", "\\[").replace("]", "\\]")
                .replace("`", "\\`").replace("~", "\\~").replace("@", "@\u200B");
    }

    @Override
    public String getName() {
        return "ChangeLog";
    }
}
