package com.mikemcgarr.jbakesite.writing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mikemcgarr.jbakesite.internal.CommandRunner;
import com.mikemcgarr.jbakesite.internal.FakeRunner;
import com.mikemcgarr.jbakesite.internal.Git;
import com.mikemcgarr.jbakesite.internal.ProcessRunner;
import com.mikemcgarr.jbakesite.internal.SiteLayout;
import com.mikemcgarr.jbakesite.internal.TestRepositories;
import com.mikemcgarr.jbakesite.internal.ToolFailure;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The draft, its branch and the refusals, in a real temporary repository; the -Ppr flow against a scripted gh. */
class NewPostTest {

    private static final String HOST = "https://www.example.test";
    private static final Clock TODAY = Clock.fixed(Instant.parse("2026-10-28T12:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path root;

    private SiteLayout site;
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private PrintStream printer;

    @BeforeEach
    void aRepositoryWithTheSiteLayout() throws IOException, InterruptedException {
        root = root.toRealPath();
        site = SiteLayout.standard(root);
        printer = new PrintStream(out, true, StandardCharsets.UTF_8);
        Files.createDirectories(site.drafts());
        Files.createDirectories(site.publishedUrls().getParent());
        Files.writeString(site.blog().resolve("alm.md"), "title=ALM\ndate=2010-01-01\ntype=post\ntags=DevOps, java\nstatus=published\n~~~~~~\n");
        Files.writeString(site.publishedUrls(), "index.html\nblog/alm.html\nblog/old-post.html\n");
        TestRepositories.init(root);
        TestRepositories.commitAll(root, "Site");
    }

    @Test
    void slugifiesATitle() {
        assertEquals("boyds-law-iteration-speed", NewPost.slugify("Boyd's Law: Iteration & Speed!"));
        assertEquals("cafe-au-lait", NewPost.slugify("Café au lait"));
        assertEquals("m10-test", NewPost.slugify("  M10 Test  "));
        assertEquals("its-a-test", NewPost.slugify("It’s a test"));
    }

    @Test
    void createsTheDraftWithTheHeaderOnANewBranchFromMain() throws IOException, InterruptedException {
        Path file = newPost().create(new NewPostOptions("M10 Test", "DevOps, management", "A teaser.", null, null, null, null));

        assertEquals(site.drafts().resolve("m10-test.md"), file);
        assertEquals("title=M10 Test\ndate=2026-10-28\ntype=post\ntags=DevOps, management\nstatus=draft\nsummary=A teaser.\n~~~~~~\n\n",
                Files.readString(file));
        assertEquals("post/m10-test", TestRepositories.git(root, "branch", "--show-current"));
        String printed = out.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("\nCreated src/jbake/content/blog/drafts/m10-test.md (status=draft, never published)\n"
                + "Switched to the new branch post/m10-test (from main)\n"), printed);
        assertTrue(printed.contains("    http://localhost:8080/blog/drafts/m10-test-draft.html\n"), printed);
        assertTrue(printed.contains("  To publish: set status=published, move the file to src/jbake/content/blog/, and open a PR.\n"
                + "    It will live at " + HOST + "/blog/m10-test.html (permanent).\n"), printed);
        assertFalse(printed.contains("Fill in summary="), "a summary was given");
        assertTrue(printed.contains("Note: 'management' is a new tag, so publishing creates /tags/management.html."), printed);
        assertFalse(printed.contains("WARNING"), "DevOps is spelled as the existing post spells it");
    }

    @Test
    void warnsAboutATagSpelledUnlikeAnExistingOneAndAsksForASummary() throws IOException, InterruptedException {
        newPost().create(new NewPostOptions("Second", "devops", null, null, "false", null, null));

        String printed = out.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("WARNING: the tag 'devops' is already used as [DevOps]. A different capitalization makes a separate"
                + " tag page. Use the existing spelling."), printed);
        assertTrue(printed.contains("  Fill in summary= (optional): it's the teaser under the title on the home page.\n"), printed);
        assertEquals("main", TestRepositories.git(root, "branch", "--show-current"), "-Pbranch=false stays put");
        assertFalse(printed.contains("Switched to"), printed);
    }

    @Test
    void refusesWithoutATitle() {
        ToolFailure e = assertThrows(ToolFailure.class, () -> newPost().create(NewPostOptions.withTitle("  ")));

        assertEquals(ToolFailure.FAILED, e.exitCode());
        assertEquals("A title is required: ./gradlew newPost -Ptitle=\"My Post Title\"", e.getMessage());
    }

    @Test
    void refusesTheRetiredFormatOptionABadSlugAndPrWithoutABranch() {
        assertEquals("-Pformat is gone: every post is Markdown (T102, 2026-10-18), so the draft is always <slug>.md.",
                failure(new NewPostOptions("T", null, null, null, null, null, "asciidoc")));
        assertEquals("The slug 'My Slug' isn't usable. Use lowercase letters, digits, and single hyphens, for example -Pslug=my-post.",
                failure(new NewPostOptions("T", null, null, "My Slug", null, null, null)));
        assertEquals("-Ppr takes no value (or true/false), not 'maybe'.",
                failure(new NewPostOptions("T", null, null, null, null, "maybe", null)));
        assertEquals("-Ppr opens a pull request from the post/<slug> branch, so it can't be combined with -Pbranch=false.",
                failure(new NewPostOptions("T", null, null, null, "false", "", null)));
    }

    @Test
    void refusesASlugThatExistsAsAPostADraftOrAPublishedUrl() throws IOException {
        assertEquals("src/jbake/content/blog/alm.md already exists. Pick another title or pass -Pslug=<other-slug>.",
                failure(NewPostOptions.withTitle("ALM")));
        Files.writeString(site.drafts().resolve("wip.html"), "");
        assertEquals("src/jbake/content/blog/drafts/wip.html already exists. Pick another title or pass -Pslug=<other-slug>.",
                failure(NewPostOptions.withTitle("WIP")));
        assertEquals("/blog/old-post.html was already published (docs/baseline/urls.txt). Published URLs are permanent, so pick"
                + " another title or pass -Pslug=<other-slug>.", failure(NewPostOptions.withTitle("Old Post")));
    }

    @Test
    void refusesToBranchWithUncommittedChangesOrAnExistingBranch() throws IOException, InterruptedException {
        Files.writeString(site.blog().resolve("alm.md"), "changed");
        assertEquals("You have uncommitted changes. Commit or stash them first, so they don't end up on post/t (or pass"
                + " -Pbranch=false to stay on the current branch).", failure(NewPostOptions.withTitle("T")));

        TestRepositories.commitAll(root, "Edit");
        TestRepositories.git(root, "branch", "post/t");
        assertEquals("The branch post/t already exists.", failure(NewPostOptions.withTitle("T")));
    }

    @Test
    void withPrItCommitsPushesAndOpensADraftPullRequestThroughGh() throws IOException, InterruptedException {
        FakeRunner gh = new FakeRunner()
                .answer("gh --version", 0, "gh version 2.80.0")
                .answer("gh auth status", 0, "Logged in")
                .answer("gh repo view", 0, "owner/site")
                .answer("gh pr create", 0, "Creating pull request\nhttps://github.com/owner/site/pull/7")
                .answer("git rev-parse --verify --quiet refs/heads/post/", 1, "")
                .answer("git rev-parse --verify --quiet refs/remotes/origin/post/", 1, "")
                .answer("git rev-parse --verify --quiet refs/heads/main", 0, "abc123")
                .answer("git status --porcelain", 0, "")
                .answer("git remote get-url origin", 0, "git@github.com:owner/site.git")
                .answer("git var GIT_COMMITTER_IDENT", 0, "Test <test@example.test> 1 +0000")
                .answer("git rev-list --count", 0, "0")
                .answer("git switch", 0, "")
                .answer("git add", 0, "")
                .answer("git commit", 0, "")
                .answer("git push", 0, "Branch 'post/pr-post' set up to track 'origin/post/pr-post'.\n");

        new NewPost(site, HOST, new Git(gh), gh, TODAY, printer).create(new NewPostOptions("PR Post", null, null, null, null, "", null));

        String printed = out.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("Committed src/jbake/content/blog/drafts/pr-post.md on post/pr-post\n"
                + "Pushed post/pr-post to origin\n"
                + "Opened the draft pull request \"[WIP] PR Post\": https://github.com/owner/site/pull/7\n"), printed);
        assertTrue(printed.contains("  Commit and push to post/pr-post as you go; the draft PR picks it up.\n"
                + "  To publish: set status=published, move the file to src/jbake/content/blog/, drop [WIP] from\n"
                + "    the PR title, mark it ready (gh pr ready), and merge.\n"), printed);
        assertTrue(gh.calls().contains("git add -- src/jbake/content/blog/drafts/pr-post.md"), gh.calls().toString());
        assertTrue(gh.calls().stream().anyMatch(c -> c.startsWith("gh pr create --draft --repo owner/site --base main --head post/pr-post"
                + " --title [WIP] PR Post --body-file ")), gh.calls().toString());
        assertTrue(gh.shown().contains("set up to track"), "git's output is shown to the author");
    }

    @Test
    void withPrItStopsBeforeCreatingAnythingWhenGhIsMissingOrLoggedOut() {
        FakeRunner noGh = new FakeRunner();
        NewPost tool = new NewPost(site, HOST, new Git(noGh), noGh, TODAY, printer);
        ToolFailure e = assertThrows(ToolFailure.class, () -> tool.create(new NewPostOptions("T", null, null, null, null, "true", null)));
        assertEquals("-Ppr needs the GitHub CLI (gh), and it isn't installed or isn't on the PATH. Install it (https://cli.github.com),"
                + " run gh auth login, then try again.", e.getMessage());
        assertFalse(Files.exists(site.drafts().resolve("t.md")));

        FakeRunner loggedOut = new FakeRunner().answer("gh --version", 0, "gh").answer("gh auth status", 1, "You are not logged in");
        NewPost tool2 = new NewPost(site, HOST, new Git(loggedOut), loggedOut, TODAY, printer);
        e = assertThrows(ToolFailure.class, () -> tool2.create(new NewPostOptions("T", null, null, null, null, "true", null)));
        assertEquals("-Ppr needs the GitHub CLI to be logged in, and gh auth status says it isn't:\n  You are not logged in\n"
                + "Run gh auth login, then try again.", e.getMessage());
    }

    @Test
    void aFailedPushSaysWhatToRunNext() {
        FakeRunner gh = new FakeRunner()
                .answer("gh --version", 0, "gh").answer("gh auth status", 0, "ok").answer("gh repo view", 0, "owner/site")
                .answer("git rev-parse --verify --quiet refs/heads/post/", 1, "")
                .answer("git rev-parse --verify --quiet refs/remotes/origin/post/", 1, "")
                .answer("git rev-parse --verify --quiet refs/heads/main", 0, "abc")
                .answer("git status --porcelain", 0, "").answer("git remote get-url origin", 0, "url")
                .answer("git var GIT_COMMITTER_IDENT", 0, "x").answer("git rev-list --count", 0, "0")
                .answer("git switch", 0, "").answer("git add", 0, "").answer("git commit", 0, "")
                .answer("git push", 1, "fatal: could not read from remote repository");
        NewPost tool = new NewPost(site, HOST, new Git(gh), gh, TODAY, printer);

        ToolFailure e = assertThrows(ToolFailure.class, () -> tool.create(new NewPostOptions("Push Me", null, null, null, null, "", null)));

        assertTrue(e.getMessage().startsWith("The draft is committed on post/push-me, but pushing it to origin failed (see the git output"
                + " above). Nothing was undone. To finish, run:\n  git push --set-upstream origin post/push-me\n"
                + "  gh pr create --draft --repo owner/site --base main --head post/push-me --title '[WIP] Push Me' --body-file "),
                e.getMessage());
    }

    private NewPost newPost() {
        CommandRunner runner = new ProcessRunner(root, printer, printer);
        return new NewPost(site, HOST, new Git(runner), runner, TODAY, printer);
    }

    private String failure(NewPostOptions options) {
        return assertThrows(ToolFailure.class, () -> newPost().create(options)).getMessage();
    }
}
