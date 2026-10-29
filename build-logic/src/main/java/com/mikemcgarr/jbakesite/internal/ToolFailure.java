package com.mikemcgarr.jbakesite.internal;

/**
 * What a writing or verification tool says when it stops: the message the single-file programs under
 * {@code scripts/} printed before {@code System.exit} (T127 moved them into the plugin), with the exit code they
 * used. A task turns it into a build failure with the same message; a test reads it directly. Exit 1 means the
 * tool ran and found something wrong (the review's errors, a check's problems, a refused option); exit 2 means it
 * couldn't run at all (no baked site, not a git checkout).
 */
public final class ToolFailure extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** The tool ran and found a problem, or refused what it was asked. */
    public static final int FAILED = 1;

    /** The tool couldn't run: a usage or environment problem. */
    public static final int COULD_NOT_RUN = 2;

    private final int exitCode;

    /**
     * Creates a failure with the exit code the program used.
     *
     * @param exitCode {@link #FAILED} or {@link #COULD_NOT_RUN}
     * @param message the message, as the program printed it
     */
    public ToolFailure(int exitCode, String message) {
        super(message);
        this.exitCode = exitCode;
    }

    /**
     * A failure with exit code 1: the tool found a problem or refused what it was asked.
     *
     * @param message the message
     * @return the failure, to throw
     */
    public static ToolFailure failed(String message) {
        return new ToolFailure(FAILED, message);
    }

    /**
     * A failure with exit code 2: the tool couldn't run.
     *
     * @param message the message
     * @return the failure, to throw
     */
    public static ToolFailure couldNotRun(String message) {
        return new ToolFailure(COULD_NOT_RUN, message);
    }

    /**
     * The exit code the program used.
     *
     * @return 1 or 2
     */
    public int exitCode() {
        return exitCode;
    }
}
