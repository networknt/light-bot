package com.networknt.bot.core.cmd;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CommitChangeLogCmdTest {
    @TempDir
    Path repository;

    private IsolatedGitExecutor executor() throws IOException {
        return new IsolatedGitExecutor(repository);
    }

    private String git(String... args) throws Exception {
        List<String> command = new ArrayList<>(Arrays.asList("git", "-c", "user.name=Test",
                "-c", "user.email=test@example.com", "-c", "commit.gpgsign=false", "-c", "tag.gpgsign=false"));
        command.addAll(Arrays.asList(args));
        IsolatedGitExecutor executor = executor();
        int result = executor.execute(command, repository.toFile());
        String output = executor.getStdout();
        assertEquals(0, result, executor.getStderr());
        return output.trim();
    }

    private void initialize() throws Exception {
        git("init", "-b", "main");
        git("commit", "--allow-empty", "-m", "Already released");
        git("tag", "-a", "1.0.0", "-m", "Previous release");
    }

    private ChangeLogCmd command(String tag) throws IOException {
        return new ChangeLogCmd("example", "demo", "1.1.0", tag, repository, executor());
    }

    @Test
    void includesDirectAndPrCommitsByAncestryWithLinksAndEscaping() throws Exception {
        initialize();
        // An old author date must not exclude a commit reachable after the tag.
        git("commit", "--allow-empty", "--date=2000-01-01T00:00:00Z", "-m", "Fix [parser] #7 <input>");
        String sha = git("rev-parse", "HEAD");
        git("commit", "--allow-empty", "-m", "Improve validation (#42)");
        String output = String.join("\n", command("1.0.0").genChangelog());
        assertTrue(output.contains("https://github.com/example/demo/tree/1.1.0"));
        assertTrue(output.contains("Fix \\[parser\\] #7 &lt;input&gt;"));
        assertTrue(output.contains("https://github.com/example/demo/commit/" + sha));
        assertTrue(output.contains("Improve validation (#42)"));
        assertTrue(output.contains("(by Test)"));
        assertFalse(output.contains("Already released"));
        assertFalse(output.contains("Merged pull requests"));
    }

    @Test
    void excludesMergesAndRecognizedAutomationButKeepsBranchChanges() throws Exception {
        initialize();
        git("checkout", "-b", "feature");
        git("commit", "--allow-empty", "-m", "Feature change");
        git("checkout", "main");
        git("merge", "--no-ff", "feature", "-m", "Merge feature");
        for (String subject : Arrays.asList("light-bot checkin CHANGELOG.md", "bot checkin",
                "[maven-release-plugin] prepare release 1.1.0",
                "[maven-release-plugin] prepare for next development iteration")) {
            git("commit", "--allow-empty", "-m", subject);
        }
        git("commit", "--allow-empty", "-m", "Fix release configuration");
        String output = String.join("\n", command("1.0.0").genChangelog());
        assertTrue(output.contains("Feature change"));
        assertTrue(output.contains("Fix release configuration"));
        assertFalse(output.contains("Merge feature"));
        assertFalse(output.contains("checkin"));
        assertFalse(output.contains("maven-release-plugin"));
    }

    @Test
    void createsMissingFileAndRegeneratesOnlySection() throws Exception {
        initialize();
        git("commit", "--allow-empty", "-m", "Direct change");
        ChangeLogCmd cmd = command("1.0.0");
        assertEquals(0, cmd.execute());
        String first = Files.readString(repository.resolve("CHANGELOG.md"));
        assertEquals(0, cmd.execute());
        assertEquals(first, Files.readString(repository.resolve("CHANGELOG.md")));
    }

    @Test
    void preservesPreambleAndOlderReleaseWhenRegenerating() throws Exception {
        initialize();
        git("commit", "--allow-empty", "-m", "Direct change");
        String preamble = "# Changelog\n\nProject release history.\n\n";
        String older = "## [1.0.0](https://github.com/example/demo/tree/1.0.0) (2026-01-01)\n\n- Old entry\n";
        Files.writeString(repository.resolve("CHANGELOG.md"), preamble
                + "## [1.1.0](https://github.com/example/demo/tree/1.1.0) (2026-01-02)\n\n- Stale entry\n\n" + older);
        command("1.0.0").execute();
        String output = Files.readString(repository.resolve("CHANGELOG.md"));
        assertTrue(output.startsWith(preamble));
        assertTrue(output.endsWith(older));
        assertFalse(output.contains("Stale entry"));
        assertTrue(output.contains("Direct change"));
        command("1.0.0").execute();
        assertEquals(output, Files.readString(repository.resolve("CHANGELOG.md")));
    }

    @Test
    void failsWithoutChangingFileForMissingInvalidOrUnrelatedTags() throws Exception {
        initialize();
        Path file = repository.resolve("CHANGELOG.md");
        Files.writeString(file, "Retained history\n");
        for (String tag : Arrays.asList("missing", "", "main", "1.0.0; touch unwanted")) {
            assertThrows(IOException.class, () -> command(tag).execute());
            assertEquals("Retained history\n", Files.readString(file));
        }
        assertFalse(Files.exists(repository.resolve("unwanted")));
        git("checkout", "--orphan", "unrelated");
        git("commit", "--allow-empty", "-m", "Unrelated history");
        git("tag", "unrelated-tag");
        git("checkout", "main");
        assertThrows(IOException.class, () -> command("unrelated-tag").execute());
        assertEquals("Retained history\n", Files.readString(file));
    }

    @Test
    void supportsEmptyRange() throws Exception {
        initialize();
        assertTrue(command("1.0.0").genChangelog().stream().noneMatch(line -> line.startsWith("- ")));
    }
    @Test
    void replacesHandwrittenUnreleasedAndDuplicateVersionHeaders() throws Exception {
        initialize();
        git("commit", "--allow-empty", "-m", "Final change");
        String old = "## [1.0.0] - Released\n\n- Retained\n";
        Files.writeString(repository.resolve("CHANGELOG.md"), "# Changelog\n\n"
                + "## [1.1.0] - Unreleased\n\n- Draft\n\n"
                + "## [1.1.0](https://github.com/example/demo/tree/1.1.0) (2026-01-01)\n\n- Duplicate\n"
                + old);
        command("1.0.0").execute();
        String output = Files.readString(repository.resolve("CHANGELOG.md"));
        assertEquals(1, output.split("## \\[1.1.0\\]", -1).length - 1);
        assertFalse(output.contains("Draft"));
        assertFalse(output.contains("Duplicate"));
        assertTrue(output.endsWith(old));
    }

    @Test
    void retainsReferencesAndNeutralizesMentionsAndFormatting() throws Exception {
        initialize();
        git("commit", "--allow-empty", "--author=Contributor @someone <author@example.com>",
                "-m", "Fixes #2804 (#2807), @someone and ~~strike~~");
        String output = String.join("\n", command("1.0.0").genChangelog());
        assertTrue(output.contains("Fixes #2804 (#2807)"));
        assertFalse(output.contains("@someone"));
        assertTrue(output.contains("@\u200Bsomeone"));
        assertTrue(output.contains("\\~\\~strike\\~\\~"));
        assertTrue(output.contains("(by Contributor @\u200Bsomeone)"));
    }

    @Test
    void reportsGitDiagnosticsAndNonAncestorReason() throws Exception {
        initialize();
        IOException missing = assertThrows(IOException.class, () -> command("missing").execute());
        assertTrue(missing.getMessage().contains("refs/tags/missing"));
        assertTrue(missing.getMessage().contains("fatal:"));
        git("checkout", "--orphan", "other");
        git("commit", "--allow-empty", "-m", "Other root");
        git("tag", "other");
        git("checkout", "main");
        IOException unrelated = assertThrows(IOException.class, () -> command("other").execute());
        assertTrue(unrelated.getMessage().contains("prev_tag 'other' is not an ancestor of HEAD"));
    }

    @Test
    void handlesSignatureConfigAndUsesConsistentBlankLines() throws Exception {
        initialize();
        git("config", "log.showSignature", "true");
        // A signed commit object needs no real signing key: verification must not run at all.
        String parent = git("rev-parse", "HEAD");
        String tree = git("rev-parse", "HEAD^{tree}");
        Path verifier = repository.resolve("signature-verifier");
        Files.writeString(verifier, "#!/bin/sh\ntouch '" + repository.resolve("verification-ran") + "'\nexit 1\n");
        assertTrue(verifier.toFile().setExecutable(true));
        git("config", "gpg.program", verifier.toString());
        Path commit = repository.resolve("signed-commit");
        Files.writeString(commit, "tree " + tree + "\nparent " + parent
                + "\nauthor Test <test@example.com> 1700000000 +0000"
                + "\ncommitter Test <test@example.com> 1700000000 +0000"
                + "\ngpgsig -----BEGIN PGP SIGNATURE-----\n \n dGVzdA==\n -----END PGP SIGNATURE-----"
                + "\n\nSigned change\n");
        String sha = git("hash-object", "-t", "commit", "-w", commit.toString());
        git("update-ref", "HEAD", sha);
        List<String> lines = command("1.0.0").genChangelog();
        assertEquals("", lines.get(1));
        assertTrue(lines.stream().noneMatch(line -> line.contains("\n") || line.contains("\r")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("Signed change ([")));
        assertFalse(Files.exists(repository.resolve("verification-ran")));
    }

    @Test
    void isolatedExecutorIgnoresHooksAndRejectsNonGitCommands() throws Exception {
        initialize();
        Path hooks = repository.resolve("hostile-hooks");
        Files.createDirectories(hooks);
        Path hook = hooks.resolve("pre-commit");
        Files.writeString(hook, "#!/bin/sh\nexit 99\n");
        assertTrue(hook.toFile().setExecutable(true));
        git("config", "core.hooksPath", hooks.toString());
        git("commit", "--allow-empty", "-m", "Hook-independent change");
        assertTrue(git("config", "--global", "--list").isEmpty());
        assertTrue(command("1.0.0").genChangelog().stream().anyMatch(line -> line.contains("Hook-independent")));
        assertThrows(AssertionError.class, () -> executor().execute(List.of("curl", "https://example.com"), repository.toFile()));
    }
}
