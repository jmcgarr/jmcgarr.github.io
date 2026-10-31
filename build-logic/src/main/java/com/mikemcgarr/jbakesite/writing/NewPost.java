package com.mikemcgarr.jbakesite.writing;

import com.mikemcgarr.jbakesite.internal.CommandRunner;
import com.mikemcgarr.jbakesite.internal.CommandRunner.Result;
import com.mikemcgarr.jbakesite.internal.CommandRunner.Stderr;
import com.mikemcgarr.jbakesite.internal.Git;
import com.mikemcgarr.jbakesite.internal.SiteLayout;
import com.mikemcgarr.jbakesite.internal.Tags;
import com.mikemcgarr.jbakesite.internal.ToolFailure;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Starts a new draft post (revival tasks T060 and T061, issue #8; {@code scripts/NewPost.java} until T127). It
 * creates {@code content/blog/drafts/<slug>.md} (Markdown, the site's only format since T102) with the JBake header
 * filled in and {@code status=draft}, on a new branch {@code post/<slug>} made from {@code main}. Drafts render for
 * local preview only and are never published (T009). It refuses to reuse a slug that exists as a draft, a post, or
 * a published URL, because published URLs are permanent (AGENTS.md, Rule 1).
 *
 * <p>With {@code -Ppr} (T061) it also commits the draft, pushes {@code post/<slug>} to origin, and opens a draft
 * pull request titled "[WIP] &lt;title&gt;" with the GitHub CLI (gh). Pull requests never deploy (T008), so this
 * publishes nothing. A refusal is a {@link ToolFailure} with the program's message; {@link NewPostTask} runs it.
 */
final class NewPost {

    /** The file extensions JBake would still render, so a slug in either is taken. */
    static final List<String> EXTENSIONS = List.of("md", "html");

    private final SiteLayout site;
    private final String siteHost;
    private final Git git;
    private final CommandRunner runner;
    private final Clock clock;
    private final PrintStream out;

    /**
     * Creates the tool.
     *
     * @param site where the content is
     * @param siteHost the published address, for the "it will live at" line
     * @param git the repository the draft's branch is made in
     * @param runner how gh and git are run for {@code -Ppr}
     * @param clock today's date for {@code date=}
     * @param out where the tool talks to the author
     */
    NewPost(SiteLayout site, String siteHost, Git git, CommandRunner runner, Clock clock, PrintStream out) {
        this.site = site;
        this.siteHost = siteHost;
        this.git = git;
        this.runner = runner;
        this.clock = clock;
        this.out = out;
    }

    /**
     * Creates the draft, and the branch and pull request when asked.
     *
     * @param options what the author asked for
     * @return the draft file
     * @throws IOException if a file can't be written or git isn't installed
     * @throws InterruptedException if interrupted while waiting for git or gh
     * @throws ToolFailure when the options or the repository's state don't allow it, with the reason
     */
    Path create(NewPostOptions options) throws IOException, InterruptedException {
        String title = options.title() == null ? "" : options.title().strip();
        if (title.isEmpty()) {
            throw fail("A title is required: ./gradlew newPost -Ptitle=\"My Post Title\"");
        }
        if (options.format() != null) {
            throw fail("-Pformat is gone: every post is Markdown (T102, 2026-10-18), so the draft is always <slug>.md.");
        }
        String slug = options.slug() != null ? options.slug().strip() : slugify(title);
        if (!slug.matches("[a-z0-9]+(-[a-z0-9]+)*")) {
            throw fail("The slug '" + slug + "' isn't usable. Use lowercase letters, digits, and single hyphens, "
                    + "for example -Pslug=my-post.");
        }
        List<String> tags = Tags.split(options.tags() == null ? "" : options.tags());
        String summary = options.summary() == null ? "" : options.summary().strip();
        boolean branch = !"false".equalsIgnoreCase(options.branch() == null ? "true" : options.branch());
        boolean pr = switch ((options.pr() == null ? "false" : options.pr()).strip().toLowerCase(Locale.ROOT)) {
            case "", "true" -> true;
            case "false" -> false;
            default -> throw fail("-Ppr takes no value (or true/false), not '" + options.pr() + "'.");
        };
        if (pr && !branch) {
            throw fail("-Ppr opens a pull request from the post/<slug> branch, so it can't be combined with "
                    + "-Pbranch=false.");
        }

        Path drafts = site.drafts();
        if (!Files.isDirectory(drafts)) {
            throw fail("Run this from the repository root (no " + site.display(drafts) + " here).");
        }
        refuseExistingSlug(slug);
        warnAboutTagSpellings(tags);

        String branchName = "post/" + slug;
        String repo = pr ? checkReadyToOpenPr() : null;
        if (branch) {
            if (!git.output("rev-parse", "--verify", "--quiet", "refs/heads/" + branchName).isEmpty()
                    || !git.output("rev-parse", "--verify", "--quiet", "refs/remotes/origin/" + branchName).isEmpty()) {
                throw fail("The branch " + branchName + " already exists.");
            }
            if (!git.output("status", "--porcelain", "--untracked-files=no").isEmpty()) {
                throw fail("You have uncommitted changes. Commit or stash them first, so they don't end up on "
                        + branchName + " (or pass -Pbranch=false to stay on the current branch).");
            }
            if (git.output("rev-parse", "--verify", "--quiet", "refs/heads/main").isEmpty()) {
                throw fail("There's no local main branch to start " + branchName + " from.");
            }
            gitOrFail("switch", "--quiet", "--create", branchName, "main");
        }

        Path file = drafts.resolve(slug + ".md");
        String header = String.join("\n",
                "title=" + title,
                "date=" + LocalDate.now(clock),
                "type=post",
                "tags=" + String.join(", ", tags),
                "status=draft",
                "summary=" + summary,
                "~~~~~~",
                "",
                "");
        Files.writeString(file, header, StandardCharsets.UTF_8);

        out.println();
        out.println("Created " + site.display(file) + " (status=draft, never published)");
        if (branch) {
            out.println("Switched to the new branch " + branchName + " (from main)");
        }
        String prUrl = pr ? commitPushAndOpenPr(file, title, slug, branchName, repo) : null;

        out.println();
        out.println("Next:");
        out.println("  Write the post, then preview it:  ./gradlew bakePreview");
        out.println("    http://localhost:8080/blog/drafts/" + slug + "-draft.html");
        if (summary.isEmpty()) {
            out.println("  Fill in summary= (optional): it's the teaser under the title on the home page.");
        }
        if (prUrl != null) {
            out.println("  Commit and push to " + branchName + " as you go; the draft PR picks it up.");
            out.println("  To publish: set status=published, move the file to " + site.display(site.blog()) + "/, drop [WIP] from");
            out.println("    the PR title, mark it ready (gh pr ready), and merge.");
        } else {
            out.println("  To publish: set status=published, move the file to " + site.display(site.blog()) + "/, and open a PR.");
        }
        out.println("    It will live at " + siteHost + "/blog/" + slug + ".html (permanent).");
        return file;
    }

    /**
     * Checks everything -Ppr needs before anything is created ({@link GitHubCli}): gh is installed and logged in,
     * there's an origin to push to, gh can find the GitHub repository, git knows who is committing, and main has
     * nothing unpushed (it would ride along in the post's PR). Returns the repository as owner/name.
     */
    private String checkReadyToOpenPr() throws IOException, InterruptedException {
        GitHubCli gh = new GitHubCli(git, runner, "-Ppr");
        String repo = gh.repository("the post branch");
        gh.committerKnown("the draft");
        gh.mainIsPushed("the post's pull request");
        return repo;
    }

    /**
     * Commits the new draft, pushes the branch, and opens a draft PR. If a step fails, it says what happened and
     * what to run next, and stops without undoing anything. Returns the PR's URL.
     */
    private String commitPushAndOpenPr(Path file, String title, String slug, String branchName, String repo)
            throws IOException, InterruptedException {
        String shownFile = site.display(file);
        String prTitle = "[WIP] " + title;
        Path body = Files.createTempFile("newpost-pr-body-", ".md");
        Files.writeString(body, String.join("\n",
                "Draft post: **" + title + "**",
                "",
                "- File: `" + shownFile + "` (`status=draft`)",
                "- Preview: `./gradlew bakePreview`, then http://localhost:8080/blog/drafts/" + slug + "-draft.html",
                "- Once published, it will live at " + siteHost + "/blog/" + slug + ".html",
                "",
                "Work in progress, started with `./gradlew newPost -Ppr`. Drafts are never deployed, and pull "
                        + "requests never deploy, so nothing here is live yet. To publish: set `status=published`, "
                        + "move the file to `" + site.display(site.blog()) + "/`, drop `[WIP]` from the title, mark the PR ready for "
                        + "review, and merge.",
                ""), StandardCharsets.UTF_8);
        List<String> push = List.of("git", "push", "--set-upstream", "origin", branchName);
        List<String> openPr = List.of("gh", "pr", "create", "--draft", "--repo", repo, "--base", "main",
                "--head", branchName, "--title", prTitle, "--body-file", body.toString());
        boolean retryNeedsBody = false;
        try {
            return commitPushAndOpenPr(shownFile, title, branchName, prTitle, push, openPr);
        } catch (ToolFailure e) {
            retryNeedsBody = true;   // its message tells the user to run gh pr create --body-file <body> next
            throw e;
        } finally {
            if (!retryNeedsBody) {
                Files.deleteIfExists(body);
            }
        }
    }

    private String commitPushAndOpenPr(String shownFile, String title, String branchName, String prTitle,
            List<String> push, List<String> openPr) throws IOException, InterruptedException {
        List<String> add = List.of("git", "add", "--", shownFile);
        List<String> commit = List.of("git", "commit", "--quiet",
                "-m", "Starting a draft post: " + title,
                "-m", "Created with ./gradlew newPost -Ppr. It has status=draft, so it renders for local preview "
                        + "only and isn't published.");
        if (!runner.runVisibly(add) || !runner.runVisibly(commit)) {
            throw GitHubCli.stopAfter("Committing the draft failed (see the git output above). The draft file is in place on "
                    + branchName + ", uncommitted.", List.of(add, commit, push, openPr));
        }
        out.println("Committed " + shownFile + " on " + branchName);

        if (!runner.runVisibly(push)) {
            throw GitHubCli.stopAfter("The draft is committed on " + branchName + ", but pushing it to origin failed "
                    + "(see the git output above).", List.of(push, openPr));
        }
        out.println("Pushed " + branchName + " to origin");

        Result created;
        try {
            created = runner.run(openPr, Stderr.SHOW);
        } catch (IOException e) {
            throw GitHubCli.stopAfter(branchName + " is committed and pushed, but opening the pull request failed (gh didn't run).", List.of(openPr));
        }
        if (created.exit() != 0) {
            throw GitHubCli.stopAfter(branchName + " is committed and pushed, but opening the pull request failed (see the gh output above).",
                    List.of(openPr));
        }
        String url = created.output().lines().filter(l -> l.startsWith("https://")).reduce((a, b) -> b)
                .orElse(created.output());
        out.println("Opened the draft pull request \"" + prTitle + "\": " + url);
        return url;
    }

    /**
     * The file and URL name made from a title: "Boyd's Law: Iteration &amp; Speed!" becomes
     * "boyds-law-iteration-speed".
     *
     * @param title the title
     * @return the slug
     */
    static String slugify(String title) {
        String ascii = Normalizer.normalize(title, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return ascii.toLowerCase(Locale.ROOT)
                .replaceAll("['’]", "")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
    }

    private void refuseExistingSlug(String slug) throws IOException {
        for (Path dir : List.of(site.blog(), site.drafts())) {
            for (String ext : EXTENSIONS) {
                Path existing = dir.resolve(slug + "." + ext);
                if (Files.exists(existing)) {
                    throw fail(site.display(existing) + " already exists. Pick another title or pass -Pslug=<other-slug>.");
                }
            }
        }
        Path publishedUrls = site.publishedUrls();
        if (Files.exists(publishedUrls) && Files.readAllLines(publishedUrls).contains("blog/" + slug + ".html")) {
            throw fail("/blog/" + slug + ".html was already published (" + site.display(publishedUrls) + "). Published URLs are "
                    + "permanent, so pick another title or pass -Pslug=<other-slug>.");
        }
    }

    /** Tags that differ only by case make separate tag pages that each list only some posts (T055). */
    private void warnAboutTagSpellings(List<String> tags) throws IOException {
        if (tags.isEmpty()) {
            return;
        }
        List<Path> posts;
        try (Stream<Path> files = Files.list(site.blog())) {
            posts = files.filter(Files::isRegularFile).toList();
        }
        Map<String, TreeSet<String>> spellings = Tags.spellingsIn(posts);
        for (String tag : tags) {
            TreeSet<String> existing = spellings.get(tag.toLowerCase(Locale.ROOT));
            if (existing == null) {
                out.println("Note: '" + tag + "' is a new tag, so publishing creates /tags/" + tag + ".html.");
            } else if (!existing.contains(tag)) {
                out.println("WARNING: the tag '" + tag + "' is already used as " + existing + ". "
                        + "A different capitalization makes a separate tag page. Use the existing spelling.");
            }
        }
    }

    private void gitOrFail(String... args) throws InterruptedException {
        if (!git.runVisibly(args)) {
            throw fail("git " + String.join(" ", args) + " failed.");
        }
    }

    private static ToolFailure fail(String message) {
        return ToolFailure.failed(message);
    }
}
