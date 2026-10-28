package com.mikemcgarr.jbakesite.review;

import com.mikemcgarr.jbakesite.internal.FrontMatter;
import com.mikemcgarr.jbakesite.internal.SiteLayout;
import com.mikemcgarr.jbakesite.internal.Tags;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The checks on a changed content file's source: E4 (the header), E1 (Markdown link syntax) and W4 (alt text) on
 * the body, W2 (a page's date didn't move), W3 (tag spelling), W7 (an existing post has no summary), and E5 for
 * the masthead the header names.
 */
final class MarkdownChecks {

    private static final Pattern CODE_SPAN = Pattern.compile("(`+)(.+?)\\1");
    private static final Pattern FENCE = Pattern.compile("^\\s{0,3}(```|~~~)");
    private static final Pattern URL_AFTER_BRACKET = Pattern.compile("\\[([^\\]]*)\\]\\s*((?:https?://|www\\.)\\S*)");
    private static final Pattern SPACE_BEFORE_PAREN = Pattern.compile("\\[([^\\]]*)\\]\\s+\\(([^)]*)\\)");
    private static final Pattern EMPTY_URL = Pattern.compile("\\[([^\\]]*)\\]\\(\\s*\\)");
    private static final Pattern PADDED_TEXT = Pattern.compile("\\[(\\s+[^\\]]*|[^\\]]*\\s+)\\]\\(");
    private static final Pattern REF_LINK = Pattern.compile("\\[([^\\]]+)\\]\\[([^\\]]*)\\]");
    private static final Pattern REF_DEF = Pattern.compile("^\\s{0,3}\\[([^\\]]+)\\]:\\s*\\S");
    private static final Pattern FOOTNOTE_REF = Pattern.compile("\\[\\^([^\\]]+)\\](?!:)");
    private static final Pattern FOOTNOTE_DEF = Pattern.compile("^\\[\\^([^\\]]+)\\]:");
    private static final Pattern IMG_TAG = Pattern.compile("<img\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern RAW_CODE = Pattern.compile("(?is)<(pre|code)\\b[^>]*>.*?</\\1\\s*>");
    private static final Pattern RAW_CODE_OPEN = Pattern.compile("(?i)<(pre|code)\\b[^>]*>");
    private static final Pattern ALT_ATTR = Pattern.compile("\\balt\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+))", Pattern.CASE_INSENSITIVE);
    private static final Pattern MD_IMAGE = Pattern.compile("!\\[([^\\]]*)\\]\\(");
    private static final Pattern SRC_ATTR = Pattern.compile("\\bsrc\\s*=\\s*[\"']([^\"']*)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern FILE_NAME = Pattern.compile("(?i).*\\.(jpe?g|png|gif|webp|svg)$|^[\\w-]+$");

    private final SiteLayout site;
    private final Changes changes;
    private final ImageChecks images;
    private final Findings findings;
    private final Clock clock;
    private final boolean all;

    MarkdownChecks(SiteLayout site, Changes changes, ImageChecks images, Findings findings, Clock clock, boolean all) {
        this.site = site;
        this.changes = changes;
        this.images = images;
        this.findings = findings;
        this.clock = clock;
        this.all = all;
    }

    /** Checks a content file; returns its header, or null when the page can't be expected in the bake. */
    FrontMatter check(Change c) throws IOException, InterruptedException {
        String file = site.display(c.path());
        byte[] bytes = Files.readAllBytes(c.path());
        String text = new String(bytes, StandardCharsets.UTF_8);
        if (text.indexOf('\r') >= 0) {
            findings.add(true, "E4", file, 1 + (int) text.substring(0, text.indexOf('\r')).chars().filter(ch -> ch == '\n').count(),
                    "The file has a CR line ending. Content is LF only (AGENTS.md Rule 3; CI fails on this).",
                    "Convert the line endings: perl -pi -e 's/\\r\\n?/\\n/g' " + file);
        }
        FrontMatter fm = FrontMatter.parse(text);
        if (!fm.separated()) {
            findings.add(true, "E4", file, 1, "No " + FrontMatter.SEPARATOR + " line ends the header, so JBake won't render this file.",
                    "Put " + FrontMatter.SEPARATOR + " on its own line after the key=value lines.");
            return null;
        }
        boolean ok = true;
        for (String key : List.of("title", "date", "type", "status")) {
            if (fm.get(key).isEmpty()) {
                findings.add(true, "E4", file, 1, "The header has no " + key + "=.", "Add " + key + "= above the " + FrontMatter.SEPARATOR + " line.");
                ok = false;
            }
        }
        LocalDate today = LocalDate.now(clock);
        if (!fm.get("date").isEmpty()) {
            try {
                LocalDate.parse(fm.get("date"));
            } catch (DateTimeParseException e) {
                findings.add(true, "E4", file, fm.lines().get("date"), "date=" + fm.get("date") + " isn't a date of the form YYYY-MM-DD.",
                        "date=" + today);
                ok = false;
            }
        }
        if (!fm.get("type").isEmpty() && !List.of("post", "page").contains(fm.get("type"))) {
            findings.add(true, "E4", file, fm.lines().get("type"), "type=" + fm.get("type") + " isn't post or page.", "type=post");
            ok = false;
        }
        if (!fm.get("status").isEmpty() && !List.of("draft", "published").contains(fm.get("status"))) {
            findings.add(true, "E4", file, fm.lines().get("status"), "status=" + fm.get("status") + " isn't draft or published.",
                    "status=draft");
            ok = false;
        }
        boolean post = fm.get("type").equals("post");
        if (post && fm.get("summary").isEmpty()) {
            if (c.isNew()) {
                findings.add(true, "E4", file, fm.lines().getOrDefault("summary", 1),
                        "A new post needs a summary=: it's the teaser on the home page, in the feed and on share cards.",
                        "summary=One sentence that says what the post is about.");
            } else if (!all && fm.get("status").equals("published")) {
                findings.add(false, "W7", file, fm.lines().getOrDefault("summary", 1),
                        "This post has no summary=. The home page, the feed and share cards use it. Adding one is your call.",
                        null);
            }
        }
        if (c.status() == 'M' && fm.get("type").equals("page") && !all) {
            FrontMatter before = FrontMatter.parse(changes.atMergeBase(c.path()));
            if (!before.get("date").isEmpty() && before.get("date").equals(fm.get("date"))) {
                findings.add(false, "W2", file, fm.lines().get("date"), "This page changed but date=" + fm.get("date")
                        + " did not. The sitemap reports it as lastmod. Today is " + today + ".",
                        "date=" + today);
            }
        }
        checkTags(c, fm);
        checkMasthead(c, fm);
        checkBody(file, text, fm);
        return ok ? fm : null;
    }

    /** W3: a tag spelled or cased unlike an existing one makes a second tag page (T055); a new tag makes a page. */
    private void checkTags(Change c, FrontMatter fm) throws IOException, InterruptedException {
        if (fm.get("tags").isEmpty()) {
            return;
        }
        String file = site.display(c.path());
        Map<String, TreeSet<String>> spellings = new HashMap<>();
        Set<String> before = new HashSet<>();   // this file's tags at the merge base: those aren't new
        if (!all && (c.status() == 'M' || c.status() == 'R')) {
            before.addAll(Tags.split(FrontMatter.parse(changes.atMergeBase(c.removed())).get("tags")));
        }
        for (Path dir : List.of(site.blog(), site.drafts())) {
            if (!Files.isDirectory(dir)) {
                continue;
            }
            try (Stream<Path> files = Files.list(dir)) {
                for (Path f : files.filter(p -> p.toString().endsWith(".md") && !p.equals(c.path())).toList()) {
                    for (String t : Tags.split(FrontMatter.parse(Files.readString(f, StandardCharsets.UTF_8)).get("tags"))) {
                        spellings.computeIfAbsent(t.toLowerCase(Locale.ROOT), k -> new TreeSet<>()).add(t);
                    }
                }
            }
        }
        for (String tag : Tags.split(fm.get("tags"))) {
            TreeSet<String> existing = spellings.get(tag.toLowerCase(Locale.ROOT));
            if (existing == null) {
                if (!all && !before.contains(tag)) {
                    findings.add(false, "W3", file, fm.lines().get("tags"), "'" + tag + "' is a new tag, so publishing makes"
                            + " /tags/" + tag.replace(' ', '-') + ".html. Fine if intended; check the Topics page for a close match.", null);
                }
            } else if (!existing.contains(tag)) {
                findings.add(false, "W3", file, fm.lines().get("tags"), "The tag '" + tag + "' is already used as "
                        + existing + ". A different spelling or capitalization makes a separate tag page.",
                        "Use " + existing.first());
            }
        }
    }

    /** E5 for a masthead named in the header: it exists, and has its 960 and 1440 variants (T071). */
    private void checkMasthead(Change c, FrontMatter fm) {
        String name = fm.get("masthead");
        if (name.isEmpty() || name.startsWith("http")) {
            return;
        }
        Path image = name.startsWith("/") ? site.assets().resolve(name.substring(1)) : site.mastheads().resolve(name);
        if (!Files.isRegularFile(image)) {
            findings.add(true, "E5", site.display(c.path()), fm.lines().get("masthead"), "masthead=" + name + " isn't in "
                    + site.display(image.getParent()) + ".", "Add the image there, then run " + ImageChecks.variantsCommand("<image>"));
            return;
        }
        images.checkVariants(image, site.display(c.path()), fm.lines().get("masthead"));
    }

    /** E1 and W4 on the body, line by line, outside fenced code, code spans and HTML comments. */
    private void checkBody(String file, String text, FrontMatter fm) {
        String[] lines = text.split("\n", -1);
        Set<String> refDefs = new HashSet<>();
        Set<String> footnoteDefs = new HashSet<>();
        for (String line : lines) {
            Matcher m = REF_DEF.matcher(line);
            if (m.find()) {
                refDefs.add(m.group(1).toLowerCase(Locale.ROOT));
            }
            m = FOOTNOTE_DEF.matcher(line);
            if (m.find()) {
                footnoteDefs.add(m.group(1));
            }
        }
        boolean inFence = false;
        boolean inComment = false;
        String inRawCode = null;   // "pre" or "code" while inside a raw HTML code block that spans lines
        Set<String> suppressNext = Set.of();
        for (int i = fm.bodyStart(); i < lines.length; i++) {
            int n = i + 1;
            String raw = lines[i];
            if (FENCE.matcher(raw).find()) {
                inFence = !inFence;
                continue;
            }
            if (inFence) {
                continue;
            }
            Set<String> suppression = Suppressions.in(raw);
            if (suppression != null) {
                suppressNext = suppression;
                continue;
            }
            String line = raw;
            if (inRawCode != null) {
                int end = line.toLowerCase(Locale.ROOT).indexOf("</" + inRawCode);
                if (end < 0) {
                    continue;
                }
                line = line.substring(end);
                inRawCode = null;
            }
            if (inComment) {
                int end = line.indexOf("-->");
                if (end < 0) {
                    continue;
                }
                line = line.substring(end + 3);
                inComment = false;
            }
            line = line.replaceAll("<!--.*?-->", "");
            int start = line.indexOf("<!--");
            if (start >= 0) {
                line = line.substring(0, start);
                inComment = true;
            }
            line = RAW_CODE.matcher(line).replaceAll(" ");
            Matcher open = RAW_CODE_OPEN.matcher(line);
            if (open.find()) {
                inRawCode = open.group(1).toLowerCase(Locale.ROOT);
                line = line.substring(0, open.start());
            }
            if (line.isBlank()) {
                continue;
            }
            Set<String> ignore = suppressNext;
            suppressNext = Set.of();
            String code = CODE_SPAN.matcher(line).replaceAll(m -> " ".repeat(m.group().length()));
            String plain = code.replace("\\[", "  ").replace("\\]", "  ");

            checkLinkSyntax(ignore, file, n, plain, refDefs, footnoteDefs);
            checkAltText(ignore, file, n, line, code);
        }
    }

    /** E1: the ways a Markdown link fails to form and renders as literal brackets. */
    private void checkLinkSyntax(Set<String> ignore, String file, int n, String plain, Set<String> refDefs, Set<String> footnoteDefs) {
        Matcher m = URL_AFTER_BRACKET.matcher(plain);
        while (m.find()) {
            String url = m.group(2).replaceAll("[).,;:!?]+$", "");
            findings.unlessIgnored(ignore, true, "E1", file, n, "Link text \"[" + m.group(1) + "]\" is followed by \"" + Report.shorten(url)
                    + "\" with no \"(\": it renders as literal brackets.", "[" + m.group(1) + "](" + url + ")");
        }
        m = SPACE_BEFORE_PAREN.matcher(plain);
        while (m.find()) {
            findings.unlessIgnored(ignore, true, "E1", file, n, "A space between \"[" + m.group(1) + "]\" and \"(\": it renders as literal"
                    + " brackets, not a link.", "[" + m.group(1) + "](" + m.group(2) + ")");
        }
        m = EMPTY_URL.matcher(plain);
        while (m.find()) {
            findings.unlessIgnored(ignore, true, "E1", file, n, "The link \"[" + m.group(1) + "]()\" has no URL.", null);
        }
        m = PADDED_TEXT.matcher(plain);
        while (m.find()) {
            findings.unlessIgnored(ignore, true, "E1", file, n, "The link text \"[" + m.group(1) + "]\" starts or ends with a space, which"
                    + " shows in the link.", "[" + m.group(1).strip() + "](…)");
        }
        m = REF_LINK.matcher(plain);
        while (m.find()) {
            String ref = (m.group(2).isEmpty() ? m.group(1) : m.group(2)).toLowerCase(Locale.ROOT);
            if (!refDefs.contains(ref)) {
                findings.unlessIgnored(ignore, true, "E1", file, n, "The reference link \"" + m.group() + "\" has no \"[" + ref
                        + "]: URL\" definition, so it renders as literal text.", null);
            }
        }
        m = FOOTNOTE_REF.matcher(plain);
        while (m.find()) {
            if (!footnoteDefs.contains(m.group(1))) {
                findings.unlessIgnored(ignore, true, "E1", file, n, "The footnote \"[^" + m.group(1) + "]\" has no \"[^" + m.group(1)
                        + "]: text\" definition, so it renders as literal text.", null);
            }
        }
        int depth = 0;
        boolean unbalanced = false;
        for (char ch : plain.toCharArray()) {
            if (ch == '[') {
                depth++;
            } else if (ch == ']' && --depth < 0) {
                unbalanced = true;
                break;
            }
        }
        if (unbalanced || depth != 0) {
            findings.unlessIgnored(ignore, true, "E1", file, n, "Unmatched \"" + (unbalanced ? "]" : "[") + "\" on this line (outside code)."
                    + " A bracket that should show literally is written \\[ or \\].", null);
        }
    }

    /** W4: an image without alt text, or with its file name as the alt text. */
    private void checkAltText(Set<String> ignore, String file, int n, String line, String code) {
        Matcher m = IMG_TAG.matcher(line);
        while (m.find()) {
            Matcher alt = ALT_ATTR.matcher(m.group());
            if (!alt.find()) {
                findings.unlessIgnored(ignore, false, "W4", file, n, "This <img> has no alt text. Say what the image shows, or alt=\"\" if"
                        + " it's decorative.", null);
            } else {
                String value = alt.group(1) != null ? alt.group(1) : alt.group(2) != null ? alt.group(2) : alt.group(3);
                Matcher src = SRC_ATTR.matcher(m.group());
                String name = src.find() ? src.group(1).replaceAll(".*/", "") : "";
                String stem = name.replaceAll("\\.[^.]*$", "");
                if (!value.isBlank() && (FILE_NAME.matcher(value.strip()).matches() && value.contains(".")
                        || value.strip().equalsIgnoreCase(name) || !stem.isEmpty() && value.strip().equalsIgnoreCase(stem))) {
                    findings.unlessIgnored(ignore, false, "W4", file, n, "alt=\"" + value + "\" is the image's file name. Say what the image shows.", null);
                }
            }
        }
        m = MD_IMAGE.matcher(code);
        while (m.find()) {
            if (m.group(1).isBlank()) {
                findings.unlessIgnored(ignore, false, "W4", file, n, "This image has empty alt text. Say what it shows: ![what it shows](…).", null);
            }
        }
    }
}
