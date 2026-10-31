package com.mikemcgarr.jbakesite.writing;

import com.mikemcgarr.jbakesite.internal.CommandRunner;
import com.mikemcgarr.jbakesite.internal.CommandRunner.Result;
import com.mikemcgarr.jbakesite.internal.CommandRunner.Stderr;
import com.mikemcgarr.jbakesite.internal.Git;
import com.mikemcgarr.jbakesite.internal.ToolFailure;
import java.io.IOException;
import java.util.List;

/**
 * What a tool checks before it pushes a branch and opens a pull request with the GitHub CLI, and how it explains a
 * step that failed: {@code newPost -Ppr}'s readiness checks (T061), shared with {@code openPr} (T121) so they exist
 * once. Every message starts with the name of the command that needs the check ({@code -Ppr}, {@code openPr}).
 */
final class GitHubCli {

    private final Git git;
    private final CommandRunner runner;
    private final String who;

    /**
     * Creates the checks for one command.
     *
     * @param git the repository
     * @param runner how gh is run
     * @param who the command the messages name, {@code -Ppr} or {@code openPr}
     */
    GitHubCli(Git git, CommandRunner runner, String who) {
        this.git = git;
        this.runner = runner;
        this.who = who;
    }

    /**
     * Checks that gh is installed and logged in, that there is an origin to push to and that gh can find the GitHub
     * repository.
     *
     * @param whatIsPushed what the command pushes, for the no-origin message: "the post branch", "the branch"
     * @return the repository as {@code owner/name}
     * @throws IOException if git isn't installed
     * @throws InterruptedException if interrupted while waiting for git or gh
     * @throws ToolFailure when a check fails, saying what to do
     */
    String repository(String whatIsPushed) throws IOException, InterruptedException {
        try {
            runner.run(List.of("gh", "--version"), Stderr.MERGE);
        } catch (IOException e) {
            throw ToolFailure.failed(who + " needs the GitHub CLI (gh), and it isn't installed or isn't on the PATH. "
                    + "Install it (https://cli.github.com), run gh auth login, then try again.");
        }
        Result auth = runner.run(List.of("gh", "auth", "status"), Stderr.MERGE);
        if (auth.exit() != 0) {
            throw ToolFailure.failed(who + " needs the GitHub CLI to be logged in, and gh auth status says it isn't:\n"
                    + indent(auth.output()) + "\nRun gh auth login, then try again.");
        }
        if (git.output("remote", "get-url", "origin").isEmpty()) {
            throw ToolFailure.failed(who + " pushes " + whatIsPushed + " to origin, and this repository has no origin remote.");
        }
        Result repo = runner.run(List.of("gh", "repo", "view", "--json", "nameWithOwner", "--jq", ".nameWithOwner"), Stderr.MERGE);
        if (repo.exit() != 0 || repo.output().isEmpty()) {
            throw ToolFailure.failed(who + " couldn't find the GitHub repository with gh repo view:\n" + indent(repo.output()));
        }
        return repo.output();
    }

    /**
     * Checks that git knows who is committing.
     *
     * @param what what the command commits, for the message: "the draft", "your changes"
     * @throws IOException if git isn't installed
     * @throws InterruptedException if interrupted while waiting for git
     * @throws ToolFailure when the committer isn't configured
     */
    void committerKnown(String what) throws IOException, InterruptedException {
        if (git.output("var", "GIT_COMMITTER_IDENT").isEmpty()) {
            throw ToolFailure.failed(who + " commits " + what + ", and git doesn't know your name and email. Set them with "
                    + "git config user.name and git config user.email, then try again.");
        }
    }

    /**
     * Checks that the local {@code main} has nothing unpushed, which would ride along in the pull request.
     *
     * @param whosePullRequest the pull request named in the message: "the post's pull request", "the pull request"
     * @throws IOException if git isn't installed
     * @throws InterruptedException if interrupted while waiting for git
     * @throws ToolFailure when main is ahead of origin/main
     */
    void mainIsPushed(String whosePullRequest) throws IOException, InterruptedException {
        String unpushed = git.output("rev-list", "--count", "refs/remotes/origin/main..refs/heads/main");
        if (!unpushed.isEmpty() && !unpushed.equals("0")) {
            throw ToolFailure.failed("Your local main has " + unpushed + " commit(s) that aren't on origin/main. They would end up "
                    + "in " + whosePullRequest + ". Push or reset main first.");
        }
    }

    /**
     * Explains a failed step and lists the commands that finish the job by hand. Nothing is undone.
     *
     * @param what what happened, and what is in place
     * @param remaining the commands left to run, each as its words
     * @return the failure, to throw
     */
    static ToolFailure stopAfter(String what, List<List<String>> remaining) {
        StringBuilder msg = new StringBuilder(what).append(" Nothing was undone. To finish, run:");
        for (List<String> cmd : remaining) {
            msg.append("\n  ").append(String.join(" ", cmd.stream().map(GitHubCli::shellQuote).toList()));
        }
        return ToolFailure.failed(msg.toString());
    }

    /**
     * A word as it is typed in a shell: quoted unless it is plain.
     *
     * @param s the word
     * @return the word, single-quoted when it needs it
     */
    static String shellQuote(String s) {
        return s.matches("[A-Za-z0-9_./:=@+,-]+") ? s : "'" + s.replace("'", "'\\''") + "'";
    }

    /**
     * A command's output as it reads inside a message: each line indented, or "(no output)".
     *
     * @param text the output
     * @return the indented text
     */
    static String indent(String text) {
        return text.lines().map(l -> "  " + l).reduce((a, b) -> a + "\n" + b).orElse("  (no output)");
    }
}
