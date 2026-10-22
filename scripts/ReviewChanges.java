// Reviews the pages a change touched (task T117 of docs/05-REVIEW-FLOW.md). Run it through Gradle, after a bake:
//
//   ./gradlew reviewChanges [-Pbase=main] [-Pall] [-Pstrict]
//
// or directly, with build/jbake already baked:  java scripts/ReviewChanges.java [--base=main] [--all] [--strict]
//
// It lists every file that differs from the merge base with `base` (committed or not, plus untracked files under
// src/jbake), maps each to the page it renders, and checks the source and the rendered page:
//
//   Errors (exit 1):   E1 Markdown link syntax     E2 brackets left in the rendered text
//                      E3 local links and images    E4 front matter          E5 image rules
//   Warnings (listed): W2 a page's date didn't move W3 tag spelling          W4 alt text
//                      W6 a published URL changes   W7 an existing post has no summary
//
// External links (W1) are task T119. Spelling (W5) is deferred (T120). A finding that is right for the site but
// wrong for one line is suppressed by an HTML comment on the line before it:
//   <!-- review: ignore E2, the brackets are intended -->
// It never changes a file. Exit codes: 0 nothing to fix, 1 errors (or warnings with --strict), 2 couldn't run.
//
// A single-file Java program (JDK 21 or newer), like scripts/NewPost.java.

import java.awt.Dimension;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

public class ReviewChanges {

    static final Path CONTENT = Path.of("src/jbake/content");
    static final Path ASSETS = Path.of("src/jbake/assets");
    static final Path TEMPLATES = Path.of("src/jbake/templates");
    static final Path CASE_REDIRECTS = Path.of("src/case-redirects");
    static final Path PUBLISHED_URLS = Path.of("docs/baseline/urls.txt");
    static final String SEPARATOR = "~~~~~~";
    static final long MAX_IMAGE_BYTES = 400 * 1024;
    static final int MAX_IMAGE_WIDTH = 1920;
    static final int[] MASTHEAD_WIDTHS = {960, 1440};

    static Path site = Path.of("build/jbake");
    static boolean all;
    static boolean strict;
    static String base = "main";

    /** One thing to fix or look at. {@code file} is repository-relative; {@code line} is 0 when unknown. */
    record Finding(boolean error, String id, String file, int line, String message, String fix) {}

    /** A changed file: status A (added), M (modified), D (deleted), R (renamed, with oldPath) or U (untracked). */
    record Change(char status, Path path, Path oldPath) {
        boolean isNew() { return status == 'A' || status == 'U'; }
        boolean exists() { return status != 'D'; }
    }

    /** The header of a content file: key=value lines before the separator, each with its line number. */
    record FrontMatter(Map<String, String> values, Map<String, Integer> lines, int bodyStart, boolean separated) {
        String get(String key) { return values.getOrDefault(key, "").strip(); }
        boolean isDraft() { return get("status").equals("draft"); }
    }

    static final List<Finding> findings = new ArrayList<>();
    static int suppressed;
    static final Set<String> reviewedPages = new LinkedHashSet<>();
    static final Map<Path, Set<String>> idCache = new HashMap<>();
    static Set<String> publishedUrls = Set.of();

    public static void main(String[] args) throws Exception {
        Map<String, String> opts = parse(args);
        base = opts.getOrDefault("base", "main").strip();
        all = flag(opts, "all");
        strict = flag(opts, "strict");
        if (opts.containsKey("site")) {
            site = Path.of(opts.get("site"));
        }
        if (flag(opts, "online")) {
            System.out.println("Note: external links aren't checked yet (task T119); --online is accepted and ignored.");
        }
        if (!Files.isDirectory(CONTENT)) {
            stop("Run this from the repository root (no " + CONTENT + " here).");
        }
        if (!Files.isRegularFile(site.resolve("index.html"))) {
            stop("No baked site in " + site + ". Run ./gradlew bake first (./gradlew reviewChanges does).");
        }
        if (Files.exists(PUBLISHED_URLS)) {
            publishedUrls = new HashSet<>(Files.readAllLines(PUBLISHED_URLS));
        }

        List<Change> changes = all ? everything() : changedFiles();
        if (changes.isEmpty()) {
            System.out.println("Nothing to review: no changes under src/jbake or src/case-redirects since the merge base"
                    + " with " + base + ".");
            System.exit(0);
        }

        Map<String, Change> pages = new LinkedHashMap<>();   // site-relative page -> the change that made it
        boolean templatesChanged = false;
        List<Path> images = new ArrayList<>();
        for (Change c : changes) {
            if (c.path.startsWith(CONTENT) && c.path.toString().endsWith(".md")) {
                if (!c.exists()) {
                    continue;
                }
                FrontMatter fm = checkMarkdown(c);
                if (fm != null) {
                    pages.put(pageFor(c.path, fm), c);
                }
            } else if (c.path.startsWith(TEMPLATES) && c.exists()) {
                templatesChanged = true;
            } else if (c.path.startsWith(ASSETS.resolve("img")) && c.exists()) {
                images.add(c.path);
            }
            if ((c.status == 'D' || c.status == 'R') && (c.status == 'R' ? c.oldPath : c.path).startsWith(ASSETS)) {
                for (String page : pagesReferencing(c.status == 'R' ? c.oldPath : c.path)) {
                    pages.putIfAbsent(page, null);   // still points at the old name: E3 says so
                }
            }
        }
        checkRemovals(changes);
        for (Path image : images) {
            checkImage(image);
            for (String page : pagesReferencing(image)) {
                pages.putIfAbsent(page, null);
            }
        }
        if (templatesChanged) {
            for (String page : samplePages()) {
                pages.putIfAbsent(page, null);
            }
        }
        for (Map.Entry<String, Change> e : pages.entrySet()) {
            checkPage(e.getKey(), e.getValue());
        }
        report(changes.size());
    }

    // ---------------------------------------------------------------- what changed

    static List<Change> changedFiles() throws IOException, InterruptedException {
        if (git("rev-parse", "--is-inside-work-tree").isEmpty()) {
            stop("Not a git checkout, so there's no change to review. Use --all to review every page.");
        }
        if (git("rev-parse", "--verify", "--quiet", base + "^{commit}").isEmpty()) {
            stop("The base '" + base + "' isn't a commit here. Pass --base=<ref> (for example --base=origin/main),"
                    + " or fetch it first.");
        }
        String mergeBase = git("merge-base", base, "HEAD");
        if (mergeBase.isEmpty()) {
            stop("No merge base between " + base + " and HEAD (a shallow clone? fetch with more history).");
        }
        List<Change> changes = new ArrayList<>();
        for (String line : git("diff", "--name-status", "-M", mergeBase, "--",
                "src/jbake", "src/case-redirects").split("\n")) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split("\t");
            char status = parts[0].charAt(0);
            if (status == 'R' && parts.length >= 3) {
                changes.add(new Change('R', Path.of(parts[2]), Path.of(parts[1])));
            } else if (parts.length >= 2) {
                changes.add(new Change(status == 'C' ? 'A' : status, Path.of(parts[1]), null));
            }
        }
        for (String line : git("ls-files", "--others", "--exclude-standard", "--",
                "src/jbake", "src/case-redirects").split("\n")) {
            if (!line.isBlank()) {
                changes.add(new Change('U', Path.of(line), null));
            }
        }
        return changes;
    }

    /** --all: every content file, as if modified (so nothing is "new" and the diff-only warnings stay quiet). */
    static List<Change> everything() throws IOException {
        List<Change> changes = new ArrayList<>();
        try (Stream<Path> files = Files.walk(CONTENT)) {
            for (Path p : files.filter(f -> f.toString().endsWith(".md")).sorted().toList()) {
                changes.add(new Change('M', p, null));
            }
        }
        return changes;
    }

    /** The site-relative page a content file renders to: blog/x.md -> blog/x.html, a draft -> blog/drafts/x-draft.html. */
    static String pageFor(Path source, FrontMatter fm) {
        String rel = CONTENT.relativize(source).toString().replace('\\', '/');
        String stem = rel.substring(0, rel.length() - ".md".length());
        return stem + (fm.isDraft() ? "-draft.html" : ".html");
    }

    /** For a template change: the four pages the Definition of Done names, plus the newest and the oldest post. */
    static List<String> samplePages() throws IOException {
        List<String> pages = new ArrayList<>(List.of("index.html", "archive.html", "tags/index.html", "about.html"));
        TreeMap<String, String> byDate = new TreeMap<>();
        try (Stream<Path> files = Files.list(CONTENT.resolve("blog"))) {
            for (Path p : files.filter(f -> f.toString().endsWith(".md")).toList()) {
                FrontMatter fm = frontMatter(Files.readString(p, StandardCharsets.UTF_8));
                if (fm.get("status").equals("published") && fm.get("type").equals("post")) {
                    byDate.put(fm.get("date") + " " + p.getFileName(), pageFor(p, fm));
                }
            }
        }
        if (!byDate.isEmpty()) {
            pages.add(byDate.lastEntry().getValue());
            pages.add(byDate.firstEntry().getValue());
        }
        return pages;
    }

    /** Pages in the baked site whose HTML mentions this asset (by its site path, with or without the leading /). */
    static List<String> pagesReferencing(Path asset) throws IOException {
        String sitePath = ASSETS.relativize(asset).toString().replace('\\', '/');
        List<String> pages = new ArrayList<>();
        try (Stream<Path> files = Files.walk(site)) {
            for (Path p : files.filter(f -> f.toString().endsWith(".html")).sorted().toList()) {
                String html = Files.readString(p, StandardCharsets.UTF_8);
                if (html.contains("\"/" + sitePath) || html.contains("\"" + sitePath) || html.contains("/" + sitePath + "\"")) {
                    pages.add(site.relativize(p).toString().replace('\\', '/'));
                }
            }
        }
        return pages;
    }

    // ---------------------------------------------------------------- the source (E1, E4, E5, W2, W3, W4, W7)

    static FrontMatter checkMarkdown(Change c) throws IOException, InterruptedException {
        String file = c.path.toString();
        byte[] bytes = Files.readAllBytes(c.path);
        String text = new String(bytes, StandardCharsets.UTF_8);
        if (text.indexOf('\r') >= 0) {
            add(true, "E4", file, 1 + (int) text.substring(0, text.indexOf('\r')).chars().filter(ch -> ch == '\n').count(),
                    "The file has a CR line ending. Content is LF only (AGENTS.md Rule 3; CI fails on this).",
                    "Convert the line endings: perl -pi -e 's/\\r\\n?/\\n/g' " + file);
        }
        FrontMatter fm = frontMatter(text);
        if (!fm.separated) {
            add(true, "E4", file, 1, "No " + SEPARATOR + " line ends the header, so JBake won't render this file.",
                    "Put " + SEPARATOR + " on its own line after the key=value lines.");
            return null;
        }
        boolean ok = true;
        for (String key : List.of("title", "date", "type", "status")) {
            if (fm.get(key).isEmpty()) {
                add(true, "E4", file, 1, "The header has no " + key + "=.", "Add " + key + "= above the " + SEPARATOR + " line.");
                ok = false;
            }
        }
        if (!fm.get("date").isEmpty()) {
            try {
                LocalDate.parse(fm.get("date"));
            } catch (DateTimeParseException e) {
                add(true, "E4", file, fm.lines.get("date"), "date=" + fm.get("date") + " isn't a date of the form YYYY-MM-DD.",
                        "date=" + LocalDate.now());
                ok = false;
            }
        }
        if (!fm.get("type").isEmpty() && !List.of("post", "page").contains(fm.get("type"))) {
            add(true, "E4", file, fm.lines.get("type"), "type=" + fm.get("type") + " isn't post or page.", "type=post");
            ok = false;
        }
        if (!fm.get("status").isEmpty() && !List.of("draft", "published").contains(fm.get("status"))) {
            add(true, "E4", file, fm.lines.get("status"), "status=" + fm.get("status") + " isn't draft or published.",
                    "status=draft");
            ok = false;
        }
        boolean post = fm.get("type").equals("post");
        if (post && fm.get("summary").isEmpty()) {
            if (c.isNew()) {
                add(true, "E4", file, fm.lines.getOrDefault("summary", 1),
                        "A new post needs a summary=: it's the teaser on the home page, in the feed and on share cards.",
                        "summary=One sentence that says what the post is about.");
            } else if (!all && fm.get("status").equals("published")) {
                add(false, "W7", file, fm.lines.getOrDefault("summary", 1),
                        "This post has no summary=. The home page, the feed and share cards use it. Adding one is your call.",
                        null);
            }
        }
        if (c.status == 'M' && fm.get("type").equals("page") && !all) {
            FrontMatter before = frontMatter(git("show", git("merge-base", base, "HEAD") + ":" + c.path.toString().replace('\\', '/')));
            if (!before.get("date").isEmpty() && before.get("date").equals(fm.get("date"))) {
                add(false, "W2", file, fm.lines.get("date"), "This page changed but date=" + fm.get("date")
                        + " did not. The sitemap reports it as lastmod. Today is " + LocalDate.now() + ".",
                        "date=" + LocalDate.now());
            }
        }
        checkTags(c, fm);
        checkMasthead(c, fm);
        checkBody(file, text, fm);
        return ok ? fm : null;
    }

    static FrontMatter frontMatter(String text) {
        Map<String, String> values = new LinkedHashMap<>();
        Map<String, Integer> lines = new HashMap<>();
        String[] all = text.split("\n", -1);
        for (int i = 0; i < all.length; i++) {
            String line = all[i].strip();
            if (line.startsWith(SEPARATOR)) {
                return new FrontMatter(values, lines, i + 1, true);
            }
            int eq = line.indexOf('=');
            if (eq > 0) {
                String key = line.substring(0, eq).strip();
                values.putIfAbsent(key, line.substring(eq + 1));
                lines.putIfAbsent(key, i + 1);
            }
        }
        return new FrontMatter(values, lines, all.length, false);
    }

    /** W3: a tag spelled or cased unlike an existing one makes a second tag page (T055); a new tag makes a page. */
    static void checkTags(Change c, FrontMatter fm) throws IOException, InterruptedException {
        if (fm.get("tags").isEmpty()) {
            return;
        }
        Map<String, TreeSet<String>> spellings = new HashMap<>();
        Set<String> before = new HashSet<>();   // this file's tags at the merge base: those aren't new
        if (!all && (c.status == 'M' || c.status == 'R')) {
            Path old = c.status == 'R' ? c.oldPath : c.path;
            for (String t : frontMatter(git("show", git("merge-base", base, "HEAD") + ":" + old.toString().replace('\\', '/'))).get("tags").split(",")) {
                if (!t.isBlank()) {
                    before.add(t.strip());
                }
            }
        }
        for (Path dir : List.of(CONTENT.resolve("blog"), CONTENT.resolve("blog/drafts"))) {
            try (Stream<Path> files = Files.list(dir)) {
                for (Path f : files.filter(p -> p.toString().endsWith(".md") && !p.equals(c.path)).toList()) {
                    String tags = frontMatter(Files.readString(f, StandardCharsets.UTF_8)).get("tags");
                    for (String t : tags.split(",")) {
                        if (!t.isBlank()) {
                            spellings.computeIfAbsent(t.strip().toLowerCase(), k -> new TreeSet<>()).add(t.strip());
                        }
                    }
                }
            }
        }
        for (String t : fm.get("tags").split(",")) {
            String tag = t.strip();
            if (tag.isEmpty()) {
                continue;
            }
            TreeSet<String> existing = spellings.get(tag.toLowerCase());
            if (existing == null) {
                if (!all && !before.contains(tag)) {
                    add(false, "W3", c.path.toString(), fm.lines.get("tags"), "'" + tag + "' is a new tag, so publishing makes"
                            + " /tags/" + tag.replace(' ', '-') + ".html. Fine if intended; check the Topics page for a close match.", null);
                }
            } else if (!existing.contains(tag)) {
                add(false, "W3", c.path.toString(), fm.lines.get("tags"), "The tag '" + tag + "' is already used as "
                        + existing + ". A different spelling or capitalization makes a separate tag page.",
                        "Use " + existing.first());
            }
        }
    }

    /** E5 for a masthead named in the header: it exists, and has its 960 and 1440 variants (T071). */
    static void checkMasthead(Change c, FrontMatter fm) {
        String name = fm.get("masthead");
        if (name.isEmpty() || name.startsWith("http")) {
            return;
        }
        Path image = name.startsWith("/") ? ASSETS.resolve(name.substring(1)) : ASSETS.resolve("img/masthead").resolve(name);
        if (!Files.isRegularFile(image)) {
            add(true, "E5", c.path.toString(), fm.lines.get("masthead"), "masthead=" + name + " isn't in " + image.getParent() + ".",
                    "Add the image there, then run scripts/masthead-variants.py");
            return;
        }
        checkVariants(image, c.path.toString(), fm.lines.get("masthead"));
    }

    static void checkVariants(Path image, String file, int line) {
        String s = image.toString();
        int dot = s.lastIndexOf('.');
        List<String> missing = new ArrayList<>();
        for (int w : MASTHEAD_WIDTHS) {
            Path variant = Path.of(s.substring(0, dot) + "-" + w + s.substring(dot));
            if (!Files.isRegularFile(variant)) {
                missing.add(variant.getFileName().toString());
            }
        }
        if (!missing.isEmpty()) {
            add(true, "E5", file, line, "The masthead " + image.getFileName() + " has no " + String.join(" or ", missing)
                    + " variant for phones and tablets (CI fails without them).", "python3 scripts/masthead-variants.py");
        }
    }

    static final Pattern CODE_SPAN = Pattern.compile("(`+)(.+?)\\1");
    static final Pattern FENCE = Pattern.compile("^\\s{0,3}(```|~~~)");
    static final Pattern SUPPRESS = Pattern.compile("<!--\\s*review:\\s*ignore\\s+([A-Z]\\d+(?:\\s*,\\s*[A-Z]\\d+)*)[^>]*-->");
    static final Pattern URL_AFTER_BRACKET = Pattern.compile("\\[([^\\]]*)\\]\\s*((?:https?://|www\\.)\\S*)");
    static final Pattern SPACE_BEFORE_PAREN = Pattern.compile("\\[([^\\]]*)\\]\\s+\\(([^)]*)\\)");
    static final Pattern EMPTY_URL = Pattern.compile("\\[([^\\]]*)\\]\\(\\s*\\)");
    static final Pattern PADDED_TEXT = Pattern.compile("\\[(\\s+[^\\]]*|[^\\]]*\\s+)\\]\\(");
    static final Pattern REF_LINK = Pattern.compile("\\[([^\\]]+)\\]\\[([^\\]]*)\\]");
    static final Pattern REF_DEF = Pattern.compile("^\\s{0,3}\\[([^\\]]+)\\]:\\s*\\S");
    static final Pattern FOOTNOTE_REF = Pattern.compile("\\[\\^([^\\]]+)\\](?!:)");
    static final Pattern FOOTNOTE_DEF = Pattern.compile("^\\[\\^([^\\]]+)\\]:");
    static final Pattern IMG_TAG = Pattern.compile("<img\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    static final Pattern RAW_CODE = Pattern.compile("(?is)<(pre|code)\\b[^>]*>.*?</\\1\\s*>");
    static final Pattern RAW_CODE_OPEN = Pattern.compile("(?i)<(pre|code)\\b[^>]*>");
    static final Pattern ALT_ATTR = Pattern.compile("\\balt\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+))", Pattern.CASE_INSENSITIVE);
    static final Pattern MD_IMAGE = Pattern.compile("!\\[([^\\]]*)\\]\\(");
    static final Pattern SRC_ATTR = Pattern.compile("\\bsrc\\s*=\\s*[\"']([^\"']*)[\"']", Pattern.CASE_INSENSITIVE);
    static final Pattern FILE_NAME = Pattern.compile("(?i).*\\.(jpe?g|png|gif|webp|svg)$|^[\\w-]+$");

    /** E1 and W4 on the body, line by line, outside fenced code, code spans and HTML comments. */
    static void checkBody(String file, String text, FrontMatter fm) {
        String[] lines = text.split("\n", -1);
        Set<String> refDefs = new HashSet<>();
        Set<String> footnoteDefs = new HashSet<>();
        for (String line : lines) {
            Matcher m = REF_DEF.matcher(line);
            if (m.find()) {
                refDefs.add(m.group(1).toLowerCase());
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
        for (int i = fm.bodyStart; i < lines.length; i++) {
            int n = i + 1;
            String raw = lines[i];
            if (FENCE.matcher(raw).find()) {
                inFence = !inFence;
                continue;
            }
            if (inFence) {
                continue;
            }
            Matcher sup = SUPPRESS.matcher(raw);
            if (sup.find()) {
                suppressNext = Set.of(sup.group(1).split("\\s*,\\s*"));
                continue;
            }
            String line = raw;
            if (inRawCode != null) {
                int end = line.toLowerCase().indexOf("</" + inRawCode);
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
                inRawCode = open.group(1).toLowerCase();
                line = line.substring(0, open.start());
            }
            if (line.isBlank()) {
                continue;
            }
            Set<String> ignore = suppressNext;
            suppressNext = Set.of();
            String code = CODE_SPAN.matcher(line).replaceAll(m -> " ".repeat(m.group().length()));
            String plain = code.replace("\\[", "  ").replace("\\]", "  ");

            Matcher m = URL_AFTER_BRACKET.matcher(plain);
            while (m.find()) {
                String url = m.group(2).replaceAll("[).,;:!?]+$", "");
                finding(ignore, true, "E1", file, n, "Link text \"[" + m.group(1) + "]\" is followed by \"" + shorten(url)
                        + "\" with no \"(\": it renders as literal brackets.", "[" + m.group(1) + "](" + url + ")");
            }
            m = SPACE_BEFORE_PAREN.matcher(plain);
            while (m.find()) {
                finding(ignore, true, "E1", file, n, "A space between \"[" + m.group(1) + "]\" and \"(\": it renders as literal"
                        + " brackets, not a link.", "[" + m.group(1) + "](" + m.group(2) + ")");
            }
            m = EMPTY_URL.matcher(plain);
            while (m.find()) {
                finding(ignore, true, "E1", file, n, "The link \"[" + m.group(1) + "]()\" has no URL.", null);
            }
            m = PADDED_TEXT.matcher(plain);
            while (m.find()) {
                finding(ignore, true, "E1", file, n, "The link text \"[" + m.group(1) + "]\" starts or ends with a space, which"
                        + " shows in the link.", "[" + m.group(1).strip() + "](…)");
            }
            m = REF_LINK.matcher(plain);
            while (m.find()) {
                String ref = (m.group(2).isEmpty() ? m.group(1) : m.group(2)).toLowerCase();
                if (!refDefs.contains(ref)) {
                    finding(ignore, true, "E1", file, n, "The reference link \"" + m.group() + "\" has no \"[" + ref
                            + "]: URL\" definition, so it renders as literal text.", null);
                }
            }
            m = FOOTNOTE_REF.matcher(plain);
            while (m.find()) {
                if (!footnoteDefs.contains(m.group(1))) {
                    finding(ignore, true, "E1", file, n, "The footnote \"[^" + m.group(1) + "]\" has no \"[^" + m.group(1)
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
                finding(ignore, true, "E1", file, n, "Unmatched \"" + (unbalanced ? "]" : "[") + "\" on this line (outside code)."
                        + " A bracket that should show literally is written \\[ or \\].", null);
            }

            m = IMG_TAG.matcher(line);
            while (m.find()) {
                Matcher alt = ALT_ATTR.matcher(m.group());
                if (!alt.find()) {
                    finding(ignore, false, "W4", file, n, "This <img> has no alt text. Say what the image shows, or alt=\"\" if"
                            + " it's decorative.", null);
                } else {
                    String value = alt.group(1) != null ? alt.group(1) : alt.group(2) != null ? alt.group(2) : alt.group(3);
                    Matcher src = SRC_ATTR.matcher(m.group());
                    String name = src.find() ? src.group(1).replaceAll(".*/", "") : "";
                    String stem = name.replaceAll("\\.[^.]*$", "");
                    if (!value.isBlank() && (FILE_NAME.matcher(value.strip()).matches() && value.contains(".")
                            || value.strip().equalsIgnoreCase(name) || !stem.isEmpty() && value.strip().equalsIgnoreCase(stem))) {
                        finding(ignore, false, "W4", file, n, "alt=\"" + value + "\" is the image's file name. Say what the image shows.", null);
                    }
                }
            }
            m = MD_IMAGE.matcher(code);
            while (m.find()) {
                if (m.group(1).isBlank()) {
                    finding(ignore, false, "W4", file, n, "This image has empty alt text. Say what it shows: ![what it shows](…).", null);
                }
            }
        }
    }

    // ---------------------------------------------------------------- the image files (E5)

    static void checkImage(Path image) {
        String file = image.toString();
        try {
            long size = Files.size(image);
            if (size > MAX_IMAGE_BYTES) {
                add(true, "E5", file, 0, "The image is " + (size / 1024) + " KB; the target is 400 KB or less (AGENTS.md Rule 4).",
                        "Re-encode it (JPEG quality 80, sRGB, metadata stripped) before adding it.");
            }
            Dimension d = dimensions(image);
            if (d != null && d.width > MAX_IMAGE_WIDTH) {
                add(true, "E5", file, 0, "The image is " + d.width + " px wide; the limit is " + MAX_IMAGE_WIDTH + " px (AGENTS.md Rule 4).",
                        "Resize it to at most " + MAX_IMAGE_WIDTH + " px wide.");
            }
        } catch (IOException e) {
            add(true, "E5", file, 0, "Couldn't read the image: " + e.getMessage(), null);
        }
        if (image.startsWith(ASSETS.resolve("img/masthead")) && !image.getFileName().toString().matches(".*-(960|1440)\\.\\w+")) {
            checkVariants(image, file, 0);
        }
    }

    static Dimension dimensions(Path image) throws IOException {
        try (ImageInputStream in = ImageIO.createImageInputStream(image.toFile())) {
            if (in == null) {
                return null;
            }
            var readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                return null;   // SVG, WebP and other formats the JDK can't read: size is still checked
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(in);
                return new Dimension(reader.getWidth(0), reader.getHeight(0));
            } finally {
                reader.dispose();
            }
        }
    }

    // ---------------------------------------------------------------- removed or renamed URLs (W6)

    static void checkRemovals(List<Change> changes) throws IOException, InterruptedException {
        for (Change c : changes) {
            if (c.status != 'D' && c.status != 'R') {
                continue;
            }
            Path old = c.status == 'R' ? c.oldPath : c.path;
            String what;
            if (old.startsWith(CONTENT)) {
                FrontMatter before = frontMatter(git("show", git("merge-base", base, "HEAD") + ":" + old.toString().replace('\\', '/')));
                if (before.isDraft()) {
                    continue;   // a draft was never a URL
                }
                what = "/" + CONTENT.relativize(old).toString().replace('\\', '/').replaceAll("\\.md$", ".html");
            } else if (old.startsWith(ASSETS)) {
                what = "/" + ASSETS.relativize(old).toString().replace('\\', '/');
            } else {
                continue;
            }
            add(false, "W6", old.toString(), 0, (c.status == 'R' ? "Renaming" : "Deleting") + " this file removes the published URL "
                    + what + ". Published URLs are permanent (AGENTS.md Rule 1): this needs the owner's approval, a redirect stub at"
                    + " the old path, and a line in docs/baseline/expected-removals.txt. CI's check-urls.sh fails otherwise.", null);
        }
    }

    // ---------------------------------------------------------------- the rendered page (E2, E3)

    static final Pattern ATTR = Pattern.compile("([a-zA-Z_:][-\\w:.]*)\\s*(?:=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s\"'>]+)))?");
    static final Set<String> SKIP_TEXT_IN = Set.of("code", "pre", "script", "style", "textarea", "svg");
    static final Set<String> VOID = Set.of("area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta",
            "param", "source", "track", "wbr");
    static final Pattern LEFTOVER = Pattern.compile("\\]\\(|\\]\\s*(?:https?://|www\\.)|\\[[^\\]\\n]*\\]");

    record Tag(String name, Map<String, String> attrs, boolean closing, boolean selfClosing) {}

    static void checkPage(String page, Change source) throws IOException {
        Path file = site.resolve(page);
        if (!Files.isRegularFile(file)) {
            add(true, source != null ? "E4" : "E3", source != null ? source.path.toString() : page, 1,
                    "JBake produced no page " + page + " in " + site + ". Check the header, then ./gradlew bake.", null);
            return;
        }
        reviewedPages.add(page);
        String html = Files.readString(file, StandardCharsets.UTF_8);
        String[] sourceLines = source == null ? new String[0]
                : Files.readString(source.path, StandardCharsets.UTF_8).split("\n", -1);
        String sourceFile = source == null ? null : source.path.toString();
        // Links in a draft are resolved from where the post will live when published (blog/), not from blog/drafts/
        String pageDir = page.contains("/") ? page.substring(0, page.lastIndexOf('/') + 1) : "";
        if (pageDir.equals("blog/drafts/")) {
            pageDir = "blog/";
        }
        Set<String> ids = idsOf(file);

        List<String> openTags = new ArrayList<>();
        int skipDepth = -1;           // depth at which a code/pre/script/style element opened
        int commentsDepth = -1;       // depth at which <details class="archived-comments"> opened
        Set<String> pendingIgnore = null;
        Set<String> ignore = Set.of();
        int ignoreDepth = -1;
        int pos = 0;
        int line = 1;
        int len = html.length();
        while (pos < len) {
            int lt = html.indexOf('<', pos);
            if (lt < 0) {
                lt = len;
            }
            if (lt > pos) {
                String text = decode(html.substring(pos, lt));
                if (skipDepth < 0 && commentsDepth < 0 && !text.isBlank()) {
                    Matcher m = LEFTOVER.matcher(text);
                    while (m.find()) {
                        String excerpt = m.group();
                        if (sourceLine(sourceLines, excerpt.replace("[", "\\[").replace("]", "\\]")) > 0) {
                            continue;   // written as \[…\] in the Markdown: the brackets are meant to show
                        }
                        int at = sourceLine(sourceLines, excerpt);
                        finding(ignore, true, "E2", at > 0 ? sourceFile : page, at > 0 ? at : line,
                                "The page shows \"" + shorten(text.strip()) + "\" with the brackets as text. A Markdown link"
                                        + " didn't form (see E1), or a bracket needs escaping as \\[." + (at > 0 ? "" : " (On " + page + ".)"), null);
                    }
                }
                line += count(html, pos, lt, '\n');
                pos = lt;
                continue;
            }
            if (html.startsWith("<!--", pos)) {
                int end = html.indexOf("-->", pos);
                end = end < 0 ? len : end + 3;
                Matcher sup = SUPPRESS.matcher(html.substring(pos, end));
                if (sup.find()) {
                    pendingIgnore = Set.of(sup.group(1).split("\\s*,\\s*"));
                }
                line += count(html, pos, end, '\n');
                pos = end;
                continue;
            }
            int gt = endOfTag(html, pos);
            String inside = html.substring(pos + 1, gt);
            Tag tag = parseTag(inside);
            if (tag == null) {
                line += count(html, pos, gt + 1, '\n');
                pos = gt + 1;
                continue;
            }
            if (tag.closing) {
                int depth = openTags.lastIndexOf(tag.name);
                if (depth >= 0) {
                    while (openTags.size() > depth) {
                        openTags.remove(openTags.size() - 1);
                    }
                    if (skipDepth >= openTags.size()) {
                        skipDepth = -1;
                    }
                    if (commentsDepth >= openTags.size()) {
                        commentsDepth = -1;
                    }
                    if (ignoreDepth >= openTags.size()) {
                        ignoreDepth = -1;
                        ignore = Set.of();
                    }
                }
            } else {
                boolean leaf = tag.selfClosing || VOID.contains(tag.name);
                if (pendingIgnore != null) {
                    ignore = pendingIgnore;
                    ignoreDepth = leaf ? -1 : openTags.size();
                    pendingIgnore = null;
                }
                if (skipDepth < 0 && commentsDepth < 0) {
                    checkLinks(tag, page, pageDir, ids, line, ignore, sourceLines, sourceFile);
                }
                if (!leaf) {
                    openTags.add(tag.name);
                    if (skipDepth < 0 && SKIP_TEXT_IN.contains(tag.name)) {
                        skipDepth = openTags.size() - 1;
                    }
                    if (commentsDepth < 0 && tag.name.equals("details")
                            && tag.attrs.getOrDefault("class", "").contains("archived-comments")) {
                        commentsDepth = openTags.size() - 1;
                    }
                    if ((tag.name.equals("script") || tag.name.equals("style"))) {
                        int close = html.toLowerCase().indexOf("</" + tag.name, gt);
                        close = close < 0 ? len : close;
                        line += count(html, pos, close, '\n');
                        pos = close;
                        continue;
                    }
                }
                if (leaf && ignoreDepth < 0 && pendingIgnore == null && !ignore.isEmpty() && ignoreDepth == -1) {
                    ignore = Set.of();   // a suppression aimed at a void tag (an <img>) is spent
                }
            }
            line += count(html, pos, gt + 1, '\n');
            pos = gt + 1;
        }
    }

    static final Set<String> LINK_ATTRS = Set.of("href", "src", "srcset", "poster", "data");

    static void checkLinks(Tag tag, String page, String pageDir, Set<String> ids, int line, Set<String> ignore,
            String[] sourceLines, String sourceFile) throws IOException {
        for (Map.Entry<String, String> a : tag.attrs.entrySet()) {
            if (!LINK_ATTRS.contains(a.getKey()) || a.getValue() == null) {
                continue;
            }
            List<String> urls = new ArrayList<>();
            if (a.getKey().equals("srcset")) {
                for (String part : a.getValue().split(",")) {
                    String u = part.strip().split("\\s+")[0];
                    if (!u.isEmpty()) {
                        urls.add(u);
                    }
                }
            } else {
                urls.add(a.getValue().strip());
            }
            for (String url : urls) {
                String problem = resolve(url, pageDir, ids, page);
                if (problem != null) {
                    int at = sourceLine(sourceLines, url.isEmpty() ? "]()" : url);
                    finding(ignore, true, "E3", at > 0 ? sourceFile : page, at > 0 ? at : line, "<" + tag.name + " " + a.getKey()
                            + "=\"" + shorten(url) + "\">: " + problem + (at > 0 ? "" : " (On " + page + ".)"), null);
                }
            }
        }
    }

    /** Returns null when the URL resolves (or isn't ours to check), else what's wrong. */
    static String resolve(String url, String pageDir, Set<String> pageIds, String page) throws IOException {
        if (url.isEmpty()) {
            return "the URL is empty.";
        }
        String lower = url.toLowerCase();
        if (lower.startsWith("mailto:") || lower.startsWith("tel:") || lower.startsWith("javascript:") || lower.startsWith("data:")) {
            return null;
        }
        String path = url;
        if (lower.startsWith("https://www.mikemcgarr.com/") || lower.startsWith("https://mikemcgarr.com/")
                || lower.startsWith("http://www.mikemcgarr.com/") || lower.startsWith("http://mikemcgarr.com/")) {
            path = "/" + url.substring(url.indexOf("mikemcgarr.com/") + "mikemcgarr.com/".length());
        } else if (lower.matches("^[a-z][a-z0-9+.-]*:.*") || url.startsWith("//")) {
            return null;   // another site (W1, task T119)
        }
        String fragment = null;
        int hash = path.indexOf('#');
        if (hash >= 0) {
            fragment = path.substring(hash + 1);
            path = path.substring(0, hash);
        }
        int q = path.indexOf('?');
        if (q >= 0) {
            path = path.substring(0, q);
        }
        path = java.net.URLDecoder.decode(path.replace("+", "%2B"), StandardCharsets.UTF_8);
        String sitePath;
        if (path.isEmpty()) {
            sitePath = page;
        } else {
            sitePath = path.startsWith("/") ? path.substring(1) : pageDir + path;
            sitePath = Path.of("/" + sitePath).normalize().toString().substring(1).replace('\\', '/');
            if (sitePath.isEmpty() || path.endsWith("/")) {
                sitePath = sitePath + (sitePath.isEmpty() ? "" : "/") + "index.html";
            }
        }
        if (page.endsWith("-draft.html") && page.equals(sitePath.replaceAll("\\.html$", "-draft.html"))) {
            return null;   // the draft's own canonical URL: where it will live when published (never checked here)
        }
        Path target = site.resolve(sitePath);
        boolean exists = Files.isRegularFile(target)
                || Files.isRegularFile(CASE_REDIRECTS.resolve(sitePath))
                || publishedUrls.contains(sitePath);
        // A bake never deletes: a file removed from src/jbake/assets keeps a stale copy in the site until a clean bake.
        // So anything that isn't rendered or generated must still have its source.
        boolean generated = sitePath.endsWith(".html") || sitePath.endsWith(".xml") || sitePath.startsWith("css/");
        boolean stale = exists && !generated && !Files.isRegularFile(ASSETS.resolve(sitePath))
                && !Files.isRegularFile(CASE_REDIRECTS.resolve(sitePath)) && !publishedUrls.contains(sitePath);
        if (stale) {
            return "nothing at /" + sitePath + " in src/jbake/assets any more (the copy in " + site + " is left over from an"
                    + " earlier bake).";
        }
        if (!exists && Files.isDirectory(target) && Files.isRegularFile(target.resolve("index.html"))) {
            exists = true;
            sitePath = sitePath + "/index.html";
        }
        if (!exists) {
            return "nothing at /" + sitePath + " in the baked site" + (page.startsWith("blog/drafts/")
                    ? " (resolved from blog/, where the post will live when published)." : ".");
        }
        if (fragment != null && !fragment.isEmpty() && sitePath.endsWith(".html")) {
            Set<String> ids = sitePath.equals(page) ? pageIds : idsOf(site.resolve(sitePath));
            if (!ids.contains(fragment)) {
                return "/" + sitePath + " has no id=\"" + fragment + "\" to jump to.";
            }
        }
        return null;
    }

    static final Pattern ID_ATTR = Pattern.compile("\\s(?:id|name)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')");

    static Set<String> idsOf(Path htmlFile) throws IOException {
        Set<String> cached = idCache.get(htmlFile);
        if (cached != null) {
            return cached;
        }
        Set<String> ids = new HashSet<>();
        if (Files.isRegularFile(htmlFile)) {
            Matcher m = ID_ATTR.matcher(Files.readString(htmlFile, StandardCharsets.UTF_8));
            while (m.find()) {
                ids.add(m.group(1) != null ? m.group(1) : m.group(2));
            }
        }
        idCache.put(htmlFile, ids);
        return ids;
    }

    static int endOfTag(String html, int lt) {
        int i = lt + 1;
        char quote = 0;
        while (i < html.length()) {
            char ch = html.charAt(i);
            if (quote != 0) {
                if (ch == quote) {
                    quote = 0;
                }
            } else if (ch == '"' || ch == '\'') {
                quote = ch;
            } else if (ch == '>') {
                return i;
            }
            i++;
        }
        return html.length() - 1;
    }

    static Tag parseTag(String inside) {
        if (inside.isEmpty() || inside.startsWith("!") || inside.startsWith("?")) {
            return null;
        }
        boolean closing = inside.startsWith("/");
        String body = closing ? inside.substring(1) : inside;
        boolean selfClosing = body.endsWith("/");
        if (selfClosing) {
            body = body.substring(0, body.length() - 1);
        }
        Matcher m = ATTR.matcher(body);
        if (!m.find() || m.start() != 0 && !body.substring(0, m.start()).isBlank()) {
            return null;
        }
        String name = m.group(1).toLowerCase();
        Map<String, String> attrs = new LinkedHashMap<>();
        while (m.find()) {
            String value = m.group(2) != null ? m.group(2) : m.group(3) != null ? m.group(3) : m.group(4);
            attrs.put(m.group(1).toLowerCase(), value == null ? null : decode(value));
        }
        return new Tag(name, attrs, closing, selfClosing);
    }

    static String decode(String s) {
        if (s.indexOf('&') < 0) {
            return s;
        }
        return s.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'")
                .replace("&nbsp;", " ").replaceAll("&#(\\d+);", "\u0001$1\u0002").replace("&amp;", "&")
                .replaceAll("\u0001(\\d+)\u0002", "")   // numeric entities don't matter for brackets or URLs
                .replace("\u0001", "").replace("\u0002", "");
    }

    static int count(String s, int from, int to, char ch) {
        int n = 0;
        for (int i = from; i < to && i < s.length(); i++) {
            if (s.charAt(i) == ch) {
                n++;
            }
        }
        return n;
    }

    /** The first source line that contains the excerpt, to point at the Markdown instead of the HTML; 0 if none. */
    static int sourceLine(String[] sourceLines, String excerpt) {
        if (sourceLines.length == 0 || excerpt.isBlank()) {
            return 0;
        }
        for (int i = 0; i < sourceLines.length; i++) {
            if (sourceLines[i].contains(excerpt)) {
                return i + 1;
            }
        }
        return 0;
    }

    // ---------------------------------------------------------------- findings and the report

    static void finding(Set<String> ignore, boolean error, String id, String file, int line, String message, String fix) {
        if (ignore.contains(id)) {
            suppressed++;
            return;
        }
        add(error, id, file, line, message, fix);
    }

    static void add(boolean error, String id, String file, int line, String message, String fix) {
        Finding f = new Finding(error, id, file.replace('\\', '/'), line, message, fix);
        if (!findings.contains(f)) {
            findings.add(f);
        }
    }

    static void report(int changedFiles) throws IOException {
        findings.sort(Comparator.comparing((Finding f) -> !f.error).thenComparing(Finding::file)
                .thenComparingInt(Finding::line).thenComparing(Finding::id));
        long errors = findings.stream().filter(Finding::error).count();
        long warnings = findings.size() - errors;
        StringBuilder out = new StringBuilder();
        for (Finding f : findings) {
            out.append(f.error ? "ERROR " : "WARN  ").append(f.id).append(' ').append(f.file);
            if (f.line > 0) {
                out.append(':').append(f.line);
            }
            out.append('\n').append(wrap(f.message)).append('\n');
            if (f.fix != null) {
                out.append("  Fix: ").append(f.fix).append('\n');
            }
            out.append('\n');
        }
        String pages = reviewedPages.isEmpty() ? "no pages" : reviewedPages.size() + (reviewedPages.size() == 1 ? " page (" : " pages (")
                + String.join(", ", reviewedPages.size() > 6 ? List.of(String.join(", ", new ArrayList<>(reviewedPages).subList(0, 6)), "…")
                        : reviewedPages) + ")";
        String summary = "Reviewed " + pages + (all ? "" : " from " + changedFiles + " changed file" + (changedFiles == 1 ? "" : "s"))
                + ". " + errors + " error" + (errors == 1 ? "" : "s") + ", " + warnings + " warning" + (warnings == 1 ? "" : "s")
                + (suppressed > 0 ? ", " + suppressed + " suppressed" : "") + ". External links not checked (T119).";
        out.append(summary).append('\n');
        System.out.print(out);

        if ("true".equals(System.getenv("GITHUB_ACTIONS"))) {
            for (Finding f : findings) {
                System.out.println("::" + (f.error ? "error" : "warning") + " file=" + f.file + (f.line > 0 ? ",line=" + f.line : "")
                        + ",title=" + f.id + "::" + escape(f.message + (f.fix != null ? " Fix: " + f.fix : "")));
            }
            String stepSummary = System.getenv("GITHUB_STEP_SUMMARY");
            if (stepSummary != null && !stepSummary.isEmpty()) {
                Files.writeString(Path.of(stepSummary), "### Review of the changed pages\n\n```\n" + out + "```\n",
                        StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
            }
        }
        System.exit(errors > 0 || (strict && warnings > 0) ? 1 : 0);
    }

    static String wrap(String message) {
        StringBuilder sb = new StringBuilder("  ");
        int col = 2;
        for (String word : message.split(" ")) {
            if (col + word.length() > 110 && col > 2) {
                sb.append("\n  ");
                col = 2;
            }
            sb.append(word).append(' ');
            col += word.length() + 1;
        }
        return sb.toString().stripTrailing();
    }

    static String shorten(String s) {
        s = s.replaceAll("\\s+", " ");
        return s.length() > 80 ? s.substring(0, 77) + "…" : s;
    }

    static String escape(String s) {
        return s.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A");
    }

    // ---------------------------------------------------------------- options and git

    static boolean flag(Map<String, String> opts, String name) {
        if (!opts.containsKey(name)) {
            return false;
        }
        return switch (opts.get(name).strip().toLowerCase()) {
            case "", "true" -> true;
            case "false" -> false;
            default -> {
                stop("--" + name + " takes no value (or true/false), not '" + opts.get(name) + "'.");
                yield false;
            }
        };
    }

    static Map<String, String> parse(String[] args) {
        Map<String, String> opts = new LinkedHashMap<>();
        for (String arg : args) {
            if (!arg.startsWith("--") || arg.length() == 2) {
                stop("Unexpected argument '" + arg + "'. Expected --name=value, --base=<ref>, --all, --strict or --site=<dir>.");
            }
            int eq = arg.indexOf('=');
            opts.put(eq < 0 ? arg.substring(2) : arg.substring(2, eq), eq < 0 ? "true" : arg.substring(eq + 1));
        }
        for (String name : opts.keySet()) {
            if (!List.of("base", "all", "strict", "online", "site").contains(name)) {
                stop("Unknown option --" + name + ". Expected --base=<ref>, --all, --strict or --site=<dir>.");
            }
        }
        return opts;
    }

    /** Runs git and returns its trimmed output, or "" if it exits non-zero. */
    static String git(String... args) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>(List.of("git"));
        cmd.addAll(List.of(args));
        ProcessBuilder pb = new ProcessBuilder(cmd).redirectInput(Redirect.from(
                new java.io.File(System.getProperty("os.name").startsWith("Windows") ? "NUL" : "/dev/null")));
        pb.redirectError(Redirect.DISCARD);
        try {
            Process p = pb.start();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
            return p.waitFor() == 0 ? out : "";
        } catch (IOException e) {
            throw new IOException("git isn't installed or isn't on the PATH.");
        }
    }

    /** A usage or environment problem: says so and exits 2 (the review didn't run). */
    static void stop(String message) {
        System.err.println("reviewChanges: " + message);
        System.exit(2);
    }
}
