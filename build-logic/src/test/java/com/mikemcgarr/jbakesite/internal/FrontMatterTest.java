package com.mikemcgarr.jbakesite.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The key=value header up to the separator, as the review and the tag tools read it. */
class FrontMatterTest {

    @Test
    void readsTheKeysTheirLinesAndWhereTheBodyStarts() {
        FrontMatter fm = FrontMatter.parse("title=About\n date = 2026-10-21 \ntype=page\nstatus=published\n~~~~~~\n\nA page.\n");

        assertEquals("About", fm.get("title"));
        assertEquals("2026-10-21", fm.get("date"), "values are stripped on the way out");
        assertEquals(" 2026-10-21", fm.values().get("date"), "and kept raw inside (the line is stripped, the value after = is not)");
        assertEquals(2, fm.lines().get("date"));
        assertEquals(5, fm.bodyStart(), "the first body line, 0-based");
        assertTrue(fm.separated());
        assertTrue(fm.isPublished());
        assertFalse(fm.isDraft());
        assertEquals("", fm.get("summary"), "an absent key reads as empty");
    }

    @Test
    void theFirstValueOfARepeatedKeyWins() {
        FrontMatter fm = FrontMatter.parse("tags=a\ntags=b\n~~~~~~\n");

        assertEquals("a", fm.get("tags"));
        assertEquals(1, fm.lines().get("tags"));
    }

    @Test
    void aMissingSeparatorIsReportedAndTheWholeFileIsHeader() {
        FrontMatter fm = FrontMatter.parse("title=x\nstatus=draft\n\nBody that is not a body.\n");

        assertFalse(fm.separated());
        assertTrue(fm.isDraft());
        assertEquals(5, fm.bodyStart(), "the line count: there is no body");
    }

    @Test
    void aLineWithoutAKeyIsIgnored() {
        FrontMatter fm = FrontMatter.parse("=nokey\njust words\ntitle=T\n~~~~~~\n");

        assertEquals(1, fm.values().size());
        assertEquals("T", fm.get("title"));
    }
}
