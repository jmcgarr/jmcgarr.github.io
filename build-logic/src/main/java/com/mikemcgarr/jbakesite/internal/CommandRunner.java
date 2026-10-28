package com.mikemcgarr.jbakesite.internal;

import java.util.List;

/**
 * Runs an external command ({@code git}, {@code gh}) for the writing tools, in the project directory. The
 * single-file programs each had their own {@code run} helper around {@link ProcessBuilder}; this is the one the
 * plugin's tools share (T127), and the seam a test replaces with a fake. {@link ProcessRunner} is the real one.
 */
public interface CommandRunner {

    /** What a command did: its exit code and its captured, trimmed output. */
    record Result(int exit, String output) {
    }

    /** Where a command's standard error goes. */
    enum Stderr {
        /** Captured with the standard output, in one stream, as {@code 2>&1}. */
        MERGE,
        /** Thrown away; only the standard output is captured. */
        DISCARD,
        /** Shown to the user, after the command finishes; only the standard output is captured. */
        SHOW
    }

    /**
     * Runs a command with no standard input and captures its standard output.
     *
     * @param command the program and its arguments
     * @param stderr where the standard error goes
     * @return the exit code and the captured output, or null if the program couldn't be started (not installed, not
     *         on the PATH)
     * @throws InterruptedException if interrupted while waiting for the command
     */
    Result run(List<String> command, Stderr stderr) throws InterruptedException;

    /**
     * Runs a command with its output shown to the user, as a step the user would have typed.
     *
     * @param command the program and its arguments
     * @return whether it exited with 0; false too when it couldn't be started, after saying so
     * @throws InterruptedException if interrupted while waiting for the command
     */
    boolean runVisibly(List<String> command) throws InterruptedException;
}
