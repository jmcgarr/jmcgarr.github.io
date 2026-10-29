package com.mikemcgarr.jbakesite.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * The {@link CommandRunner} that starts real processes, in a fixed working directory, with their standard input
 * closed (the null device) and {@code GH_PROMPT_DISABLED} set so the GitHub CLI never waits for an answer. Output
 * meant for the user is captured and printed when the command finishes: a task runs inside the Gradle daemon, whose
 * own standard output isn't the user's terminal, so {@code inheritIO} (what {@code NewPost.java} used) would lose it.
 */
public final class ProcessRunner implements CommandRunner {

    private final Path workingDirectory;
    private final PrintStream out;
    private final PrintStream err;

    /**
     * Creates a runner.
     *
     * @param workingDirectory where commands run, usually the project directory
     * @param out where a visibly run command's output goes
     * @param err where "couldn't run" goes
     */
    public ProcessRunner(Path workingDirectory, PrintStream out, PrintStream err) {
        this.workingDirectory = workingDirectory;
        this.out = out;
        this.err = err;
    }

    @Override
    public Result run(List<String> command, Stderr stderr) throws InterruptedException {
        ProcessBuilder builder = builder(command);
        switch (stderr) {
            case MERGE -> builder.redirectErrorStream(true);
            case DISCARD -> builder.redirectError(Redirect.DISCARD);
            case SHOW -> builder.redirectError(Redirect.PIPE);
        }
        try {
            Process process = builder.start();
            // Both pipes are drained at once: a child that fills one while this end waits on the other would hang.
            Future<byte[]> errorBytes = stderr == Stderr.SHOW
                    ? drain(process.getErrorStream())
                    : CompletableFuture.completedFuture(new byte[0]);
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
            int exit = process.waitFor();
            byte[] error = errorBytes.get();
            if (error.length > 0) {
                out.print(new String(error, StandardCharsets.UTF_8));
                out.flush();
            }
            return new Result(exit, output);
        } catch (IOException e) {
            return null;
        } catch (ExecutionException e) {
            return null;   // the stderr pipe broke: the same as not being able to run the command
        }
    }

    @Override
    public boolean runVisibly(List<String> command) throws InterruptedException {
        try {
            Process process = builder(command).redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exit = process.waitFor();
            if (!output.isEmpty()) {
                out.print(output);
                out.flush();
            }
            return exit == 0;
        } catch (IOException e) {
            err.println("couldn't run " + command.get(0) + ": " + e.getMessage());
            return false;
        }
    }

    /** Reads a stream to its end on a thread of its own. */
    private static Future<byte[]> drain(InputStream stream) {
        FutureTask<byte[]> task = new FutureTask<>(stream::readAllBytes);
        Thread thread = new Thread(task, "process-stderr");
        thread.setDaemon(true);
        thread.start();
        return task;
    }

    private ProcessBuilder builder(List<String> command) {
        ProcessBuilder builder = new ProcessBuilder(command)
                .directory(workingDirectory.toFile())
                .redirectInput(Redirect.from(nullDevice()));
        builder.environment().put("GH_PROMPT_DISABLED", "1");
        return builder;
    }

    private static File nullDevice() {
        return new File(System.getProperty("os.name").startsWith("Windows") ? "NUL" : "/dev/null");
    }
}
