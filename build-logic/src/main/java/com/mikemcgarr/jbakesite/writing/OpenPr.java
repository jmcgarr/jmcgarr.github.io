package com.mikemcgarr.jbakesite.writing;

import com.mikemcgarr.jbakesite.internal.CommandRunner;
import com.mikemcgarr.jbakesite.internal.CommandRunner.Result;
import com.mikemcgarr.jbakesite.internal.CommandRunner.Stderr;
import com.mikemcgarr.jbakesite.internal.Git;
import com.mikemcgarr.jbakesite.internal.SiteLayout;
import com.mikemcgarr.jbakesite.internal.ToolFailure;
import com.mikemcgarr.jbakesite.review.ReviewOutcome;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Opens a pull request for the current branch once the review passes (task T121 of docs/05-REVIEW-FLOW.md, "The
 * three entry points"). In order: it refuses to run on {@code main} or {@code master} (AGENTS.md Rule 2); it lists
 * the uncommitted work it would commit (changed and untracked files under the repository's sources,
 * {@link #COMMITTED_PATHS}; anything else is listed as not included and left alone) and stops there unless
 * {@code -Pmessage} is given, in which case it commits exactly those files with that message and the site's
 * {@code commitTrailers}; it runs the review of the changes since the merge base with {@code main} and stops before
 * the push on errors; then it pushes the branch to origin and opens a ready (not draft) pull request against
 * {@code main} with the GitHub CLI: the title from {@code -Ptitle} or the branch's first commit subject, a description
 * with the commits, the review's summary line and the {@code Closes #N}/{@code Refs #N} lines from {@code -Pcloses},
 * {@code -Prefs} and the commit messages. {@code -PdryRun} does everything but commit, push and open.
 * {@link OpenPrTask} runs it.
 */
final class OpenPr {

    /**
     * Where uncommitted work is committed from: the design's list (docs/05, the owner's decision of 2026-10-21) plus
     * the build's own sources since M10 ({@code build-logic/}, {@code settings.gradle}, {@code CLAUDE.md}). A
     * directory ends with {@code /}.
     */
    static final List<String> COMMITTED_PATHS = List.of("src/", "docs/", "scripts/", "build-logic/", ".github/",
            "README.md", "AGENTS.md", "CLAUDE.md", "build.gradle", "settings.gradle");

    /** The branches a change never goes on directly (Rule 2). */
    static final List<String> PROTECTED_BRANCHES = List.of("main", "master");

    /** The branch pull requests are opened against, and the review diffs against. */
    static final String BASE = "main";

    private static final Pattern ISSUE_LINE = Pattern.compile("(?i)^\\s*(closes|fixes|resolves|refs)\\s+#(\\d+)\\s*$");

    /** Runs the review of the changes since the merge base with a branch; {@code ReviewRunner::run}, or a test's fake. */
    @FunctionalInterface
    interface Reviewer {

        /**
         * Reviews and prints the report.
         *
         * @param base the branch to diff against
         * @return whether the review passed, and its summary line
         * @throws IOException if a file can't be read
         * @throws InterruptedException if interrupted while asking git
         * @throws ToolFailure when the review can't run
         */
        ReviewOutcome review(String base) throws IOException, InterruptedException;
    }

    private final SiteLayout site;
    private final Git git;
    private final CommandRunner runner;
    private final Reviewer reviewer;
    private final List<String> commitTrailers;
    private final PrintStream out;

    /**
     * Creates the tool.
     *
     * @param site the repository, for the paths in messages
     * @param git the repository
     * @param runner how git and gh are run
     * @param reviewer the review
     * @param commitTrailers the lines appended to a commit made from uncommitted work, after a blank line; may be empty
     * @param out where the tool talks to the author
     */
    OpenPr(SiteLayout site, Git git, CommandRunner runner, Reviewer reviewer, List<String> commitTrailers, PrintStream out) {
        this.site = site;
        this.git = git;
        this.runner = runner;
        this.reviewer = reviewer;
        this.commitTrailers = commitTrailers;
        this.out = out;
    }

    /**
     * Opens the pull request.
     *
     * @param options what the author asked for
     * @return the pull request's URL, or null for a dry run
     * @throws IOException if git isn't installed or the description can't be written
     * @throws InterruptedException if interrupted while waiting for git or gh
     * @throws ToolFailure when it refuses or stops, with the reason: on a protected branch, with uncommitted work
     *         and no message, when the review finds errors, when a step fails
     */
    String open(OpenPrOptions options) throws IOException, InterruptedException {
        if (git.output("rev-parse", "--is-inside-work-tree").isEmpty()) {
            throw ToolFailure.couldNotRun("Not a git checkout: openPr pushes the current branch and opens a pull request for it.");
        }
        String branch = git.output("branch", "--show-current");
        if (branch.isEmpty()) {
            throw fail("openPr needs a branch, and HEAD is detached. Switch to the branch of your change (git switch <branch>) first.");
        }
        if (PROTECTED_BRANCHES.contains(branch)) {
            throw fail("You're on " + branch + ": every change goes on its own branch off main, never on " + branch
                    + " itself (AGENTS.md, Rule 2). Run git switch -c <type>/<short-description>, then ./gradlew openPr.");
        }
        if (git.output("rev-parse", "--verify", "--quiet", "refs/heads/" + BASE).isEmpty()) {
            throw ToolFailure.couldNotRun("There's no local " + BASE + " branch to open the pull request against.");
        }

        WorkingTree tree = workingTree();
        String message = options.message() == null ? "" : options.message().strip();
        if (!tree.included().isEmpty()) {
            out.println("Would commit:");
            tree.included().forEach(line -> out.println("  " + line));
            tree.printNotIncluded(out);
            if (message.isEmpty()) {
                throw fail("Nothing was committed or pushed. Re-run with -Pmessage=\"<what and why>\" to commit the files listed "
                        + "above with that message, or commit them yourself first.");
            }
        } else {
            tree.printNotIncluded(out);
            if (!message.isEmpty()) {
                out.println("Nothing to commit: the working tree is clean, so -Pmessage isn't used.");
            }
        }

        GitHubCli gh = new GitHubCli(git, runner, "openPr");
        String repo = gh.repository("the branch");
        gh.mainIsPushed("the pull request");

        if (!tree.included().isEmpty()) {
            if (options.dryRun()) {
                out.println("Dry run: not committing.");
            } else {
                gh.committerKnown("your changes");
                commit(tree, message, branch);
            }
        }

        String mergeBase = git.output("merge-base", BASE, "HEAD");
        if (mergeBase.isEmpty()) {
            throw ToolFailure.couldNotRun("Can't find where " + branch + " left " + BASE + " (git merge-base " + BASE + " HEAD).");
        }
        List<String> subjects = lines(git.output("log", "--reverse", "--format=%s", mergeBase + "..HEAD"));
        if (subjects.isEmpty() && options.dryRun() && !message.isEmpty()) {
            subjects = List.of(message);   // the commit the real run would make
        }
        if (subjects.isEmpty()) {
            throw fail(branch + " has no commits that " + BASE + " doesn't have, so there is nothing to open a pull request for.");
        }

        ReviewOutcome review = reviewer.review(BASE);
        if (!review.passed()) {
            throw fail("The review found errors (listed above), so nothing was pushed. Fix them, or suppress one with "
                    + "<!-- review: ignore E2, why --> on the line before it, then run ./gradlew openPr again.");
        }

        String title = options.title() == null || options.title().isBlank() ? subjects.get(0) : options.title().strip();
        List<String> issueLines = issueLines(options.closes(), options.refs(), git.output("log", "--format=%B", mergeBase + "..HEAD"));
        String body = description(subjects, review.summary(), issueLines);

        if (options.dryRun()) {
            out.println("Dry run: nothing was committed, pushed or opened. The pull request would be:");
            out.println("  Title: " + title);
            out.println("  Base:  " + BASE + "  Head: " + branch);
            out.println(GitHubCli.indent(body));
            return null;
        }
        return pushAndOpen(branch, repo, title, body);
    }

    /**
     * Stages exactly the listed files and commits them, and only them, with the message and the trailers. The
     * {@code git add} makes untracked files known to git (the old path of a rename isn't added: it is gone from the
     * index already, and git refuses a pathspec that matches nothing); {@code git commit --only -- <paths>} then commits
     * those paths alone, both sides of a rename, so a change the author had staged outside {@link #COMMITTED_PATHS}
     * (listed as not included) stays in the index instead of riding along (review of 2026-10-28).
     */
    private void commit(WorkingTree tree, String message, String branch) throws InterruptedException {
        List<String> add = new ArrayList<>(List.of("git", "add", "--"));
        add.addAll(tree.staged());
        List<String> commit = new ArrayList<>(List.of("git", "commit", "--quiet", "--only", "-m", message));
        if (!commitTrailers.isEmpty()) {
            commit.add("-m");
            commit.add(String.join("\n", commitTrailers));
        }
        commit.add("--");
        commit.addAll(tree.paths());
        if (!runner.runVisibly(add)) {
            throw fail("Staging the files failed (see the git output above). Nothing was committed or pushed.");
        }
        if (!runner.runVisibly(commit)) {
            throw fail("Committing failed (see the git output above). Nothing was pushed; the listed files are staged.");
        }
        int files = tree.included().size();
        out.println("Committed " + files + (files == 1 ? " file" : " files") + " on " + branch + ": " + message);
    }

    /** Pushes the branch and opens the pull request; a failed step says what is in place and what to run next. */
    private String pushAndOpen(String branch, String repo, String title, String body) throws IOException, InterruptedException {
        Path bodyFile = Files.createTempFile("openpr-body-", ".md");
        Files.writeString(bodyFile, body, StandardCharsets.UTF_8);
        List<String> push = List.of("git", "push", "--set-upstream", "origin", branch);
        List<String> create = List.of("gh", "pr", "create", "--repo", repo, "--base", BASE, "--head", branch,
                "--title", title, "--body-file", bodyFile.toString());
        boolean keepBody = false;
        try {
            if (!runner.runVisibly(push)) {
                keepBody = true;
                throw GitHubCli.stopAfter("Pushing " + branch + " to origin failed (see the git output above).", List.of(push, create));
            }
            out.println("Pushed " + branch + " to origin");
            Result created = runner.run(create, Stderr.SHOW);
            if (created == null || created.exit() != 0) {
                keepBody = true;
                throw GitHubCli.stopAfter(branch + " is pushed, but opening the pull request failed"
                        + (created == null ? " (gh didn't run)." : " (see the gh output above)."), List.of(create));
            }
            String url = created.output().lines().filter(l -> l.startsWith("https://")).reduce((a, b) -> b).orElse(created.output());
            out.println("Opened the pull request \"" + title + "\": " + url);
            return url;
        } finally {
            if (!keepBody) {
                Files.deleteIfExists(bodyFile);   // kept otherwise: the message says to run gh pr create --body-file <it>
            }
        }
    }

    /**
     * The uncommitted work, from {@code git status}, split into what is committed from and what isn't. The porcelain
     * v2 format is read because its entries never start with a space ({@code 1 }, {@code 2 }, {@code ? }), which the
     * trimmed command output would lose, and with {@code -z} (NUL between entries, and between a rename's new and old
     * path) because without it git C-quotes a path with a space, a quote or a non-ASCII letter
     * ({@code "src/jbake/content/caf\303\251.md"}), which would be misread and then handed to {@code git add} as the
     * quoted literal (review of 2026-10-28). Each entry is shown the way the short format reads it ({@code  M path},
     * {@code ?? path}, {@code R  old -> new}).
     *
     * @throws ToolFailure when a merge is in progress: an unmerged file must be resolved by the author, not committed
     *         with its conflict markers
     */
    private WorkingTree workingTree() throws IOException, InterruptedException {
        List<String> included = new ArrayList<>();
        List<String> notIncluded = new ArrayList<>();
        List<String> paths = new ArrayList<>();
        List<String> staged = new ArrayList<>();
        for (StatusEntry entry : StatusEntry.parseAll(git.output("status", "--porcelain=v2", "-z", "--untracked-files=all"))) {
            if (entry.unmerged()) {
                throw fail("Resolve the merge conflicts first (git status shows them), then run ./gradlew openPr.");
            }
            if (isCommitted(entry.path())) {
                included.add(entry.shown());
                if (entry.original() != null) {
                    paths.add(entry.original());
                }
                paths.add(entry.path());
                staged.add(entry.path());
            } else {
                notIncluded.add(entry.shown());
            }
        }
        return new WorkingTree(included, notIncluded, paths, staged);
    }

    /**
     * Whether a repository-relative path is one {@code openPr} commits.
     *
     * @param path the path as git prints it
     * @return true under one of {@link #COMMITTED_PATHS}
     */
    static boolean isCommitted(String path) {
        for (String committed : COMMITTED_PATHS) {
            if (committed.endsWith("/") ? path.startsWith(committed) : path.equals(committed)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The {@code Closes #N} and {@code Refs #N} lines of the description: from the options first, then every such
     * line in the commit messages, each once (AGENTS.md Rule 5: one keyword per issue, on its own line).
     *
     * @param closes {@code -Pcloses}, comma-separated numbers, or null
     * @param refs {@code -Prefs}, comma-separated numbers, or null
     * @param commitMessages the commit messages, as {@code git log --format=%B} prints them
     * @return the lines, in order
     * @throws ToolFailure when an option isn't issue numbers
     */
    static List<String> issueLines(String closes, String refs, String commitMessages) {
        Set<String> lines = new LinkedHashSet<>();
        for (String number : numbers(closes, "closes")) {
            lines.add("Closes #" + number);
        }
        for (String number : numbers(refs, "refs")) {
            lines.add("Refs #" + number);
        }
        for (String line : commitMessages.split("\n")) {
            Matcher m = ISSUE_LINE.matcher(line);
            if (m.matches()) {
                String keyword = m.group(1).toLowerCase(Locale.ROOT);
                lines.add(Character.toUpperCase(keyword.charAt(0)) + keyword.substring(1) + " #" + m.group(2));
            }
        }
        return new ArrayList<>(lines);
    }

    private static List<String> numbers(String option, String name) {
        List<String> numbers = new ArrayList<>();
        if (option == null) {
            return numbers;
        }
        for (String part : option.split(",")) {
            String number = part.strip().replaceFirst("^#", "");
            if (number.isEmpty()) {
                continue;
            }
            if (!number.matches("\\d+")) {
                throw ToolFailure.couldNotRun("-P" + name + " takes issue numbers, comma-separated (-P" + name + "=3,16), not '" + part.strip() + "'.");
            }
            numbers.add(number);
        }
        return numbers;
    }

    /**
     * The pull request's description: the commits, the review's summary line, and the issue lines.
     *
     * @param subjects the commit subjects, oldest first
     * @param reviewSummary the review's summary line
     * @param issueLines the {@code Closes}/{@code Refs} lines, possibly none
     * @return the Markdown
     */
    static String description(List<String> subjects, String reviewSummary, List<String> issueLines) {
        StringBuilder sb = new StringBuilder("## What changed\n\n");
        for (String subject : subjects) {
            sb.append("- ").append(subject).append('\n');
        }
        sb.append("\n## Review\n\n").append(reviewSummary).append("\n\n")
                .append("`./gradlew openPr` ran `reviewChanges` against `").append(BASE)
                .append("` before pushing; CI runs the same review on this pull request.\n");
        if (!issueLines.isEmpty()) {
            sb.append('\n');
            issueLines.forEach(line -> sb.append(line).append('\n'));
        }
        return sb.toString();
    }

    private static List<String> lines(String output) {
        return output.isEmpty() ? List.of() : output.lines().filter(l -> !l.isBlank()).toList();
    }

    private static ToolFailure fail(String message) {
        return ToolFailure.failed(message);
    }

    /**
     * One entry of {@code git status --porcelain=v2 -z}: an ordinary change ({@code 1 XY ... path}), a rename or copy
     * ({@code 2 XY ... path NUL original}), an untracked file ({@code ? path}) or an unmerged file
     * ({@code u XY ... path}).
     *
     * @param status the two-letter index and working-tree status, {@code ??} for an untracked file
     * @param path the path, the new one for a rename
     * @param original the old path of a rename, or null
     * @param unmerged whether the file has merge conflicts (a {@code u} entry)
     */
    record StatusEntry(String status, String path, String original, boolean unmerged) {

        private static final int CHANGE_FIELDS = 8;     // "1 XY sub mH mI mW hH hI" then the path
        private static final int RENAME_FIELDS = 9;     // "2 XY sub mH mI mW hH hI Xscore" then the path, NUL, the original
        private static final int UNMERGED_FIELDS = 10;  // "u XY sub m1 m2 m3 mW h1 h2 h3" then the path

        /**
         * Parses the whole output.
         *
         * @param output the output of {@code git status --porcelain=v2 -z}, NUL-separated
         * @return the entries, in git's order; a token that isn't one (a header, or a kind not listed) is skipped
         */
        static List<StatusEntry> parseAll(String output) {
            List<StatusEntry> entries = new ArrayList<>();
            String[] tokens = output.split("\0");
            for (int i = 0; i < tokens.length; i++) {
                String token = tokens[i];
                if (token.isEmpty()) {
                    continue;
                }
                if (token.startsWith("? ")) {
                    entries.add(new StatusEntry("??", token.substring(2), null, false));
                    continue;
                }
                int fields = switch (token.charAt(0)) {
                    case '1' -> CHANGE_FIELDS;
                    case '2' -> RENAME_FIELDS;
                    case 'u' -> UNMERGED_FIELDS;
                    default -> 0;
                };
                if (fields == 0) {
                    continue;
                }
                String[] parts = token.split(" ", fields + 1);
                if (parts.length <= fields) {
                    continue;
                }
                String status = parts[1].replace('.', ' ');
                String path = parts[fields];
                if (fields == RENAME_FIELDS) {
                    String original = i + 1 < tokens.length ? tokens[++i] : "";
                    entries.add(new StatusEntry(status, path, original, false));
                } else {
                    entries.add(new StatusEntry(status, path, null, fields == UNMERGED_FIELDS));
                }
            }
            return entries;
        }

        /** The entry as {@code git status --short} shows it. */
        String shown() {
            return status + " " + (original == null ? path : original + " -> " + path);
        }
    }

    /**
     * The uncommitted work.
     *
     * @param included the {@code git status} lines of the files openPr commits
     * @param notIncluded the lines of the files it leaves alone
     * @param paths the paths to commit for the included files (both sides of a rename)
     * @param staged the paths to stage first: the same without the old side of a rename
     */
    record WorkingTree(List<String> included, List<String> notIncluded, List<String> paths, List<String> staged) {

        void printNotIncluded(PrintStream out) {
            if (notIncluded.isEmpty()) {
                return;
            }
            out.println("Not included (outside the paths openPr commits; left as they are):");
            notIncluded.forEach(line -> out.println("  " + line));
        }
    }
}
