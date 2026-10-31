package com.mikemcgarr.jbakesite.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The git front end against a real repository, and the process runner it is built on. */
class GitTest {

    @TempDir
    Path repo;

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private ProcessRunner runner;
    private Git git;

    @BeforeEach
    void initARepository() throws IOException, InterruptedException {
        runner = new ProcessRunner(repo, new PrintStream(out, true, StandardCharsets.UTF_8), new PrintStream(out, true, StandardCharsets.UTF_8));
        git = new Git(runner);
        TestRepositories.init(repo);
        Files.writeString(repo.resolve("file.txt"), "hello\n");
        TestRepositories.commitAll(repo, "First");
    }

    @Test
    void outputIsGitsStandardOutputTrimmed() throws IOException, InterruptedException {
        assertEquals("main", git.output("branch", "--show-current"));
    }

    @Test
    void outputIsEmptyWhenGitExitsNonZero() throws IOException, InterruptedException {
        assertEquals("", git.output("rev-parse", "--verify", "--quiet", "refs/heads/no-such-branch"));
        assertEquals("", out.toString(), "and stderr is discarded");
    }

    @Test
    void succeedsIsTheExitCode() throws IOException, InterruptedException {
        assertTrue(git.succeeds("cat-file", "-e", "main:file.txt"));
        assertFalse(git.succeeds("cat-file", "-e", "main:missing.txt"));
    }

    @Test
    void runVisiblyShowsGitsOutput() throws InterruptedException {
        assertTrue(git.runVisibly("status", "--short", "--branch"));

        assertTrue(out.toString(StandardCharsets.UTF_8).contains("## main"), out.toString());
    }

    @Test
    void aMissingProgramIsAnIOExceptionFromTheRunnerAndGitSaysSoInItsOwnWords() throws InterruptedException {
        IOException notRun = assertThrows(IOException.class,
                () -> runner.run(List.of("no-such-program-xyz", "--version"), CommandRunner.Stderr.MERGE));
        assertTrue(notRun.getMessage().contains("no-such-program-xyz"), notRun.getMessage());
        assertFalse(runner.runVisibly(List.of("no-such-program-xyz")), "a visible run reports it and returns false instead");
        assertTrue(out.toString(StandardCharsets.UTF_8).startsWith("couldn't run no-such-program-xyz: "));

        IOException e = assertThrows(IOException.class, () -> new Git(new FakeRunner()).output("status"));
        assertEquals("git isn't installed or isn't on the PATH.", e.getMessage());
        assertTrue(e.getCause() instanceof IOException, "the runner's reason is kept as the cause");
    }

    @Test
    void stderrIsMergedDiscardedOrShownAsAsked() throws IOException, InterruptedException {
        List<String> failing = List.of("git", "rev-parse", "--verify", "refs/heads/no-such-branch");

        CommandRunner.Result merged = runner.run(failing, CommandRunner.Stderr.MERGE);
        assertTrue(merged.exit() != 0);
        assertTrue(merged.output().contains("fatal"), "git's error is in the captured output: " + merged.output());

        CommandRunner.Result discarded = runner.run(failing, CommandRunner.Stderr.DISCARD);
        assertEquals("", discarded.output());
        assertEquals("", out.toString());

        CommandRunner.Result shown = runner.run(failing, CommandRunner.Stderr.SHOW);
        assertEquals("", shown.output(), "only stdout is captured");
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("fatal"), "stderr went to the user");
    }
}
