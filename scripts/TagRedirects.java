// Redirect stubs that keep the site's old tag page URLs working (T039, T055; Java since T139, before that
// scripts/tag-redirects.py). Run it from the repository root:
//
//   java scripts/TagRedirects.java                  write any missing or outdated stub (never deletes one)
//   java scripts/TagRedirects.java --check SITE     check a baked site (build/jbake or build/site), exit 1 on a problem
//
// Tag pages used to be published under the tag exactly as written, spaces included (tags/acceptance test.html),
// and a few tags existed in two spellings (DevOps and devops, Groovy and groovy). With `tag.sanitize=true` JBake
// turns each space into a hyphen (tags/acceptance-test.html), and the capitalized spellings were lowercased in
// the posts. Every old URL in docs/baseline/urls.txt that no longer has a real tag page gets a small HTML page at
// the old path that sends readers (and search engines) on to the new one: a meta refresh, rel=canonical, robots
// noindex, and a visible link.
//
// Where the stubs go:
//   src/jbake/assets/tags/          most stubs; JBake copies them as they are.
//   src/case-redirects/tags/        stubs whose name differs from a real tag page only by letter case
//                                   (tags/DevOps.html -> tags/devops.html). On a case-insensitive filesystem
//                                   (macOS) the two names are one file, and JBake copies assets after rendering,
//                                   so the stub would replace the real page. The bake task (build.gradle) copies
//                                   these only when the output folder is case-sensitive, as on Linux, where CI
//                                   builds and GitHub Pages serves.
//
// The old tag URLs kept are:
//   - every tag URL in docs/baseline/urls.txt (the live site before the revival), and
//   - the hyphenated page of every baseline tag with a space (tags/acceptance-test.html). Those were published
//     by T039 on 2026-10-09 and aren't in the baseline, which only changes at a milestone checkpoint. Each is
//     derived from its baseline twin, so the list needs no upkeep.
//
// The new name of an old tag is worked out from the posts: spaces become hyphens, as JBake does, and if no
// published post uses that exact tag any more, a tag that differs only by case is used. A tag that was merged
// into another or renamed on purpose is listed in RENAMES (old -> new, as written in the posts), and all its old
// URLs (spaced and hyphenated) point to the new tag's page. An old tag with no match at all is an error: decide
// what it should point to (add it to RENAMES) before going on.
//
// --check also makes sure each real tag page shows its tag as written in the posts, spaces included
// (templates/tag-names.ftl, T057): JBake keeps only the hyphenated form, so a tag with a hyphen of its own
// (apt-get) must be listed in that template, or it would be shown as "apt get".
//
// The 59 committed stubs are published URLs and committed bytes: write mode reproduces every one of them byte
// for byte (`git status` is clean after a run on a clean tree), which is why the marker line below still names
// the Python script that first wrote them, and why the percent-encoding is hand-rolled to match FreeMarker's
// ?url_path (head-meta.ftl's canonicals) rather than taken from java.net.URLEncoder, which writes "+" for a
// space and encodes "~".
//
// A single-file Java program (JDK 21 or newer), like scripts/NewPost.java: nothing to install.

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class TagRedirects {

    static final String SITE = "https://www.mikemcgarr.com";
    static final Path BASELINE = Path.of("docs/baseline/urls.txt");
    static final Path CONTENT = Path.of("src/jbake/content");
    static final String STUB_DIR = "src/jbake/assets";          // most stubs
    static final String CASE_STUB_DIR = "src/case-redirects";   // stubs that differ from a real page only by case
    // Kept as the Python script wrote it, so the committed stubs regenerate byte for byte (see the header).
    static final String MARKER = "<!-- tag redirect stub (scripts/tag-redirects.py) -->";

    // Tags merged into another tag or renamed, old -> new, both as written in the posts' tags= lines. Every old URL
    // of the old tag (baseline and hyphenated) becomes a stub to the new tag's page. Never remove an entry: its
    // stubs are published URLs. Owner-approved 2026-10-09 (docs/reports/content-2026-10-09.md, section H).
    static final Map<String, String> RENAMES = new LinkedHashMap<>();
    static {
        RENAMES.put("presentation", "presentations");                       // H1 merge
        RENAMES.put("talks", "presentations");                              // H2 merge
        RENAMES.put("excella", "Excella Consulting");                       // H3 merge
        RENAMES.put("vi", "vim");                                           // H4 merge
        RENAMES.put("lamdbas", "lambdas");                                  // H5 typo
        RENAMES.put("google calender", "google calendar");                  // H6 typo
        RENAMES.put("software craftsmenship", "software craftsmanship");    // H7 typo
        RENAMES.put("paired programming", "pair programming");              // H8 the usual term
        RENAMES.put("build", "build automation");                           // H9 merge
    }

    static final Pattern REFRESH = Pattern.compile("<meta http-equiv=\"refresh\" content=\"0; url=([^\"]+)\">");
    static final Pattern CANONICAL = Pattern.compile("<link rel=\"canonical\" href=\"([^\"]+)\">");
    static final Pattern OG_TITLE = Pattern.compile("<meta property=\"og:title\" content=\"Tag: ([^\"]*)\">");

    public static void main(String[] args) throws IOException {
        if (!Files.isRegularFile(BASELINE) || !Files.isDirectory(CONTENT)) {
            System.err.println("TagRedirects: run it from the repository root (" + BASELINE + " and " + CONTENT + " must exist).");
            System.exit(2);
        }
        if (args.length == 2 && args[0].equals("--check")) {
            check(Path.of(args[1]));
        } else if (args.length == 0) {
            write();
        } else {
            System.err.println("usage: java scripts/TagRedirects.java                  write any missing or outdated stub");
            System.err.println("       java scripts/TagRedirects.java --check <site>   check a baked site (build/jbake or build/site)");
            System.exit(1);
        }
    }

    /** Encodes a site path the way head-meta.ftl does (FreeMarker ?url_path), so canonicals match. */
    static String urlPath(String path) {
        String safe = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789/-_.!~*'()";
        StringBuilder sb = new StringBuilder();
        for (byte b : path.getBytes(StandardCharsets.UTF_8)) {
            int c = b & 0xFF;
            if (c < 128 && safe.indexOf(c) >= 0) {
                sb.append((char) c);
            } else {
                sb.append('%').append(String.format("%02X", c));
            }
        }
        return sb.toString();
    }

    /** Decodes %XX escapes (UTF-8); "+" stays "+", as in urllib's unquote. */
    static String unquote(String s) {
        if (s.indexOf('%') < 0) {
            return s;
        }
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '%' && i + 2 < s.length() + 0 && isHex(s.charAt(i + 1)) && isHex(s.charAt(i + 2))) {
                out.write(Integer.parseInt(s.substring(i + 1, i + 3), 16));
                i += 2;
            } else {
                out.writeBytes(String.valueOf(c).getBytes(StandardCharsets.UTF_8));
            }
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    static boolean isHex(char c) {
        return Character.digit(c, 16) >= 0;
    }

    /** Python's html.escape(s, quote=True). */
    static String htmlEscape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#x27;");
    }

    /** Enough of html.unescape for the entities our own stubs and tag pages contain. */
    static String htmlUnescape(String s) {
        if (s.indexOf('&') < 0) {
            return s;
        }
        Matcher m = Pattern.compile("&(#x[0-9A-Fa-f]+|#[0-9]+|amp|lt|gt|quot|apos);").matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String e = m.group(1);
            String r = switch (e) {
                case "amp" -> "&";
                case "lt" -> "<";
                case "gt" -> ">";
                case "quot" -> "\"";
                case "apos" -> "'";
                default -> new String(Character.toChars(e.startsWith("#x")
                        ? Integer.parseInt(e.substring(2), 16) : Integer.parseInt(e.substring(1))));
            };
            m.appendReplacement(sb, Matcher.quoteReplacement(r));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** Python's repr() of a str (for messages that quoted a tag name), or None. */
    static String pyRepr(String s) {
        if (s == null) {
            return "None";
        }
        boolean useDouble = s.contains("'") && !s.contains("\"");
        String body = s.replace("\\", "\\\\");
        return useDouble ? "\"" + body + "\"" : "'" + body.replace("'", "\\'") + "'";
    }

    static String pyRepr(List<String> list) {
        List<String> parts = new ArrayList<>();
        for (String s : list) {
            parts.add(pyRepr(s));
        }
        return "[" + String.join(", ", parts) + "]";
    }

    /** Reads a file as UTF-8, replacing malformed bytes (errors="replace"). */
    static String readLenient(Path p) throws IOException {
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }

    static List<Path> filesUnder(Path root) throws IOException {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> s = Files.walk(root)) {
            return s.filter(Files::isRegularFile).sorted().toList();
        }
    }

    /** Every tag of every published page, as written in its front matter (spaces kept). */
    static Set<String> writtenTags() throws IOException {
        Set<String> tags = new LinkedHashSet<>();
        for (Path f : filesUnder(CONTENT)) {
            String text = readLenient(f).replace("\r\n", "\n").replace("\r", "\n");
            int sep = text.indexOf("~~~~~~");
            String head = sep < 0 ? text : text.substring(0, sep);
            Map<String, String> meta = new HashMap<>();
            for (String line : head.split("\n")) {
                int eq = line.indexOf('=');
                if (eq >= 0) {
                    meta.put(line.substring(0, eq), line.substring(eq + 1));
                }
            }
            if (!meta.getOrDefault("status", "").strip().equals("published")) {
                continue;
            }
            for (String tag : meta.getOrDefault("tags", "").split(",")) {
                if (!tag.strip().isEmpty()) {
                    tags.add(tag.strip());
                }
            }
        }
        return tags;
    }

    /** Tag names as JBake publishes them: every tag of every published page, spaces made hyphens. */
    static Set<String> publishedTags() throws IOException {
        Set<String> out = new LinkedHashSet<>();
        for (String t : writtenTags()) {
            out.add(t.replace(' ', '-'));
        }
        return out;
    }

    /** The tag URLs in docs/baseline/urls.txt (the live site before the revival), in file order. */
    static List<String> baselineTagUrls() throws IOException {
        List<String> urls = new ArrayList<>();
        for (String line : Files.readAllLines(BASELINE, StandardCharsets.UTF_8)) {
            if (line.startsWith("tags/")) {
                urls.add(line.strip());
            }
        }
        return urls;
    }

    /** The hyphenated twin of every baseline tag URL with a space: published since T039 (2026-10-09), not in the baseline. */
    static List<String> hyphenatedTagUrls() throws IOException {
        List<String> baseline = baselineTagUrls();
        Set<String> twins = new TreeSet<>();
        for (String u : baseline) {
            if (u.contains(" ")) {
                twins.add(u.replace(' ', '-'));
            }
        }
        twins.removeAll(baseline);
        return new ArrayList<>(twins);
    }

    record OldUrl(String url, String source) {}

    /** Every tag URL the site has published and must keep, with where it comes from. */
    static List<OldUrl> oldTagUrls() throws IOException {
        List<OldUrl> out = new ArrayList<>();
        for (String u : baselineTagUrls()) {
            out.add(new OldUrl(u, "baseline"));
        }
        for (String u : hyphenatedTagUrls()) {
            out.add(new OldUrl(u, "since T039"));
        }
        return out;
    }

    /** old path -> new path for every old tag URL that no longer has a real tag page. */
    static Map<String, String> redirects() throws IOException {
        Set<String> tags = publishedTags();
        Map<String, List<String>> byFold = new HashMap<>();
        for (String t : tags) {
            byFold.computeIfAbsent(t.toLowerCase(Locale.ROOT), k -> new ArrayList<>()).add(t);
        }
        Map<String, String> renames = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : RENAMES.entrySet()) {
            renames.put(e.getKey().replace(' ', '-'), e.getValue().replace(' ', '-'));
        }
        Map<String, String> pairs = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        for (Map.Entry<String, String> e : renames.entrySet()) {
            if (tags.contains(e.getKey())) {
                errors.add("RENAMES: '" + e.getKey() + "' is still used by a published post (its stub would replace the real page)");
            }
            if (!tags.contains(e.getValue())) {
                errors.add("RENAMES: '" + e.getKey() + "' -> '" + e.getValue() + "', but no published post uses '" + e.getValue() + "'");
            }
        }
        for (OldUrl o : oldTagUrls()) {
            String old = o.url();
            String name = old.substring("tags/".length(), old.length() - ".html".length());
            String nu = name.replace(' ', '-');
            if (renames.containsKey(nu)) {
                nu = renames.get(nu);
            } else if (!tags.contains(nu)) {
                List<String> matches = byFold.getOrDefault(nu.toLowerCase(Locale.ROOT), List.of());
                if (matches.size() != 1) {
                    errors.add(old + ": no published tag " + (matches.isEmpty() ? "matches" : "is unambiguous") + ": " + pyRepr(matches));
                    continue;
                }
                nu = matches.get(0);
            }
            if (!nu.equals(name)) {
                pairs.put(old, "tags/" + nu + ".html");
            }
        }
        if (!errors.isEmpty()) {
            System.err.println("ERROR: can't work out where these old tag URLs should point:\n  " + String.join("\n  ", errors));
            System.exit(1);
        }
        return pairs;
    }

    static String stub(String old, String nu) {
        String target = nu.substring(nu.lastIndexOf('/') + 1);
        String href = htmlEscape(urlPath(target).replace("'", "%27"));
        String canonical = htmlEscape(SITE + "/" + urlPath(nu));
        String title = htmlEscape("Tag: " + target.substring(0, target.length() - ".html".length()));
        return "<!DOCTYPE html>\n"
                + MARKER + "\n"
                + "<html lang=\"en\">\n"
                + "<head>\n"
                + "<meta charset=\"utf-8\">\n"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n"
                + "<title>" + title + " (moved)</title>\n"
                + "<meta name=\"robots\" content=\"noindex\">\n"
                + "<link rel=\"canonical\" href=\"" + canonical + "\">\n"
                + "<meta http-equiv=\"refresh\" content=\"0; url=" + href + "\">\n"
                + "</head>\n"
                + "<body>\n"
                + "<p>This page has moved to <a href=\"" + href + "\">" + canonical + "</a>.</p>\n"
                + "</body>\n"
                + "</html>\n";
    }

    static String stubPath(String old, String nu) {
        return (old.equalsIgnoreCase(nu) ? CASE_STUB_DIR : STUB_DIR) + "/" + old;
    }

    static void write() throws IOException {
        Map<String, String> pairs = redirects();
        int written = 0;
        Set<String> expected = new LinkedHashSet<>();
        for (Map.Entry<String, String> e : pairs.entrySet()) {
            String path = stubPath(e.getKey(), e.getValue());
            String text = stub(e.getKey(), e.getValue());
            expected.add(path);
            Path p = Path.of(path);
            if (Files.exists(p) && Files.readString(p, StandardCharsets.UTF_8).equals(text)) {
                continue;
            }
            Files.createDirectories(p.getParent());
            Files.writeString(p, text, StandardCharsets.UTF_8);
            written++;
            System.out.println("wrote " + path + " -> " + e.getValue());
        }
        for (String d : List.of(STUB_DIR, CASE_STUB_DIR)) {
            for (Path p : filesUnder(Path.of(d, "tags"))) {
                if (readLenient(p).contains(MARKER) && !expected.contains(p.toString())) {
                    System.out.println("NOTE: " + p + " is a stub this script no longer makes. It's a published URL: keep it unless approved.");
                }
            }
        }
        System.out.println(pairs.size() + " tag redirect stubs (" + written + " written, " + (pairs.size() - written) + " already up to date)");
    }

    /** True if site/rel exists with exactly this spelling (Files.exists ignores case on macOS). */
    static boolean existsExactly(Path site, String rel) throws IOException {
        Path p = site.resolve(rel);
        Path dir = p.getParent();
        if (dir == null || !Files.isDirectory(dir)) {
            return false;
        }
        try (Stream<Path> s = Files.list(dir)) {
            return s.anyMatch(x -> x.getFileName().toString().equals(p.getFileName().toString()));
        }
    }

    /** urljoin(base, ref) for the path-only URLs in a stub: an absolute ref is kept, a relative one joins base's folder. */
    static String resolve(String base, String ref) {
        if (ref.matches("^[A-Za-z][A-Za-z0-9+.-]*:.*") || ref.startsWith("/")) {
            return ref;
        }
        String dir = base.substring(0, base.lastIndexOf('/') + 1);
        List<String> segments = new ArrayList<>();
        for (String seg : (dir + ref).split("/", -1)) {
            if (seg.equals("..")) {
                if (!segments.isEmpty()) {
                    segments.remove(segments.size() - 1);
                }
            } else if (!seg.equals(".")) {
                segments.add(seg);
            }
        }
        return String.join("/", segments);
    }

    static List<String> tagFiles(Path site) throws IOException {
        try (Stream<Path> s = Files.list(site.resolve("tags"))) {
            return s.map(p -> p.getFileName().toString()).sorted().toList();
        }
    }

    static void check(Path site) throws IOException {
        if (!Files.isDirectory(site.resolve("tags"))) {
            System.err.println("TagRedirects: " + site + " has no tags/ folder. Bake first (./gradlew bake).");
            System.exit(2);
        }
        boolean caseInsensitive = Files.exists(site.resolve("INDEX.HTML"));
        List<String> problems = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        int real = 0, stubs = 0;
        Map<String, String> pairs = redirects();
        List<OldUrl> oldUrls = oldTagUrls();
        for (OldUrl o : oldUrls) {
            String old = o.url();
            if (!existsExactly(site, old)) {
                if (caseInsensitive && pairs.containsKey(old) && old.equalsIgnoreCase(pairs.get(old))) {
                    skipped.add(old);
                } else {
                    problems.add(old + ": missing from the build");
                }
                continue;
            }
            String text = Files.readString(site.resolve(old), StandardCharsets.UTF_8);
            Matcher refresh = REFRESH.matcher(text);
            if (!refresh.find()) {
                real++;
                if (old.contains(" ")) {
                    problems.add(old + ": a real tag page with a space in its URL (tag.sanitize is off?)");
                }
                if (pairs.containsKey(old)) {
                    problems.add(old + ": a real page, but expected a stub pointing to " + pairs.get(old));
                }
                continue;
            }
            stubs++;
            String target = unquote(resolve(old, htmlUnescape(refresh.group(1))));
            Matcher canonical = CANONICAL.matcher(text);
            String expected = pairs.get(old);
            if (!target.equals(expected)) {
                problems.add(old + ": redirects to " + target + ", expected " + (expected == null ? "None" : expected));
            } else if (!existsExactly(site, target)) {
                problems.add(old + ": its target " + target + " is missing");
            } else if (Files.readString(site.resolve(target), StandardCharsets.UTF_8).contains("http-equiv=\"refresh\"")) {
                problems.add(old + ": its target " + target + " is itself a redirect");
            }
            if (target.contains(" ")) {
                problems.add(old + ": its target " + target + " has a space");
            }
            if (!canonical.find() || !htmlUnescape(canonical.group(1)).equals(SITE + "/" + urlPath(target))) {
                problems.add(old + ": rel=canonical is not " + SITE + "/" + urlPath(target));
            }
            if (!text.contains("<meta name=\"robots\" content=\"noindex\">")) {
                problems.add(old + ": no robots noindex");
            }
        }
        for (String f : tagFiles(site)) {
            if (f.contains(" ") && !pairs.containsKey("tags/" + f)) {
                problems.add("tags/" + f + ": a tag page with a space in its URL that isn't a known stub");
            }
        }
        problems.addAll(shownNames(site));
        Map<String, Integer> bySource = new HashMap<>();
        for (OldUrl o : oldUrls) {
            bySource.merge(o.source(), 1, Integer::sum);
        }
        System.out.println(oldUrls.size() + " old tag URLs (" + bySource.getOrDefault("baseline", 0) + " baseline, "
                + bySource.getOrDefault("since T039", 0) + " hyphenated since T039): " + real + " real tag pages, "
                + stubs + " redirect stubs, " + skipped.size() + " not checkable here");
        for (String old : skipped) {
            System.out.println("  ~ " + old + " (case-insensitive filesystem: same file as " + pairs.get(old) + "; the stub is added only on Linux)");
        }
        for (String p : problems) {
            System.out.println("  ! " + p);
        }
        if (!problems.isEmpty()) {
            System.err.println("FAIL: " + problems.size() + " problems in " + site);
            System.exit(1);
        }
        System.out.println("OK: every old tag URL in " + site + " is a tag page or a stub whose target is a tag page");
    }

    /** Problems with the tag name a real tag page shows (its og:title, "Tag: <name>"), compared with the posts. */
    static List<String> shownNames(Path site) throws IOException {
        Map<String, String> written = new HashMap<>();
        for (String t : writtenTags()) {
            written.put(t.replace(' ', '-'), t);
        }
        List<String> problems = new ArrayList<>();
        int checked = 0;
        for (String f : tagFiles(site)) {
            if (!f.endsWith(".html") || f.equals("index.html")) {
                continue;
            }
            String stem = f.substring(0, f.length() - ".html".length());
            if (!written.containsKey(stem)) {
                continue;
            }
            String text = Files.readString(site.resolve("tags").resolve(f), StandardCharsets.UTF_8);
            if (text.contains("http-equiv=\"refresh\"")) {
                continue;
            }
            Matcher title = OG_TITLE.matcher(text);
            String shown = title.find() ? htmlUnescape(title.group(1)) : null;
            String want = written.get(stem);
            checked++;
            if (!want.equals(shown)) {
                problems.add("tags/" + f + ": shows the tag as " + pyRepr(shown) + ", the posts write " + pyRepr(want)
                        + " (list it in src/jbake/templates/tag-names.ftl)");
            }
        }
        System.out.println(checked + " tag pages checked for the tag name they show: " + (checked - problems.size()) + " as written in the posts");
        return problems;
    }
}
