package com.mikemcgarr.jbakesite.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The YAML front matter (T145), as the review, the tag tools and the parser engine read it, and as newPost writes it. */
class FrontMatterTest {

    @Test
    void readsTheKeysTheirLinesAndWhereTheBodyStarts() {
        FrontMatter fm = FrontMatter.parse("---\ntitle: About\ndate: 2026-10-21\ntype: page\nstatus: published\n---\n\nA page.\n");

        assertEquals("About", fm.get("title"));
        assertEquals("2026-10-21", fm.get("date"), "a date reads as the text written, not a parsed date");
        assertEquals(3, fm.lines().get("date"));
        assertEquals(6, fm.bodyStart(), "the first body line, 0-based: the line after the closing ---");
        assertTrue(fm.separated());
        assertFalse(fm.legacy());
        assertNull(fm.error());
        assertTrue(fm.isPublished());
        assertFalse(fm.isDraft());
        assertEquals("", fm.get("summary"), "an absent key reads as empty");
        assertEquals(List.of("title", "date", "type", "status"), List.copyOf(fm.values().keySet()), "in file order");
    }

    @Test
    void everyValueIsTextAndAListIsJbakesCommaSeparatedForm() {
        FrontMatter fm = FrontMatter.parse("---\ntitle: 2012\ndraft: no\ntags: [boyd's law, continuous delivery, devops]\n"
                + "also:\n  - a\n  - \"b: c\"\nsummary: \"Maven Tip: Disabling \\\"Profiles\\\"\"\nempty:\n---\n");

        assertEquals("2012", fm.get("title"));
        assertEquals("no", fm.get("draft"), "no boolean, number or null resolution");
        assertEquals("boyd's law, continuous delivery, devops", fm.get("tags"));
        assertEquals(List.of("boyd's law", "continuous delivery", "devops"), Tags.split(fm.get("tags")));
        assertEquals("a, b: c", fm.get("also"), "a block list reads the same as a flow list");
        assertEquals("Maven Tip: Disabling \"Profiles\"", fm.get("summary"));
        assertEquals("", fm.get("empty"));
        assertEquals(5, fm.lines().get("also"));
    }

    @Test
    void theFirstValueOfARepeatedKeyWins() {
        FrontMatter fm = FrontMatter.parse("---\ntags: a\ntags: b\n---\n");

        assertEquals("a", fm.get("tags"));
        assertEquals(2, fm.lines().get("tags"));
    }

    @Test
    void lineEndingsOfAnyKindAreAccepted() {
        FrontMatter fm = FrontMatter.parse("---\r\ntitle: B\r\ntags: [devops, Java]\r\n---\r\nBody\r\n");

        assertEquals("B", fm.get("title"));
        assertEquals("devops, Java", fm.get("tags"));
        assertEquals(4, fm.bodyStart());
        assertTrue(fm.separated());
    }

    @Test
    void aMissingClosingLineIsReportedAndTheWholeFileIsHeader() {
        FrontMatter fm = FrontMatter.parse("---\ntitle: x\nstatus: draft\n\nBody that is not a body.\n");

        assertFalse(fm.separated());
        assertFalse(fm.legacy());
        assertTrue(fm.values().isEmpty(), "nothing is read: the block has no end");
        assertEquals(6, fm.bodyStart(), "the line count: there is no body");
    }

    @Test
    void yamlJbakeCantUseIsReportedWithItsLine() {
        FrontMatter unquoted = FrontMatter.parse("---\ntitle: Maven Tip: Disabling Profiles\ndate: 2026-10-21\n---\n");
        assertTrue(unquoted.error().startsWith("mapping values are not allowed here"), unquoted.error());
        assertEquals(2, unquoted.errorLine());
        assertTrue(unquoted.separated(), "the closing line was there; the problem is inside");

        FrontMatter nested = FrontMatter.parse("---\ntitle: T\nimage:\n  src: a.jpg\n---\n");
        assertEquals("image must be a value or a list of values, not nested keys.", nested.error());
        assertEquals(3, nested.errorLine());
        assertEquals("T", nested.get("title"), "what was read before the problem is kept");

        FrontMatter list = FrontMatter.parse("---\n- a\n- b\n---\n");
        assertEquals("The front matter must be key: value lines.", list.error());
        assertEquals(2, list.errorLine());

        assertNull(FrontMatter.parse("---\n---\nBody\n").error(), "an empty block is a header with no keys");
        assertEquals(2, FrontMatter.parse("---\n---\nBody\n").bodyStart());
    }

    @Test
    void theFormatBeforeT145IsStillRead() {
        FrontMatter fm = FrontMatter.parse("title=About\n date = 2026-10-21 \ntype=page\nstatus=published\n~~~~~~\n\nA page.\n");

        assertTrue(fm.legacy());
        assertTrue(fm.separated());
        assertEquals("About", fm.get("title"));
        assertEquals("2026-10-21", fm.get("date"), "values are stripped");
        assertEquals(2, fm.lines().get("date"));
        assertEquals(5, fm.bodyStart());

        FrontMatter unseparated = FrontMatter.parse("title=x\nstatus=draft\n\nBody that is not a body.\n");
        assertTrue(unseparated.legacy());
        assertFalse(unseparated.separated());
        assertTrue(unseparated.isDraft());

        FrontMatter none = FrontMatter.parse("Just a file.\n");
        assertTrue(none.legacy(), "no front matter at all reads as the old format with nothing in it");
        assertFalse(none.separated());
        assertTrue(none.values().isEmpty());
    }

    @Test
    void writesPlainValuesAndQuotesOnlyWhatYamlWouldMisread() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("title", "Maven Tip: Disabling Profiles when a Property Does Not Exist");
        values.put("date", "2026-10-28");
        values.put("type", "post");
        values.put("tags", List.of("boyd's law", "continuous delivery", "a, b", "[x]"));
        values.put("status", "draft");
        values.put("summary", "She said \"no\" and left.");
        values.put("subtitle", "...for those who don't know");
        values.put("empty", "");
        values.put("number", "2012");
        values.put("word", "No");
        values.put("stamp", "2026-10-28 10:00");
        values.put("hash", "C# and #tags");
        values.put("multi", "two\nlines");
        values.put("quote", "'quoted'");

        assertEquals("""
                ---
                title: "Maven Tip: Disabling Profiles when a Property Does Not Exist"
                date: 2026-10-28
                type: post
                tags: [boyd's law, continuous delivery, "a, b", "[x]"]
                status: draft
                summary: She said "no" and left.
                subtitle: ...for those who don't know
                empty: ""
                number: "2012"
                word: "No"
                stamp: "2026-10-28 10:00"
                hash: "C# and #tags"
                multi: two lines
                quote: "'quoted'"
                ---
                """, FrontMatter.yaml(values));
    }

    @Test
    void whatIsWrittenReadsBackAsTheSameText() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("title", "Book Review: The Phoenix Project");
        values.put("date", "2013-02-21");
        values.put("tags", List.of("book review", "devops"));
        values.put("summary", "I completed 'The Phoenix Project: A Novel' and wrote this.  Two spaces kept.");
        values.put("credit", "Me, Lana'i from Kaanapali");
        values.put("url", "https://flic.kr/p/rFRzzj");
        values.put("yes", "yes");

        FrontMatter fm = FrontMatter.parse(FrontMatter.yaml(values) + "Body\n");

        assertNull(fm.error());
        values.forEach((key, value) -> assertEquals(value instanceof List<?> items ? String.join(", ", items.stream().map(String::valueOf).toList())
                : value, fm.get(key), key));
    }

    @Test
    void translatesToTheFileJbakeReads() {
        String text = "---\ntitle: \"Maven Tip: Profiles\"\ndate: 2013-02-21\ntags: [a, b]\nsummary: >\n  folded\n  lines\n---\n\nBody with ---\nand ~~~~~~ in it.\n";
        FrontMatter fm = FrontMatter.parse(text);

        assertEquals("title=Maven Tip: Profiles\ndate=2013-02-21\ntags=a, b\nsummary=folded lines\n~~~~~~\n\nBody with ---\nand ~~~~~~ in it.\n",
                fm.jbake(text, "~~~~~~"), "the values as JBake takes them, the separator it looks for, the body untouched");
    }
}
