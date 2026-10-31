package com.mikemcgarr.jbakesite.writing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mikemcgarr.jbakesite.internal.CommandRunner;
import com.mikemcgarr.jbakesite.internal.FakeRunner;
import com.mikemcgarr.jbakesite.internal.Git;
import com.mikemcgarr.jbakesite.internal.ProcessRunner;
import com.mikemcgarr.jbakesite.internal.SiteLayout;
import com.mikemcgarr.jbakesite.internal.TestRepositories;
import com.mikemcgarr.jbakesite.internal.ToolFailure;
import com.mikemcgarr.jbakesite.review.ReviewOutcome;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Every branch of the openPr flow (docs/05-REVIEW-FLOW.md, T121) against a scripted git and gh: the refusals, the
 * uncommitted work with and without a message, the review stopping the push, the push and the pull request with
 * their exact arguments, a missing gh, the dry run, the title and the issue lines; and, on a real working tree, the
 * listing (with a non-ASCII name and a space) and a commit that leaves the author's other staged changes alone.
 */
class OpenPrTest {

    private static final String SUMMARY = "Reviewed 1 page (about.html) from 1 changed file. 0 errors, 1 warning. External links not checked (T119).";
    private static final OpenPr.Reviewer PASSING = base -> new ReviewOutcome(true, SUMMARY);
    /** A `git status --porcelain=v2 -z` entry for a file modified in the working tree, up to the path; entries end with NUL. */
    private static final String V2_MODIFIED = "1 .M N... 100644 100644 100644 1111111111111111111111111111111111111111 1111111111111111111111111111111111111111 ";
    /** The same for a staged rename, up to "new NUL old". */
    private static final String V2_RENAMED = "2 R. N... 100644 100644 100644 2222222222222222222222222222222222222222 3333333333333333333333333333333333333333 R100 ";
    /** The same for a file with merge conflicts, up to the path. */
    private static final String V2_UNMERGED = "u UU N... 100644 100644 100644 100644 1111111111111111111111111111111111111111 2222222222222222222222222222222222222222 3333333333333333333333333333333333333333 ";
    private static final OpenPr.Reviewer FAILING = base -> new ReviewOutcome(false,
            "Reviewed 1 page (about.html) from 1 changed file. 1 error, 0 warnings. External links not checked (T119).");

    @TempDir
    Path root;

    /** A bare repository that stands in for GitHub's, for the real push. */
    @TempDir
    Path origin;

    private SiteLayout site;
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private PrintStream printer;
    private final List<String> reviewedAgainst = new ArrayList<>();

    @BeforeEach
    void aPrinter() {
        site = SiteLayout.standard(root);
        printer = new PrintStream(out, true, StandardCharsets.UTF_8);
    }

    /** A branch `fix/thing` with one commit on top of main, a clean tree and origin set; git only, gh "not installed". */
    private FakeRunner gitOnly() {
        return new FakeRunner()
                .answer("git rev-parse --is-inside-work-tree", 0, "true")
                .answer("git branch --show-current", 0, "fix/thing")
                .answer("git rev-parse --verify --quiet refs/heads/main", 0, "abc123")
                .answer("git status --porcelain=v2 -z --untracked-files=all", 0, "")
                .answer("git remote get-url origin", 0, "git@github.com:owner/site.git")
                .answer("git rev-list --count refs/remotes/origin/main..refs/heads/main", 0, "0")
                .answer("git var GIT_COMMITTER_IDENT", 0, "Test <test@example.test> 1 +0000")
                .answer("git merge-base main HEAD", 0, "base1")
                .answer("git log --reverse --format=%s base1..HEAD", 0, "Fix the thing")
                .answer("git log --format=%B base1..HEAD", 0, "Fix the thing\n\nWhy.\n\nCloses #3\n")
                .answer("git add", 0, "")
                .answer("git commit", 0, "")
                .answer("git push --set-upstream origin fix/thing", 0, "branch 'fix/thing' set up to track 'origin/fix/thing'.");
    }

    /** The same branch with gh installed and logged in. */
    private FakeRunner readyBranch() {
        return gitOnly()
                .answer("gh --version", 0, "gh version 2.80.0")
                .answer("gh auth status", 0, "Logged in")
                .answer("gh repo view", 0, "owner/site")
                .answer("gh pr create", 0, "Creating pull request for fix/thing into main in owner/site\nhttps://github.com/owner/site/pull/42");
    }

    private OpenPr openPr(FakeRunner runner, OpenPr.Reviewer reviewer, List<String> trailers) {
        OpenPr.Reviewer recording = base -> {
            reviewedAgainst.add(base);
            return reviewer.review(base);
        };
        return new OpenPr(site, new Git(runner), runner, recording, trailers, printer);
    }

    @Test
    void refusesOnMainAndMasterNamingRuleTwo() {
        for (String branch : List.of("main", "master")) {
            FakeRunner runner = readyBranch().answer("git branch --show-current", 0, branch);
            ToolFailure e = assertThrows(ToolFailure.class, () -> openPr(runner, PASSING, List.of()).open(OpenPrOptions.NONE));
            assertEquals(ToolFailure.FAILED, e.exitCode());
            assertEquals("You're on " + branch + ": every change goes on its own branch off main, never on " + branch
                    + " itself (AGENTS.md, Rule 2). Run git switch -c <type>/<short-description>, then ./gradlew openPr.", e.getMessage());
            assertFalse(runner.calls().stream().anyMatch(c -> c.startsWith("git push") || c.startsWith("gh pr create")), runner.calls().toString());
        }
    }

    @Test
    void cannotRunOutsideAGitCheckoutOrWithoutMainAndRefusesADetachedHead() {
        FakeRunner noGit = new FakeRunner().answer("git rev-parse --is-inside-work-tree", 128, "");
        ToolFailure e = assertThrows(ToolFailure.class, () -> openPr(noGit, PASSING, List.of()).open(OpenPrOptions.NONE));
        assertEquals(ToolFailure.COULD_NOT_RUN, e.exitCode());
        assertEquals("Not a git checkout: openPr pushes the current branch and opens a pull request for it.", e.getMessage());

        FakeRunner detached = readyBranch().answer("git branch --show-current", 0, "");
        e = assertThrows(ToolFailure.class, () -> openPr(detached, PASSING, List.of()).open(OpenPrOptions.NONE));
        assertEquals("openPr needs a branch, and HEAD is detached. Switch to the branch of your change (git switch <branch>) first.", e.getMessage());

        FakeRunner noMain = readyBranch().answer("git rev-parse --verify --quiet refs/heads/main", 1, "");
        e = assertThrows(ToolFailure.class, () -> openPr(noMain, PASSING, List.of()).open(OpenPrOptions.NONE));
        assertEquals(ToolFailure.COULD_NOT_RUN, e.exitCode());
        assertEquals("There's no local main branch to open the pull request against.", e.getMessage());
    }

    @Test
    void listsTheUncommittedWorkAndStopsWithoutAMessageBeforeAskingGh() {
        FakeRunner runner = readyBranch().answer("git status --porcelain=v2 -z --untracked-files=all", 0,
                V2_MODIFIED + "src/jbake/content/about.md\0? docs/notes.md\0? scratch.txt\0" + V2_RENAMED + "README-old.md\0README.md\0"
                + V2_MODIFIED + "build.gradle\0");

        ToolFailure e = assertThrows(ToolFailure.class, () -> openPr(runner, PASSING, List.of()).open(OpenPrOptions.NONE));

        assertEquals("Nothing was committed or pushed. Re-run with -Pmessage=\"<what and why>\" to commit the files listed above with"
                + " that message, or commit them yourself first.", e.getMessage());
        assertEquals("Would commit:\n   M src/jbake/content/about.md\n  ?? docs/notes.md\n   M build.gradle\n"
                + "Not included (outside the paths openPr commits; left as they are):\n  ?? scratch.txt\n  R  README.md -> README-old.md\n",
                printed(), "a rename to a name outside the paths isn't committed");
        assertFalse(runner.calls().stream().anyMatch(c -> c.startsWith("gh ") || c.startsWith("git add") || c.startsWith("git commit")),
                "stopped before gh and before any change: " + runner.calls());
    }

    @Test
    void withAMessageItCommitsExactlyTheListedFilesWithTheTrailersThenGoesOn() throws IOException, InterruptedException {
        FakeRunner runner = readyBranch().answer("git status --porcelain=v2 -z --untracked-files=all", 0,
                V2_MODIFIED + "src/jbake/content/about.md\0? src/jbake/assets/img/new.jpg\0? scratch.txt\0" + V2_RENAMED + "docs/b.md\0docs/a.md\0");
        List<String> trailers = List.of("Co-Authored-By: Pair <pair@example.test>", "Session: https://example.test/s/1");

        String url = openPr(runner, PASSING, trailers).open(new OpenPrOptions(null, "Edit the About page", null, null, false));

        assertEquals("https://github.com/owner/site/pull/42", url);
        assertTrue(runner.calls().contains("git add -- src/jbake/content/about.md src/jbake/assets/img/new.jpg docs/b.md"),
                "exactly the listed files, not -A; a rename's old path is gone from the index already: " + runner.calls());
        assertTrue(runner.calls().contains("git commit --quiet --only -m Edit the About page -m Co-Authored-By: Pair <pair@example.test>\nSession: https://example.test/s/1 -- src/jbake/content/about.md src/jbake/assets/img/new.jpg docs/a.md docs/b.md"),
                "both sides of the rename, and nothing the author staged outside the paths: " + runner.calls());
        assertTrue(printed().contains("Committed 3 files on fix/thing: Edit the About page\n"), printed());
        int commit = runner.calls().indexOf("git commit --quiet --only -m Edit the About page -m Co-Authored-By: Pair <pair@example.test>\nSession: https://example.test/s/1 -- src/jbake/content/about.md src/jbake/assets/img/new.jpg docs/a.md docs/b.md");
        int committer = runner.calls().indexOf("git var GIT_COMMITTER_IDENT");
        int auth = runner.calls().indexOf("gh auth status");
        assertTrue(auth < commit && committer < commit, "readiness and the committer are checked before committing: " + runner.calls());
    }

    @Test
    void withoutTrailersTheCommitHasTheMessageAlone() throws IOException, InterruptedException {
        FakeRunner runner = readyBranch().answer("git status --porcelain=v2 -z --untracked-files=all", 0, V2_MODIFIED + "AGENTS.md\0");

        openPr(runner, PASSING, List.of()).open(new OpenPrOptions(null, "Clarify Rule 2", null, null, false));

        assertTrue(runner.calls().contains("git commit --quiet --only -m Clarify Rule 2 -- AGENTS.md"), runner.calls().toString());
        assertTrue(printed().contains("Committed 1 file on fix/thing: Clarify Rule 2\n"), printed());
    }

    @Test
    void aMergeInProgressIsRefusedBeforeAnythingIsStagedOrAsked() {
        FakeRunner runner = readyBranch().answer("git status --porcelain=v2 -z --untracked-files=all", 0,
                V2_MODIFIED + "README.md\0" + V2_UNMERGED + "src/jbake/content/about.md\0");

        ToolFailure e = assertThrows(ToolFailure.class,
                () -> openPr(runner, PASSING, List.of()).open(new OpenPrOptions(null, "Merge main", null, null, false)));

        assertEquals(ToolFailure.FAILED, e.exitCode());
        assertEquals("Resolve the merge conflicts first (git status shows them), then run ./gradlew openPr.", e.getMessage());
        assertFalse(runner.calls().stream().anyMatch(c -> c.startsWith("gh ") || c.startsWith("git add") || c.startsWith("git commit")),
                "a conflicted file is never committed with its markers: " + runner.calls());
        assertEquals("", printed(), "no listing either");
    }

    @Test
    void aFailedStageOrCommitSaysWhatStateTheFilesAreIn() {
        FakeRunner addFails = readyBranch().answer("git status --porcelain=v2 -z --untracked-files=all", 0, V2_MODIFIED + "AGENTS.md\0")
                .answer("git add", 128, "fatal: index locked");
        ToolFailure e = assertThrows(ToolFailure.class,
                () -> openPr(addFails, PASSING, List.of()).open(new OpenPrOptions(null, "Clarify", null, null, false)));
        assertEquals("Staging the files failed (see the git output above). Nothing was committed or pushed.", e.getMessage());
        assertFalse(addFails.calls().stream().anyMatch(c -> c.startsWith("git commit")), addFails.calls().toString());

        FakeRunner commitFails = readyBranch().answer("git status --porcelain=v2 -z --untracked-files=all", 0, V2_MODIFIED + "AGENTS.md\0")
                .answer("git commit", 1, "hook declined");
        e = assertThrows(ToolFailure.class,
                () -> openPr(commitFails, PASSING, List.of()).open(new OpenPrOptions(null, "Clarify", null, null, false)));
        assertEquals("Committing failed (see the git output above). Nothing was pushed; the listed files are staged.", e.getMessage());
        assertFalse(commitFails.calls().stream().anyMatch(c -> c.startsWith("git push")), commitFails.calls().toString());
    }

    @Test
    void reviewErrorsStopItBeforeThePush() {
        FakeRunner runner = readyBranch();

        ToolFailure e = assertThrows(ToolFailure.class, () -> openPr(runner, FAILING, List.of()).open(OpenPrOptions.NONE));

        assertEquals(ToolFailure.FAILED, e.exitCode());
        assertEquals("The review found errors (listed above), so nothing was pushed. Fix them, or suppress one with"
                + " <!-- review: ignore E2, why --> on the line before it, then run ./gradlew openPr again.", e.getMessage());
        assertEquals(List.of("main"), reviewedAgainst, "the same scope as reviewChanges: the merge base with main");
        assertFalse(runner.calls().stream().anyMatch(c -> c.startsWith("git push") || c.startsWith("gh pr create")), runner.calls().toString());
    }

    @Test
    void onSuccessItPushesWithUpstreamAndOpensAReadyPullRequestAgainstMain() throws IOException, InterruptedException {
        FakeRunner runner = readyBranch();

        String url = openPr(runner, PASSING, List.of()).open(OpenPrOptions.NONE);

        assertEquals("https://github.com/owner/site/pull/42", url);
        assertTrue(runner.calls().contains("git push --set-upstream origin fix/thing"), runner.calls().toString());
        String create = runner.calls().stream().filter(c -> c.startsWith("gh pr create")).findFirst().orElseThrow();
        assertTrue(create.startsWith("gh pr create --repo owner/site --base main --head fix/thing --title Fix the thing --body-file "), create);
        assertFalse(create.contains("--draft"), "ready, not draft: " + create);
        assertTrue(printed().contains("Pushed fix/thing to origin\nOpened the pull request \"Fix the thing\": https://github.com/owner/site/pull/42\n"), printed());
        assertTrue(runner.shown().contains("set up to track"), "git's output is shown to the author");
        assertTrue(runner.calls().indexOf("git push --set-upstream origin fix/thing") < runner.calls().indexOf(create), "push, then the PR");
        assertFalse(Files.exists(Path.of(create.substring(create.indexOf("--body-file ") + "--body-file ".length()))), "the body file is cleaned up");
    }

    @Test
    void theTitleIsTheOptionOrTheFirstCommitSubject() throws IOException, InterruptedException {
        FakeRunner given = readyBranch();
        openPr(given, PASSING, List.of()).open(new OpenPrOptions("  A better title ", null, null, null, false));
        assertTrue(given.calls().stream().anyMatch(c -> c.contains("--title A better title --body-file")), given.calls().toString());

        FakeRunner several = readyBranch().answer("git log --reverse --format=%s base1..HEAD", 0, "First commit\nSecond commit\nThird");
        openPr(several, PASSING, List.of()).open(OpenPrOptions.NONE);
        assertTrue(several.calls().stream().anyMatch(c -> c.contains("--title First commit --body-file")), several.calls().toString());
    }

    @Test
    void withNoCommitsBeyondMainThereIsNothingToOpen() {
        FakeRunner runner = readyBranch().answer("git log --reverse --format=%s base1..HEAD", 0, "");

        ToolFailure e = assertThrows(ToolFailure.class, () -> openPr(runner, PASSING, List.of()).open(OpenPrOptions.NONE));

        assertEquals("fix/thing has no commits that main doesn't have, so there is nothing to open a pull request for.", e.getMessage());
        assertEquals(List.of(), reviewedAgainst, "no review of nothing");
    }

    @Test
    void aMissingOrLoggedOutGhStopsItWithAClearMessage() {
        FakeRunner noGh = gitOnly();
        ToolFailure e = assertThrows(ToolFailure.class, () -> openPr(noGh, PASSING, List.of()).open(OpenPrOptions.NONE));
        assertEquals("openPr needs the GitHub CLI (gh), and it isn't installed or isn't on the PATH. Install it (https://cli.github.com),"
                + " run gh auth login, then try again.", e.getMessage());
        assertFalse(noGh.calls().stream().anyMatch(c -> c.startsWith("git push")), noGh.calls().toString());

        FakeRunner loggedOut = readyBranch().answer("gh auth status", 1, "You are not logged into any GitHub hosts.");
        e = assertThrows(ToolFailure.class, () -> openPr(loggedOut, PASSING, List.of()).open(OpenPrOptions.NONE));
        assertEquals("openPr needs the GitHub CLI to be logged in, and gh auth status says it isn't:\n  You are not logged into any GitHub hosts.\n"
                + "Run gh auth login, then try again.", e.getMessage());

        FakeRunner noOrigin = readyBranch().answer("git remote get-url origin", 2, "");
        e = assertThrows(ToolFailure.class, () -> openPr(noOrigin, PASSING, List.of()).open(OpenPrOptions.NONE));
        assertEquals("openPr pushes the branch to origin, and this repository has no origin remote.", e.getMessage());

        FakeRunner unpushedMain = readyBranch().answer("git rev-list --count refs/remotes/origin/main..refs/heads/main", 0, "2");
        e = assertThrows(ToolFailure.class, () -> openPr(unpushedMain, PASSING, List.of()).open(OpenPrOptions.NONE));
        assertEquals("Your local main has 2 commit(s) that aren't on origin/main. They would end up in the pull request. Push or reset main first.",
                e.getMessage());
    }

    @Test
    void aFailedPushOrPrSaysWhatIsInPlaceAndWhatToRunNext() {
        FakeRunner pushFails = readyBranch().answer("git push --set-upstream origin fix/thing", 1, "fatal: could not read from remote repository");
        ToolFailure e = assertThrows(ToolFailure.class, () -> openPr(pushFails, PASSING, List.of()).open(OpenPrOptions.NONE));
        assertTrue(e.getMessage().startsWith("Pushing fix/thing to origin failed (see the git output above). Nothing was undone. To finish, run:\n"
                + "  git push --set-upstream origin fix/thing\n"
                + "  gh pr create --repo owner/site --base main --head fix/thing --title 'Fix the thing' --body-file "), e.getMessage());

        FakeRunner prFails = readyBranch().answer("gh pr create", 1, "GraphQL: something");
        e = assertThrows(ToolFailure.class, () -> openPr(prFails, PASSING, List.of()).open(OpenPrOptions.NONE));
        assertTrue(e.getMessage().startsWith("fix/thing is pushed, but opening the pull request failed (see the gh output above). Nothing was undone."
                + " To finish, run:\n  gh pr create --repo owner/site --base main --head fix/thing --title 'Fix the thing' --body-file "), e.getMessage());
    }

    @Test
    void aDryRunPrintsThePullRequestAndChangesNothing() throws IOException, InterruptedException {
        FakeRunner runner = readyBranch().answer("git status --porcelain=v2 -z --untracked-files=all", 0, V2_MODIFIED + "src/jbake/content/about.md\0")
                .answer("git log --reverse --format=%s base1..HEAD", 0, "");

        String url = openPr(runner, PASSING, List.of()).open(new OpenPrOptions(null, "Edit About", "16", "14", true));

        assertNull(url);
        assertFalse(runner.calls().stream().anyMatch(c -> c.startsWith("git add") || c.startsWith("git commit") || c.startsWith("git push")
                || c.startsWith("gh pr create")), runner.calls().toString());
        String printed = printed();
        assertTrue(printed.contains("Would commit:\n   M src/jbake/content/about.md\nDry run: not committing.\n"), printed);
        assertTrue(printed.contains("Dry run: nothing was committed, pushed or opened. The pull request would be:\n  Title: Edit About\n  Base:  main  Head: fix/thing\n"
                + "  ## What changed\n  \n  - Edit About\n"), "the commit the real run would make is the title: " + printed);
        assertTrue(printed.contains("  Closes #16\n  Refs #14\n  Closes #3\n"), "the options' lines, then the commits': " + printed);
    }

    @Test
    void issueLinesComeFromTheOptionsAndTheCommitMessagesOnceEach() {
        assertEquals(List.of("Closes #3", "Closes #16", "Refs #14", "Fixes #7", "Refs #9"),
                OpenPr.issueLines("3, #16", "14", "Subject\n\nfixes #7\nCloses #3\n  Refs #9  \nrefs: #10\nCloses #11 and #12\n"));
        assertEquals(List.of(), OpenPr.issueLines(null, null, "Subject\n"));
        assertEquals(List.of(), OpenPr.issueLines("", " , ", ""));

        ToolFailure e = assertThrows(ToolFailure.class, () -> OpenPr.issueLines("three", null, ""));
        assertEquals(ToolFailure.COULD_NOT_RUN, e.exitCode());
        assertEquals("-Pcloses takes issue numbers, comma-separated (-Pcloses=3,16), not 'three'.", e.getMessage());
    }

    @Test
    void theDescriptionHasTheCommitsTheReviewAndTheIssueLines() {
        assertEquals("## What changed\n\n- First\n- Second\n\n## Review\n\n" + SUMMARY + "\n\n"
                + "`./gradlew openPr` ran `reviewChanges` against `main` before pushing; CI runs the same review on this pull request.\n"
                + "\nCloses #3\n",
                OpenPr.description(List.of("First", "Second"), SUMMARY, List.of("Closes #3")));
        assertFalse(OpenPr.description(List.of("Only"), SUMMARY, List.of()).contains("Closes"));
    }

    @Test
    void onlyTheRepositorysSourcesAreCommitted() {
        for (String path : List.of("src/jbake/content/about.md", "docs/05-REVIEW-FLOW.md", "scripts/smoke-test.sh", "build-logic/build.gradle",
                ".github/workflows/gradle.yml", "README.md", "AGENTS.md", "CLAUDE.md", "build.gradle", "settings.gradle")) {
            assertTrue(OpenPr.isCommitted(path), path);
        }
        for (String path : List.of("scratch.txt", "build/jbake/index.html", "README-old.md", "gradle/wrapper/gradle-wrapper.properties", ".gitignore")) {
            assertFalse(OpenPr.isCommitted(path), path);
        }
    }

    @Test
    void readsARealWorkingTreeAsGitPrintsIt() throws IOException, InterruptedException {
        Files.createDirectories(site.content());
        Files.writeString(site.content().resolve("about.md"), "title=About\n~~~~~~\n");
        Files.writeString(root.resolve("README.md"), "# Site\n");
        TestRepositories.init(root);
        TestRepositories.commitAll(root, "Site");
        TestRepositories.git(root, "switch", "--quiet", "--create", "docs/readme");
        Files.writeString(root.resolve("README.md"), "# Site\n\nMore.\n");
        Files.writeString(site.content().resolve("new.md"), "title=New\n~~~~~~\n");
        Files.writeString(site.content().resolve("caf\u00e9.md"), "title=Caf\u00e9\n~~~~~~\n");
        Files.writeString(site.content().resolve("with space.md"), "title=Space\n~~~~~~\n");
        Files.writeString(root.resolve("scratch.txt"), "x\n");
        ProcessRunner runner = new ProcessRunner(root, printer, printer);
        OpenPr tool = new OpenPr(site, new Git(runner), runner, PASSING, List.of(), printer);

        ToolFailure e = assertThrows(ToolFailure.class, () -> tool.open(OpenPrOptions.NONE));

        assertTrue(e.getMessage().startsWith("Nothing was committed or pushed."), e.getMessage());
        assertEquals("Would commit:\n   M README.md\n  ?? src/jbake/content/caf\u00e9.md\n  ?? src/jbake/content/new.md\n  ?? src/jbake/content/with space.md\n"
                + "Not included (outside the paths openPr commits; left as they are):\n  ?? scratch.txt\n",
                printed(), "paths with a non-ASCII letter or a space are read as they are, not C-quoted");
        assertEquals("Site", TestRepositories.git(root, "log", "-1", "--format=%s"), "nothing committed");
    }

    @Test
    void onARealTreeItCommitsOnlyTheListedPathsAndLeavesOtherStagedChangesInTheIndex() throws IOException, InterruptedException {
        Files.createDirectories(site.content());
        Files.createDirectories(root.resolve("docs"));
        Files.writeString(site.content().resolve("about.md"), "title=About\n~~~~~~\n");
        Files.writeString(root.resolve("docs/a.md"), "# A\n");
        Files.writeString(root.resolve("README.md"), "# Site\n");
        TestRepositories.init(root);
        TestRepositories.commitAll(root, "Site");
        TestRepositories.git(origin, "init", "--quiet", "--bare", "--initial-branch=main");
        TestRepositories.git(root, "remote", "add", "origin", origin.toString());
        TestRepositories.git(root, "push", "--quiet", "origin", "main");
        TestRepositories.git(root, "switch", "--quiet", "--create", "post/cafe");
        Files.writeString(site.content().resolve("about.md"), "title=About\n~~~~~~\nEdited.\n");
        Files.writeString(site.content().resolve("caf\u00e9.md"), "title=Caf\u00e9\n~~~~~~\n");
        Files.writeString(site.content().resolve("with space.md"), "title=Space\n~~~~~~\n");
        TestRepositories.git(root, "mv", "docs/a.md", "docs/b.md");
        Files.writeString(root.resolve("scratch.txt"), "x\n");
        TestRepositories.git(root, "add", "scratch.txt");   // staged by the author, outside the committed paths
        ProcessRunner git = new ProcessRunner(root, printer, printer);
        FakeRunner gh = new FakeRunner()
                .answer("gh --version", 0, "gh version 2.80.0")
                .answer("gh auth status", 0, "Logged in")
                .answer("gh repo view", 0, "owner/site")
                .answer("gh pr create", 0, "https://github.com/owner/site/pull/43");
        CommandRunner runner = new CommandRunner() {
            @Override
            public CommandRunner.Result run(List<String> command, Stderr stderr) throws IOException, InterruptedException {
                return command.get(0).equals("gh") ? gh.run(command, stderr) : git.run(command, stderr);
            }

            @Override
            public boolean runVisibly(List<String> command) throws InterruptedException {
                return command.get(0).equals("gh") ? gh.runVisibly(command) : git.runVisibly(command);
            }
        };
        OpenPr tool = new OpenPr(site, new Git(runner), runner, PASSING, List.of("Co-Authored-By: Pair <pair@example.test>"), printer);

        String url = tool.open(new OpenPrOptions(null, "Add the caf\u00e9 page", null, null, false));

        assertEquals("https://github.com/owner/site/pull/43", url);
        assertTrue(printed().contains("Committed 4 files on post/cafe: Add the caf\u00e9 page\n"), printed());
        assertEquals("Add the caf\u00e9 page\n\nCo-Authored-By: Pair <pair@example.test>", TestRepositories.git(root, "log", "-1", "--format=%B"));
        assertEquals("docs/a.md\ndocs/b.md\nsrc/jbake/content/about.md\nsrc/jbake/content/caf\u00e9.md\nsrc/jbake/content/with space.md",
                TestRepositories.git(root, "-c", "core.quotePath=false", "show", "--name-only", "--no-renames", "--format=", "HEAD"),
                "both sides of the rename, the edit and both special names, nothing else");
        assertEquals("A  scratch.txt", TestRepositories.git(root, "status", "--porcelain"), "the author's staged file is still staged, not committed");
        assertEquals("origin/post/cafe", TestRepositories.git(root, "rev-parse", "--abbrev-ref", "post/cafe@{upstream}"), "pushed with upstream");
    }

    private String printed() {
        return out.toString(StandardCharsets.UTF_8);
    }
}
