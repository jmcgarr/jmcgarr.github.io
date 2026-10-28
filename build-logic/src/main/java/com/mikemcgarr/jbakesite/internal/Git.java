package com.mikemcgarr.jbakesite.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs {@code git} in the project directory for the writing tools: the {@code git} helper that {@code NewPost.java},
 * {@code ReviewChanges.java} and {@code PrepareImage.java} each carried, once (T127, the T130 decision). A command
 * that exits non-zero is not an error here (the tools ask git questions: "does this branch exist?") and yields an
 * empty answer; git being absent is.
 */
public final class Git {

    private static final String NOT_INSTALLED = "git isn't installed or isn't on the PATH.";

    private final CommandRunner runner;

    /**
     * Creates a git front end.
     *
     * @param runner how commands are run; its working directory is the repository
     */
    public Git(CommandRunner runner) {
        this.runner = runner;
    }

    /**
     * Runs git and returns its trimmed standard output, or "" if it exits non-zero.
     *
     * @param args git's arguments, such as {@code "rev-parse", "HEAD"}
     * @return the output, or ""
     * @throws IOException if git isn't installed
     * @throws InterruptedException if interrupted while waiting for git
     */
    public String output(String... args) throws IOException, InterruptedException {
        CommandRunner.Result result = runner.run(command(args), CommandRunner.Stderr.DISCARD);
        if (result == null) {
            throw new IOException(NOT_INSTALLED);
        }
        return result.exit() == 0 ? result.output() : "";
    }

    /**
     * Runs git for its exit code alone, as {@code git cat-file -e} is used.
     *
     * @param args git's arguments
     * @return whether git exited with 0
     * @throws IOException if git isn't installed
     * @throws InterruptedException if interrupted while waiting for git
     */
    public boolean succeeds(String... args) throws IOException, InterruptedException {
        CommandRunner.Result result = runner.run(command(args), CommandRunner.Stderr.DISCARD);
        if (result == null) {
            throw new IOException(NOT_INSTALLED);
        }
        return result.exit() == 0;
    }

    /**
     * Runs git with its output shown to the user, for a command that changes something ({@code git switch}).
     *
     * @param args git's arguments
     * @return whether git exited with 0
     * @throws InterruptedException if interrupted while waiting for git
     */
    public boolean runVisibly(String... args) throws InterruptedException {
        return runner.runVisibly(command(args));
    }

    private static List<String> command(String... args) {
        List<String> command = new ArrayList<>(args.length + 1);
        command.add("git");
        command.addAll(List.of(args));
        return command;
    }
}
