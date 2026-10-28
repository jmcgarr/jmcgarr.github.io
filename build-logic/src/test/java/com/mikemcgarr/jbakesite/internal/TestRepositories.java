package com.mikemcgarr.jbakesite.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Git repositories for tests: a fresh one on {@code main} with a known author, and commits of everything. */
public final class TestRepositories {

    private TestRepositories() {
    }

    /**
     * Makes a repository on a {@code main} branch, with a committer configured and no hooks or signing.
     *
     * @param dir the directory
     * @throws IOException if git can't run
     * @throws InterruptedException if interrupted
     */
    public static void init(Path dir) throws IOException, InterruptedException {
        git(dir, "init", "--quiet", "--initial-branch=main");
        git(dir, "config", "user.name", "Test");
        git(dir, "config", "user.email", "test@example.test");
        git(dir, "config", "commit.gpgsign", "false");
    }

    /**
     * Stages everything and commits.
     *
     * @param dir the repository
     * @param message the commit message
     * @throws IOException if git can't run
     * @throws InterruptedException if interrupted
     */
    public static void commitAll(Path dir, String message) throws IOException, InterruptedException {
        git(dir, "add", "-A");
        git(dir, "commit", "--quiet", "-m", message);
    }

    /**
     * Runs git in a repository and returns its output, failing the test if git does.
     *
     * @param dir the repository
     * @param args git's arguments
     * @return git's trimmed output
     * @throws IOException if git can't run
     * @throws InterruptedException if interrupted
     */
    public static String git(Path dir, String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of("git"));
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command).directory(dir.toFile()).redirectErrorStream(true)
                .redirectInput(Redirect.PIPE).start();
        process.getOutputStream().close();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
        assertEquals(0, process.waitFor(), "git " + String.join(" ", args) + ": " + output);
        return output;
    }
}
