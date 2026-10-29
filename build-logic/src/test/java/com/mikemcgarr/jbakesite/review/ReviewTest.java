package com.mikemcgarr.jbakesite.review;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mikemcgarr.jbakesite.internal.Git;
import com.mikemcgarr.jbakesite.internal.ProcessRunner;
import com.mikemcgarr.jbakesite.internal.SiteLayout;
import com.mikemcgarr.jbakesite.internal.TestRepositories;
import com.mikemcgarr.jbakesite.internal.ToolFailure;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The review on a small site in a real git repository, with the baked pages written by hand: one case for each
 * check (E1 to E5, W2 to W7), the suppressions, {@code -Pall}, and what stops it running (T117's design in
 * docs/05-REVIEW-FLOW.md).
 */
class ReviewTest {

    private static final Clock TODAY = Clock.fixed(Instant.parse("2026-10-28T12:00:00Z"), ZoneOffset.UTC);
    private static final String POST = "title=A Post\ndate=2026-01-02\ntype=post\ntags=testing, java\nstatus=published\nsummary=A summary.\n~~~~~~\n\nThe body.\n";
    private static final String OTHER = "title=Other\ndate=2025-05-05\ntype=post\ntags=testing\nstatus=published\nsummary=Yes.\n~~~~~~\n\nOther.\n";
    private static final String ABOUT = "title=About\ndate=2026-01-01\ntype=page\nstatus=published\n~~~~~~\n\nAbout me.\n";

    @TempDir
    Path root;

    private SiteLayout site;
    private Path baked;
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private Review review;

    @BeforeEach
    void aCommittedSiteWithABake() throws IOException, InterruptedException {
        site = SiteLayout.standard(root);
        baked = root.resolve("build/jbake");
        Files.createDirectories(site.blog());
        Files.createDirectories(site.templates());
        Files.createDirectories(site.mastheads());
        Files.createDirectories(site.publishedUrls().getParent());
        Files.writeString(site.blog().resolve("a-post.md"), POST);
        Files.writeString(site.blog().resolve("other.md"), OTHER);
        Files.writeString(site.content().resolve("about.md"), ABOUT);
        Files.writeString(site.templates().resolve("post.ftl"), "<html>${content.body}</html>\n");
        Files.writeString(site.assets().resolve("robots.txt"), "User-agent: *\n");
        Files.writeString(site.images().resolve("old.png"), "png");
        Files.writeString(site.publishedUrls(), "index.html\nabout.html\nblog/a-post.html\nblog/other.html\nimg/old.png\n");
        Files.writeString(root.resolve(".gitignore"), "build/\n");
        bake("index.html", "<html><body><a href=\"blog/a-post.html\">A Post</a></body></html>\n");
        bake("about.html", "<html><body><p>About me.</p></body></html>\n");
        bake("blog/a-post.html", "<html><body><p>The body.</p></body></html>\n");
        bake("blog/other.html", "<html><body><p>Other.</p></body></html>\n");
        TestRepositories.init(root);
        TestRepositories.commitAll(root, "the site");
        PrintStream printer = new PrintStream(out, true, StandardCharsets.UTF_8);
        review = new Review(site, baked, "https://www.mikemcgarr.com", new Git(new ProcessRunner(root, printer, printer)), TODAY, printer);
    }

    private void bake(String page, String html) throws IOException {
        Path file = baked.resolve(page);
        Files.createDirectories(file.getParent());
        Files.writeString(file, html);
    }

    private void edit(String file, String text) throws IOException {
        Files.writeString(root.resolve(file), text);
    }

    private ReviewResult run() throws IOException, InterruptedException {
        return review.run(ReviewOptions.DEFAULTS);
    }

    private static Finding only(ReviewResult result) {
        assertEquals(1, result.findings().size(), result.findings().toString());
        return result.findings().get(0);
    }

    private static List<String> ids(ReviewResult result) {
        return result.findings().stream().map(Finding::id).toList();
    }

    @Test
    void nothingToReviewOnACleanTree() throws IOException, InterruptedException {
        ReviewResult result = run();

        assertTrue(result.nothingToReview());
        assertEquals("Nothing to review: no changes under src/jbake or src/case-redirects since the merge base with main.\n",
                out.toString(StandardCharsets.UTF_8));
    }

    @Test
    void e1MarkdownLinkSyntax() throws IOException, InterruptedException {
        edit("src/jbake/content/blog/a-post.md", POST + "See [Booz Allen] https://www.boozallen.com/ and [ padded](x) and [b] (y).\n"
                + "A `[code] span` is fine, and so is \\[escaped\\]. Lone [ bracket.\n[ref][] and [^1] without definitions.\n");

        ReviewResult result = run();

        assertEquals(List.of("E1", "E1", "E1", "E1", "E1", "E1"), ids(result));
        Finding first = result.findings().get(0);
        assertEquals("src/jbake/content/blog/a-post.md", first.file());
        assertEquals(10, first.line());
        assertEquals("Link text \"[Booz Allen]\" is followed by \"https://www.boozallen.com/\" with no \"(\": it renders as literal brackets.", first.message());
        assertEquals("[Booz Allen](https://www.boozallen.com/)", first.fix());
        List<String> messages = result.findings().stream().map(Finding::message).toList();
        assertTrue(messages.contains("A space between \"[b]\" and \"(\": it renders as literal brackets, not a link."), messages.toString());
        assertTrue(messages.contains("The link text \"[ padded]\" starts or ends with a space, which shows in the link."), messages.toString());
        assertTrue(messages.contains("Unmatched \"[\" on this line (outside code). A bracket that should show literally is written \\[ or \\]."), messages.toString());
        assertTrue(messages.contains("The reference link \"[ref][]\" has no \"[ref]: URL\" definition, so it renders as literal text."), messages.toString());
        assertTrue(messages.contains("The footnote \"[^1]\" has no \"[^1]: text\" definition, so it renders as literal text."), messages.toString());
        assertEquals(List.of("blog/a-post.html"), result.reviewedPages());
        assertEquals(1, result.changedFiles());
    }

    @Test
    void e2BracketsLeftInTheRenderedTextPointAtTheSourceLine() throws IOException, InterruptedException {
        edit("src/jbake/content/blog/a-post.md", POST + "Worked at [Acme] for years.\nAnd \\[this\\] is meant to show.\n");
        bake("blog/a-post.html", "<html><body><p>Worked at [Acme] for years.</p>\n<p>And [this] is meant to show.</p>\n"
                + "<pre>[code] is skipped</pre><details class=\"archived-comments\">[a comment]</details></body></html>\n");

        Finding f = only(run());

        assertEquals("E2", f.id());
        assertEquals("src/jbake/content/blog/a-post.md", f.file());
        assertEquals(10, f.line());
        assertEquals("The page shows \"Worked at [Acme] for years.\" with the brackets as text. A Markdown link didn't form (see E1),"
                + " or a bracket needs escaping as \\[.", f.message());
    }

    @Test
    void e2OnAPageWithoutASourcePointsAtTheHtmlLine() throws IOException, InterruptedException {
        edit("src/jbake/templates/post.ftl", "<html>[changed] ${content.body}</html>\n");
        bake("index.html", "<html><body>\n<p>[Home]</p></body></html>\n");
        bake("archive.html", "<html></html>\n");
        bake("tags/index.html", "<html></html>\n");

        ReviewResult result = run();

        Finding e2 = result.findings().stream().filter(f -> f.id().equals("E2")).findFirst().orElseThrow();
        assertEquals("index.html", e2.file());
        assertEquals(2, e2.line());
        assertTrue(e2.message().endsWith("or a bracket needs escaping as \\[. (On index.html.)"), e2.message());
        assertEquals(List.of("index.html", "archive.html", "tags/index.html", "about.html", "blog/a-post.html", "blog/other.html"),
                result.reviewedPages(), "a template change reviews the sample pages: the four named ones, the newest and the oldest post");
    }

    @Test
    void e3LocalLinksAndImagesMustResolveInTheBake() throws IOException, InterruptedException {
        edit("src/jbake/content/blog/a-post.md", POST + "[gone](missing.html) [frag](/about.html#nowhere) [ok](/about.html)"
                + " [out](https://example.com/x) [own](https://www.mikemcgarr.com/about.html) [stale](/img/gone.png) ![x](/img/old.png)\n");
        bake("blog/a-post.html", "<html><body><a href=\"missing.html\">gone</a> <a href=\"/about.html#nowhere\">frag</a> <a href=\"/about.html\">ok</a>"
                + " <a href=\"https://example.com/x\">out</a> <a href=\"https://www.mikemcgarr.com/about.html\">own</a>"
                + " <a href=\"/img/gone.png\">stale</a> <img src=\"/img/old.png\" srcset=\"/img/old.png 1x, /img/old-2x.png 2x\"></body></html>\n");
        bake("img/gone.png", "left over from an earlier bake");

        ReviewResult result = run();

        List<String> messages = result.findings().stream().map(f -> f.id() + " " + f.file() + ":" + f.line() + " " + f.message()).toList();
        assertEquals(List.of(
                "E3 blog/a-post.html:1 <img srcset=\"/img/old-2x.png\">: nothing at /img/old-2x.png in the baked site. (On blog/a-post.html.)",
                "E3 src/jbake/content/blog/a-post.md:10 <a href=\"missing.html\">: nothing at /blog/missing.html in the baked site.",
                "E3 src/jbake/content/blog/a-post.md:10 <a href=\"/about.html#nowhere\">: /about.html has no id=\"nowhere\" to jump to.",
                "E3 src/jbake/content/blog/a-post.md:10 <a href=\"/img/gone.png\">: nothing at /img/gone.png in src/jbake/assets any more"
                        + " (the copy in build/jbake is left over from an earlier bake)."),
                messages, "sorted by file: a finding with no source line names the page");
    }

    @Test
    void e4TheHeader() throws IOException, InterruptedException {
        edit("src/jbake/content/blog/a-post.md", "title=Bad\ndate=yesterday\ntype=essay\nstatus=live\n~~~~~~\n\nBody.\r\n");
        edit("src/jbake/content/blog/new-post.md", "title=New\ndate=2026-10-28\ntype=post\nstatus=draft\n~~~~~~\n\nNo summary.\n");
        edit("src/jbake/content/blog/no-separator.md", "title=Broken\ndate=2026-10-28\ntype=post\nstatus=draft\n\nBody\n");

        ReviewResult result = run();

        List<String> messages = result.findings().stream().map(f -> f.id() + " " + f.file() + ":" + f.line() + " " + f.message()
                + (f.fix() == null ? "" : " | " + f.fix())).toList();
        assertEquals(List.of(
                "E4 src/jbake/content/blog/a-post.md:2 date=yesterday isn't a date of the form YYYY-MM-DD. | date=2026-10-28",
                "E4 src/jbake/content/blog/a-post.md:3 type=essay isn't post or page. | type=post",
                "E4 src/jbake/content/blog/a-post.md:4 status=live isn't draft or published. | status=draft",
                "E4 src/jbake/content/blog/a-post.md:7 The file has a CR line ending. Content is LF only (AGENTS.md Rule 3; CI fails on this)."
                        + " | Convert the line endings: perl -pi -e 's/\\r\\n?/\\n/g' src/jbake/content/blog/a-post.md",
                "E4 src/jbake/content/blog/new-post.md:1 A new post needs a summary=: it's the teaser on the home page, in the feed and on share cards."
                        + " | summary=One sentence that says what the post is about.",
                "E4 src/jbake/content/blog/new-post.md:1 JBake produced no page blog/new-post-draft.html in build/jbake. Check the header, then ./gradlew bake.",
                "E4 src/jbake/content/blog/no-separator.md:1 No ~~~~~~ line ends the header, so JBake won't render this file."
                        + " | Put ~~~~~~ on its own line after the key=value lines."),
                messages);
        assertEquals(List.of(), result.reviewedPages(), "a page with a broken header isn't expected in the bake; a missing summary doesn't stop the page check");
    }

    @Test
    void e4WhenJbakeProducedNoPage() throws IOException, InterruptedException {
        edit("src/jbake/content/blog/unbaked.md", POST);

        Finding f = only(run());

        assertEquals("E4", f.id());
        assertEquals("src/jbake/content/blog/unbaked.md", f.file());
        assertEquals("JBake produced no page blog/unbaked.html in build/jbake. Check the header, then ./gradlew bake.", f.message());
    }

    @Test
    void e5TheImageRulesAndTheMastheadVariants() throws IOException, InterruptedException {
        Path wide = site.mastheads().resolve("wide.jpg");
        ImageIO.write(new BufferedImage(2000, 10, BufferedImage.TYPE_INT_RGB), "jpeg", wide.toFile());
        byte[] heavy = new byte[500 * 1024];
        new Random(1).nextBytes(heavy);
        Files.write(site.images().resolve("heavy.png"), heavy);
        edit("src/jbake/content/blog/a-post.md", POST.replace("type=post\n", "type=post\nmasthead=nowhere.jpg\n"));
        edit("src/jbake/content/about.md", ABOUT.replace("type=page\n", "type=page\nmasthead=wide.jpg\n"));

        ReviewResult result = run();

        List<String> messages = result.findings().stream().filter(Finding::error).map(f -> f.id() + " " + f.file() + ":" + f.line() + " " + f.message()
                + (f.fix() == null ? "" : " | " + f.fix())).toList();
        assertEquals(List.of(
                "E5 src/jbake/assets/img/heavy.png:0 The image is 500 KB; the target is 400 KB or less (AGENTS.md Rule 4)."
                        + " | Re-encode it (JPEG quality 80, sRGB, metadata stripped) before adding it.",
                "E5 src/jbake/assets/img/masthead/wide.jpg:0 The image is 2000 px wide; the limit is 1920 px (AGENTS.md Rule 4). | Resize it to at most 1920 px wide.",
                "E5 src/jbake/assets/img/masthead/wide.jpg:0 The masthead wide.jpg has no wide-960.jpg or wide-1440.jpg variant for phones and tablets"
                        + " (CI fails without them). | ./gradlew prepareImage -Pimage=src/jbake/assets/img/masthead/wide.jpg -Pvariants",
                "E5 src/jbake/content/about.md:4 The masthead wide.jpg has no wide-960.jpg or wide-1440.jpg variant for phones and tablets"
                        + " (CI fails without them). | ./gradlew prepareImage -Pimage=src/jbake/assets/img/masthead/wide.jpg -Pvariants",
                "E5 src/jbake/content/blog/a-post.md:4 masthead=nowhere.jpg isn't in src/jbake/assets/img/masthead."
                        + " | Add the image there, then run ./gradlew prepareImage -Pimage=<image> -Pvariants"),
                messages);
    }

    @Test
    void w2APageChangedButItsDateDidNot() throws IOException, InterruptedException {
        edit("src/jbake/content/about.md", ABOUT + "More.\n");

        Finding f = only(run());

        assertEquals("W2", f.id());
        assertFalse(f.error());
        assertEquals("src/jbake/content/about.md", f.file());
        assertEquals(2, f.line());
        assertEquals("This page changed but date=2026-01-01 did not. The sitemap reports it as lastmod. Today is 2026-10-28.", f.message());
        assertEquals("date=2026-10-28", f.fix());
    }

    @Test
    void w3TagSpellingsAndNewTags() throws IOException, InterruptedException {
        edit("src/jbake/content/blog/a-post.md", POST.replace("tags=testing, java", "tags=Testing, java, brand-new"));

        ReviewResult result = run();

        List<String> messages = result.findings().stream().map(f -> f.id() + ":" + f.line() + " " + f.message() + " | " + f.fix()).toList();
        assertEquals(List.of(
                "W3:4 The tag 'Testing' is already used as [testing]. A different spelling or capitalization makes a separate tag page. | Use testing",
                "W3:4 'brand-new' is a new tag, so publishing makes /tags/brand-new.html. Fine if intended; check the Topics page for a close match. | null"),
                messages, "java was there at the merge base, so it isn't new");
    }

    @Test
    void w4AltText() throws IOException, InterruptedException {
        edit("src/jbake/content/blog/a-post.md", POST + "<img src=\"/img/old.png\">\n<img src=\"/img/old.png\" alt=\"old.png\">\n![](/img/old.png)\n"
                + "<img src=\"/img/old.png\" alt=\"The old logo\">\n");
        bake("blog/a-post.html", "<html><body><img src=\"/img/old.png\"></body></html>\n");

        ReviewResult result = run();

        assertEquals(List.of(
                "W4:10 This <img> has no alt text. Say what the image shows, or alt=\"\" if it's decorative.",
                "W4:11 alt=\"old.png\" is the image's file name. Say what the image shows.",
                "W4:12 This image has empty alt text. Say what it shows: ![what it shows](…)."),
                result.findings().stream().map(f -> f.id() + ":" + f.line() + " " + f.message()).toList());
    }

    @Test
    void w6ADeletedOrRenamedFileRemovesAPublishedUrl() throws IOException, InterruptedException {
        Files.delete(site.images().resolve("old.png"));
        Files.move(site.blog().resolve("other.md"), site.blog().resolve("renamed.md"));
        TestRepositories.git(root, "add", "-A");
        bake("blog/renamed.html", "<html><body><p>Other.</p></body></html>\n");

        ReviewResult result = run();

        List<String> messages = result.findings().stream().map(f -> f.id() + " " + f.file() + " " + f.message()).toList();
        assertEquals(List.of(
                "W6 src/jbake/assets/img/old.png Deleting this file removes the published URL /img/old.png. Published URLs are permanent (AGENTS.md Rule 1):"
                        + " this needs the owner's approval, a redirect stub at the old path, and a line in docs/baseline/expected-removals.txt."
                        + " CI's check-urls.sh fails otherwise.",
                "W6 src/jbake/content/blog/other.md Renaming this file removes the published URL /blog/other.html. Published URLs are permanent (AGENTS.md Rule 1):"
                        + " this needs the owner's approval, a redirect stub at the old path, and a line in docs/baseline/expected-removals.txt."
                        + " CI's check-urls.sh fails otherwise."),
                messages);
        assertEquals(List.of("blog/renamed.html"), result.reviewedPages());
    }

    @Test
    void w7AnExistingPostWithoutASummary() throws IOException, InterruptedException {
        edit("src/jbake/content/blog/a-post.md", POST.replace("summary=A summary.\n", ""));

        Finding f = only(run());

        assertEquals("W7", f.id());
        assertEquals(1, f.line());
        assertEquals("This post has no summary=. The home page, the feed and share cards use it. Adding one is your call.", f.message());
        assertNull(f.fix());
    }

    @Test
    void aCommentOnTheLineBeforeSuppressesAFindingInTheSourceAndThePage() throws IOException, InterruptedException {
        edit("src/jbake/content/blog/a-post.md", POST + "<!-- review: ignore E1, E2, the brackets are intended -->\nSee [Acme] https://acme.example/.\n");
        bake("blog/a-post.html", "<html><body><!-- review: ignore E1, E2, the brackets are intended -->\n<p>See [Acme] https://acme.example/.</p></body></html>\n");

        ReviewResult result = run();

        assertEquals(List.of(), result.findings());
        assertEquals(2, result.suppressed(), "the E1 in the Markdown and the E2 on the page");
    }

    @Test
    void allReviewsEveryPageAndKeepsTheDiffOnlyWarningsQuiet() throws IOException, InterruptedException {
        edit("src/jbake/content/blog/a-post.md", POST.replace("summary=A summary.\n", "").replace("tags=testing, java", "tags=Testing, new-one"));

        ReviewResult result = review.run(new ReviewOptions("main", true, false, false));

        assertTrue(result.all());
        assertEquals(List.of("about.html", "blog/a-post.html", "blog/other.html"), result.reviewedPages());
        assertEquals(List.of("W3", "W3"), ids(result), "a spelling clash is still a finding, seen from both posts; W7 and a new tag are not under -Pall");
        assertEquals(3, result.changedFiles(), "every content file counts as changed, though the summary doesn't say so under -Pall");
    }

    @Test
    void failsWhenThereAreErrorsOrStrictWarnings() throws IOException, InterruptedException {
        edit("src/jbake/content/about.md", ABOUT + "More.\n");

        ReviewResult warnings = run();

        assertFalse(warnings.failed(false));
        assertTrue(warnings.failed(true));
        assertEquals(1, warnings.warnings());
        assertEquals(0, warnings.errors());
    }

    @Test
    void stopsWhenItCannotRun() throws IOException, InterruptedException {
        ToolFailure noBase = assertThrows(ToolFailure.class, () -> review.run(new ReviewOptions("nowhere", false, false, false)));
        assertEquals(ToolFailure.COULD_NOT_RUN, noBase.exitCode());
        assertEquals("The base 'nowhere' isn't a commit here. Pass -Pbase=<ref> (for example -Pbase=origin/main), or fetch it first.", noBase.getMessage());

        Files.delete(baked.resolve("index.html"));
        ToolFailure noBake = assertThrows(ToolFailure.class, this::run);
        assertEquals("No baked site in build/jbake. Run ./gradlew bake first (./gradlew reviewChanges does).", noBake.getMessage());
    }

    @Test
    void onlineIsAcceptedAndIgnored() throws IOException, InterruptedException {
        review.run(new ReviewOptions("main", false, false, true));

        assertTrue(out.toString(StandardCharsets.UTF_8).startsWith("Note: external links aren't checked yet (task T119); -Ponline is accepted and ignored.\n"),
                out.toString());
    }
}
