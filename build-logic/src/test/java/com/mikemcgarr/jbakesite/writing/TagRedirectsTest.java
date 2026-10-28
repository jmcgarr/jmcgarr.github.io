package com.mikemcgarr.jbakesite.writing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mikemcgarr.jbakesite.internal.SiteLayout;
import com.mikemcgarr.jbakesite.internal.ToolFailure;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The encoders against the inputs the stubs contain, a stub byte for byte against one committed on the site, write
 * mode's idempotence, and check mode's failures (T139's Test line).
 */
class TagRedirectsTest {

    private static final String HOST = "https://www.mikemcgarr.com";

    /** src/jbake/assets/tags/boyd's law.html as committed: the apostrophe is %27 in the href and &#x27; in text. */
    private static final String BOYDS_LAW_STUB = """
            <!DOCTYPE html>
            <!-- tag redirect stub (scripts/tag-redirects.py) -->
            <html lang="en">
            <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>Tag: boyd&#x27;s-law (moved)</title>
            <meta name="robots" content="noindex">
            <link rel="canonical" href="https://www.mikemcgarr.com/tags/boyd&#x27;s-law.html">
            <meta http-equiv="refresh" content="0; url=boyd%27s-law.html">
            </head>
            <body>
            <p>This page has moved to <a href="boyd%27s-law.html">https://www.mikemcgarr.com/tags/boyd&#x27;s-law.html</a>.</p>
            </body>
            </html>
            """;

    @TempDir
    Path root;

    private SiteLayout site;
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private TagRedirects tool;

    @BeforeEach
    void aSiteWithRenamedTags() throws IOException {
        site = SiteLayout.standard(root);
        Files.createDirectories(site.blog());
        Files.createDirectories(site.publishedUrls().getParent());
        Files.writeString(site.blog().resolve("a.md"), "title=A\ntype=post\ntags=acceptance test, boyd's law, devops, build automation\n"
                + "status=published\n~~~~~~\n");
        Files.writeString(site.blog().resolve("b.md"), "title=B\ntype=post\ntags=presentations, .NET\nstatus=published\n~~~~~~\n");
        Files.writeString(site.blog().resolve("draft.md"), "title=D\ntype=post\ntags=DevOps, build\nstatus=draft\n~~~~~~\n");
        Files.writeString(site.publishedUrls(), "index.html\ntags/acceptance test.html\ntags/boyd's law.html\ntags/DevOps.html\n"
                + "tags/devops.html\ntags/build.html\ntags/talks.html\ntags/.NET.html\n");
        Map<String, String> renames = new LinkedHashMap<>();
        renames.put("build", "build automation");
        renames.put("talks", "presentations");
        tool = new TagRedirects(site, HOST, renames, new PrintStream(out, true, StandardCharsets.UTF_8));
    }

    @Test
    void urlPathEncodesLikeFreemarkersUrlPath() {
        assertEquals("tags/acceptance%20test.html", TagRedirects.urlPath("tags/acceptance test.html"));
        assertEquals("tags/boyd's-law.html", TagRedirects.urlPath("tags/boyd's-law.html"), "the apostrophe is safe, as in ?url_path");
        assertEquals("tags/.NET.html", TagRedirects.urlPath("tags/.NET.html"));
        assertEquals("tags/c%2B%2B.html", TagRedirects.urlPath("tags/c++.html"), "not java.net.URLEncoder's +");
        assertEquals("tags/c%23.html", TagRedirects.urlPath("tags/c#.html"));
        assertEquals("tags/caf%C3%A9.html", TagRedirects.urlPath("tags/café.html"), "UTF-8 bytes, upper-case hex");
        assertEquals("~x!*()", TagRedirects.urlPath("~x!*()"), "the tilde and the sub-delims FreeMarker leaves alone");
        assertEquals("a%26b%3Fc%3Dd", TagRedirects.urlPath("a&b?c=d"));
    }

    @Test
    void unquoteReversesTheEncodingAndLeavesPlusAlone() {
        assertEquals("tags/boyd's-law.html", TagRedirects.unquote("tags/boyd%27s-law.html"));
        assertEquals("café+x", TagRedirects.unquote("caf%C3%A9+x"));
        assertEquals("100%", TagRedirects.unquote("100%"), "a lone percent stays");
    }

    @Test
    void htmlEscapeAndUnescapeArePythonsHtmlModule() {
        assertEquals("&lt;a href=&quot;x&quot;&gt; &amp; it&#x27;s", TagRedirects.htmlEscape("<a href=\"x\"> & it's"));
        assertEquals("<a href=\"x\"> & it's é", TagRedirects.htmlUnescape("&lt;a href=&quot;x&quot;&gt; &amp; it&#x27;s &#233;"));
        assertEquals("'acceptance test'", TagRedirects.pyRepr("acceptance test"));
        assertEquals("\"boyd's law\"", TagRedirects.pyRepr("boyd's law"), "repr switches to double quotes for an apostrophe");
        assertEquals("None", TagRedirects.pyRepr((String) null));
        assertEquals("['a', \"b's\"]", TagRedirects.pyRepr(List.of("a", "b's")));
    }

    @Test
    void aStubIsByteForByteWhatThePythonScriptWrote() {
        assertEquals(BOYDS_LAW_STUB, tool.stub("tags/boyd's-law.html"));
    }

    @Test
    void worksOutTheRedirectsFromTheBaselineThePostsAndRenames() throws IOException {
        Map<String, String> redirects = tool.redirects();

        assertEquals(Map.of(
                "tags/acceptance test.html", "tags/acceptance-test.html",
                "tags/boyd's law.html", "tags/boyd's-law.html",
                "tags/DevOps.html", "tags/devops.html",          // a case-only twin, from the published spelling
                "tags/build.html", "tags/build-automation.html",  // RENAMES (H9)
                "tags/talks.html", "tags/presentations.html"),    // RENAMES (H2)
                redirects, "the hyphenated twins (since T039) are real pages now, so they get no stub");
        assertEquals(List.of("tags/acceptance-test.html", "tags/boyd's-law.html"),
                tool.oldTagUrls().stream().filter(o -> o.source().equals("since T039")).map(TagRedirects.OldUrl::url).toList());
    }

    @Test
    void writeModeIsIdempotentPutsCaseOnlyStubsWithTheCaseRedirectsAndNeverDeletes() throws IOException {
        tool.write();

        assertTrue(Files.exists(site.assets().resolve("tags/acceptance test.html")));
        assertTrue(Files.exists(site.assets().resolve("tags/boyd's law.html")));
        assertTrue(Files.exists(site.assets().resolve("tags/build.html")));
        assertTrue(Files.exists(site.caseRedirects().resolve("tags/DevOps.html")), "differs from the real page only by case");
        assertEquals(BOYDS_LAW_STUB, Files.readString(site.assets().resolve("tags/boyd's law.html")));
        assertTrue(out.toString(StandardCharsets.UTF_8).endsWith("5 tag redirect stubs (5 written, 0 already up to date)\n"), out.toString());

        out.reset();
        Files.writeString(site.assets().resolve("tags/old.html"), TagRedirects.MARKER + "\nan old stub\n");
        tool.write();

        assertEquals("NOTE: src/jbake/assets/tags/old.html is a stub this script no longer makes. It's a published URL: keep it unless approved.\n"
                + "5 tag redirect stubs (0 written, 5 already up to date)\n", out.toString(StandardCharsets.UTF_8));
        assertTrue(Files.exists(site.assets().resolve("tags/old.html")));
    }

    @Test
    void aRenameToATagNoPostUsesOrFromOneStillInUseIsAnError() throws IOException {
        Files.writeString(site.blog().resolve("b.md"), "title=B\ntype=post\ntags=.NET, build\nstatus=published\n~~~~~~\n");

        ToolFailure e = assertThrows(ToolFailure.class, tool::write);

        assertEquals(ToolFailure.FAILED, e.exitCode());
        assertEquals("ERROR: can't work out where these old tag URLs should point:\n"
                + "  RENAMES: 'build' is still used by a published post (its stub would replace the real page)\n"
                + "  RENAMES: 'talks' -> 'presentations', but no published post uses 'presentations'", e.getMessage());
    }

    @Test
    void anOldTagUrlWithNoMatchingTagIsAnError() throws IOException {
        Files.writeString(site.publishedUrls(), "tags/gone.html\ntags/DevOps.html\n");
        Files.writeString(site.blog().resolve("b.md"), "title=B\ntype=post\ntags=presentations, build automation, devops, DEVOPS\nstatus=published\n~~~~~~\n");
        Files.writeString(site.blog().resolve("a.md"), "title=A\ntype=post\ntags=devops\nstatus=published\n~~~~~~\n");

        ToolFailure e = assertThrows(ToolFailure.class, tool::write);

        assertEquals("ERROR: can't work out where these old tag URLs should point:\n"
                + "  tags/gone.html: no published tag matches: []\n"
                + "  tags/DevOps.html: no published tag is unambiguous: ['devops', 'DEVOPS']", e.getMessage());
    }

    @Test
    void theSitesOwnRenamesAreTheNineOwnerApprovedOnes() {
        assertEquals(9, TagRedirects.RENAMES.size());
        assertEquals("build automation", TagRedirects.RENAMES.get("build"));
        assertEquals("Excella Consulting", TagRedirects.RENAMES.get("excella"));
    }

    @Test
    void checkPassesOnASiteWithTheStubsAndRealTagPages() throws IOException {
        Path baked = bakedSite();

        tool.check(baked);

        String printed = out.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("6 tag pages checked for the tag name they show: 6 as written in the posts\n"), printed);
        if (Files.exists(baked.resolve("INDEX.HTML"))) {
            assertTrue(printed.contains("9 old tag URLs (7 baseline, 2 hyphenated since T039): 4 real tag pages, 4 redirect stubs, 1 not checkable here\n"
                    + "  ~ tags/DevOps.html (case-insensitive filesystem: same file as tags/devops.html; the stub is added only on Linux)\n"), printed);
        } else {
            assertTrue(printed.contains("9 old tag URLs (7 baseline, 2 hyphenated since T039): 4 real tag pages, 5 redirect stubs, 0 not checkable here\n"), printed);
        }
        assertTrue(printed.endsWith("OK: every old tag URL in build/jbake is a tag page or a stub whose target is a tag page\n"), printed);
    }

    @Test
    void checkFailsForAMissingStubAndAStubThatPointsElsewhere() throws IOException {
        Path baked = bakedSite();
        Files.delete(baked.resolve("tags/acceptance test.html"));
        Path boyds = baked.resolve("tags/boyd's law.html");
        Files.writeString(boyds, Files.readString(boyds).replace("url=boyd%27s-law.html", "url=devops.html"));

        ToolFailure e = assertThrows(ToolFailure.class, () -> tool.check(baked));

        assertEquals("FAIL: 3 problems in build/jbake", e.getMessage());
        String printed = out.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("  ! tags/acceptance test.html: missing from the build\n"), printed);
        assertTrue(printed.contains("  ! tags/boyd's law.html: redirects to tags/devops.html, expected tags/boyd's-law.html\n"), printed);
        assertTrue(printed.contains("  ! tags/boyd's law.html: rel=canonical is not https://www.mikemcgarr.com/tags/devops.html\n"), printed);
    }

    @Test
    void checkNamesATagPageThatShowsItsTagUnlikeThePosts() throws IOException {
        Path baked = bakedSite();
        Files.writeString(baked.resolve("tags/acceptance-test.html"), tagPage("acceptance test").replace("Tag: acceptance test", "Tag: acceptance-test"));

        ToolFailure e = assertThrows(ToolFailure.class, () -> tool.check(baked));

        assertEquals("FAIL: 1 problems in build/jbake", e.getMessage());
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("  ! tags/acceptance-test.html: shows the tag as 'acceptance-test', the posts"
                + " write 'acceptance test' (list it in src/jbake/templates/tag-names.ftl)\n"), out.toString());
    }

    @Test
    void checkNeedsABakedSite() {
        ToolFailure e = assertThrows(ToolFailure.class, () -> tool.check(root.resolve("build/nowhere")));

        assertEquals(ToolFailure.COULD_NOT_RUN, e.exitCode());
        assertEquals("build/nowhere has no tags/ folder. Bake first (./gradlew bake).", e.getMessage());
    }

    /**
     * What a bake of this site would hold: the real tag pages, the stubs JBake copies with the assets, and the case-only
     * stubs where the filesystem can hold them beside the real page (Linux), as the bake adds them.
     */
    private Path bakedSite() throws IOException {
        tool.write();
        out.reset();
        Path baked = root.resolve("build/jbake");
        Files.createDirectories(baked.resolve("tags"));
        Files.writeString(baked.resolve("index.html"), "<html></html>\n");
        for (String tag : List.of("acceptance test", "boyd's law", "devops", "build automation", "presentations", ".NET")) {
            Files.writeString(baked.resolve("tags/" + tag.replace(' ', '-') + ".html"), tagPage(tag));
        }
        List<Path> stubDirs = Files.exists(baked.resolve("INDEX.HTML")) ? List.of(site.assets()) : List.of(site.assets(), site.caseRedirects());
        for (Path dir : stubDirs) {
            try (Stream<Path> stubs = Files.list(dir.resolve("tags"))) {
                for (Path stub : stubs.toList()) {
                    Files.copy(stub, baked.resolve("tags").resolve(stub.getFileName()));
                }
            }
        }
        return baked;
    }

    private static String tagPage(String tag) {
        return "<!DOCTYPE html><html><head><meta property=\"og:title\" content=\"Tag: " + TagRedirects.htmlEscape(tag) + "\"></head><body>"
                + tag + "</body></html>\n";
    }
}
