package com.mikemcgarr.jbakesite.writing;

import com.mikemcgarr.jbakesite.FileTrees;
import com.mikemcgarr.jbakesite.internal.FrontMatter;
import com.mikemcgarr.jbakesite.internal.SiteLayout;
import com.mikemcgarr.jbakesite.internal.Tags;
import com.mikemcgarr.jbakesite.internal.Text;
import com.mikemcgarr.jbakesite.internal.ToolFailure;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
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

/**
 * Redirect stubs that keep the site's old tag page URLs working (T039, T055; {@code scripts/tag-redirects.py},
 * then {@code scripts/TagRedirects.java} since T139, in the plugin since T127). Two modes: {@link #write()} writes
 * any missing or outdated stub and never deletes one ({@code tagRedirects}); {@link #check(Path)} checks a baked
 * site ({@code checkTagRedirects}, which CI runs on {@code build/site}).
 *
 * <p>Tag pages used to be published under the tag exactly as written, spaces included
 * ({@code tags/acceptance test.html}), and a few tags existed in two spellings (DevOps and devops, Groovy and
 * groovy). With {@code tag.sanitize=true} JBake turns each space into a hyphen ({@code tags/acceptance-test.html}),
 * and the capitalized spellings were lowercased in the posts. Every old URL in the baseline of published URLs that
 * no longer has a real tag page gets a small HTML page at the old path that sends readers (and search engines) on
 * to the new one: a meta refresh, rel=canonical, robots noindex, and a visible link.
 *
 * <p><b>Where the stubs go.</b> Most under the assets' {@code tags/}, which JBake copies as they are. A stub whose
 * name differs from a real tag page only by letter case ({@code tags/DevOps.html} to {@code tags/devops.html}) goes
 * under the case-redirects directory: on a case-insensitive filesystem (macOS) the two names are one file, and JBake
 * copies assets after rendering, so the stub would replace the real page. The bake adds those only when the output
 * folder is case-sensitive, as on Linux, where CI builds and GitHub Pages serves.
 *
 * <p><b>The old tag URLs kept</b> are every tag URL in the baseline (the live site before the revival), and the
 * hyphenated page of every baseline tag with a space ({@code tags/acceptance-test.html}): those were published by
 * T039 on 2026-10-09 and aren't in the baseline, which only changes at a milestone checkpoint. Each is derived from
 * its baseline twin, so the list needs no upkeep.
 *
 * <p><b>The new name of an old tag</b> is worked out from the posts: spaces become hyphens, as JBake does, and if no
 * published post uses that exact tag any more, a tag that differs only by case is used. A tag that was merged into
 * another or renamed on purpose is listed in the renames (old to new, as written in the posts; {@link #RENAMES} is
 * this site's list, the {@code jbakeSite.tagRenames} default), and all its old URLs (spaced and hyphenated) point to
 * the new tag's page. An old tag with no match at all is an error: decide what it should point to (add it to the
 * renames) before going on.
 *
 * <p>{@code check} also makes sure each real tag page shows its tag as written in the posts, spaces included
 * ({@code templates/tag-names.ftl}, T057): JBake keeps only the hyphenated form, so a tag with a hyphen of its own
 * ({@code apt-get}) must be listed in that template, or it would be shown as "apt get".
 *
 * <p><b>The 59 committed stubs are published URLs and committed bytes</b> (Rule 1): write mode reproduces every one
 * of them byte for byte ({@code git status} is clean after a run on a clean tree), which is why {@link #MARKER}
 * still names the Python script that first wrote them, and why the percent-encoding is hand-rolled to match
 * FreeMarker's {@code ?url_path} (the canonicals in {@code head-meta.ftl}) rather than taken from
 * {@code java.net.URLEncoder}, which writes "+" for a space and encodes "~".
 */
public final class TagRedirects {

    /** The first line after the doctype; kept as the Python script wrote it, so the committed stubs regenerate byte for byte. */
    static final String MARKER = "<!-- tag redirect stub (scripts/tag-redirects.py) -->";

    /**
     * This site's tags merged into another tag or renamed, old to new, both as written in the posts' {@code tags=}
     * lines; the default of {@code jbakeSite.tagRenames}. Every old URL of the old tag (baseline and hyphenated)
     * becomes a stub to the new tag's page. Never remove an entry: its stubs are published URLs. Owner-approved
     * 2026-10-09 (docs/reports/content-2026-10-09.md, section H).
     */
    public static final Map<String, String> RENAMES;
    static {
        Map<String, String> renames = new LinkedHashMap<>();
        renames.put("presentation", "presentations");                       // H1 merge
        renames.put("talks", "presentations");                              // H2 merge
        renames.put("excella", "Excella Consulting");                       // H3 merge
        renames.put("vi", "vim");                                           // H4 merge
        renames.put("lamdbas", "lambdas");                                  // H5 typo
        renames.put("google calender", "google calendar");                  // H6 typo
        renames.put("software craftsmenship", "software craftsmanship");    // H7 typo
        renames.put("paired programming", "pair programming");              // H8 the usual term
        renames.put("build", "build automation");                           // H9 merge
        RENAMES = Collections.unmodifiableMap(renames);
    }

    private static final String TAGS = "tags/";
    private static final String HTML = ".html";
    private static final Pattern REFRESH = Pattern.compile("<meta http-equiv=\"refresh\" content=\"0; url=([^\"]+)\">");
    private static final Pattern CANONICAL = Pattern.compile("<link rel=\"canonical\" href=\"([^\"]+)\">");
    private static final Pattern OG_TITLE = Pattern.compile("<meta property=\"og:title\" content=\"Tag: ([^\"]*)\">");
    private static final Pattern ENTITY = Pattern.compile("&(#x[0-9A-Fa-f]+|#[0-9]+|amp|lt|gt|quot|apos);");
    private static final String URL_PATH_SAFE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789/-_.!~*'()";

    private final SiteLayout site;
    private final String siteHost;
    private final Map<String, String> renames;
    private final PrintStream out;

    /**
     * Creates the tool.
     *
     * @param site where the content, the assets, the case-redirect stubs and the baseline are
     * @param siteHost the published address the canonical links are built from, {@code https://www.mikemcgarr.com}
     * @param renames tags merged or renamed on purpose, old to new as written in the posts ({@link #RENAMES}); errors
     *        about them are reported in this order
     * @param out where the tool reports
     */
    public TagRedirects(SiteLayout site, String siteHost, Map<String, String> renames, PrintStream out) {
        this.site = site;
        this.siteHost = siteHost;
        this.renames = Collections.unmodifiableMap(new LinkedHashMap<>(renames));
        this.out = out;
    }

    /** An old tag URL and where it comes from: the baseline, or derived from it since T039. */
    record OldUrl(String url, String source) {
    }

    /**
     * Encodes a site path the way head-meta.ftl does (FreeMarker ?url_path), so canonicals match.
     *
     * @param path the path, such as {@code tags/boyd's law.html}
     * @return the path with everything but the unreserved characters and {@code /-_.!~*'()} percent-encoded
     */
    static String urlPath(String path) {
        StringBuilder sb = new StringBuilder();
        for (byte b : path.getBytes(StandardCharsets.UTF_8)) {
            int c = b & 0xFF;
            if (c < 128 && URL_PATH_SAFE.indexOf(c) >= 0) {
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
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '%' && i + 2 < s.length() && isHex(s.charAt(i + 1)) && isHex(s.charAt(i + 2))) {
                out.write(Integer.parseInt(s.substring(i + 1, i + 3), 16));
                i += 2;
            } else {
                out.writeBytes(String.valueOf(c).getBytes(StandardCharsets.UTF_8));
            }
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    private static boolean isHex(char c) {
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
        Matcher m = ENTITY.matcher(s);
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

    /** Every tag of every published page, as written in its front matter (spaces kept). */
    Set<String> writtenTags() throws IOException {
        Set<String> tags = new LinkedHashSet<>();
        for (Path f : FileTrees.regularFilesUnder(site.content())) {
            FrontMatter header = FrontMatter.parse(Text.read(f));
            if (header.isPublished()) {
                tags.addAll(Tags.split(header.get("tags")));
            }
        }
        return tags;
    }

    /** Tag names as JBake publishes them: every tag of every published page, spaces made hyphens. */
    private Set<String> publishedTags() throws IOException {
        Set<String> out = new LinkedHashSet<>();
        for (String t : writtenTags()) {
            out.add(t.replace(' ', '-'));
        }
        return out;
    }

    /** The tag URLs in the baseline (the live site before the revival), in file order. */
    private List<String> baselineTagUrls() throws IOException {
        List<String> urls = new ArrayList<>();
        for (String line : Files.readAllLines(site.publishedUrls(), StandardCharsets.UTF_8)) {
            if (line.startsWith(TAGS)) {
                urls.add(line.strip());
            }
        }
        return urls;
    }

    /** The hyphenated twin of every baseline tag URL with a space: published since T039 (2026-10-09), not in the baseline. */
    private List<String> hyphenatedTagUrls() throws IOException {
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

    /** Every tag URL the site has published and must keep, with where it comes from. */
    List<OldUrl> oldTagUrls() throws IOException {
        List<OldUrl> out = new ArrayList<>();
        for (String u : baselineTagUrls()) {
            out.add(new OldUrl(u, "baseline"));
        }
        for (String u : hyphenatedTagUrls()) {
            out.add(new OldUrl(u, "since T039"));
        }
        return out;
    }

    /**
     * Old path to new path for every old tag URL that no longer has a real tag page.
     *
     * @return the redirects, in the old URLs' order
     * @throws IOException if the content or the baseline can't be read
     * @throws ToolFailure when an old URL has no unambiguous target, or the renames disagree with the posts
     */
    Map<String, String> redirects() throws IOException {
        Set<String> tags = publishedTags();
        Map<String, List<String>> byFold = new HashMap<>();
        for (String t : tags) {
            byFold.computeIfAbsent(t.toLowerCase(Locale.ROOT), k -> new ArrayList<>()).add(t);
        }
        Map<String, String> renames = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : this.renames.entrySet()) {
            renames.put(e.getKey().replace(' ', '-'), e.getValue().replace(' ', '-'));
        }
        Map<String, String> pairs = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        for (Map.Entry<String, String> e : renames.entrySet()) {
            if (tags.contains(e.getKey())) {
                errors.add("jbakeSite.tagRenames: '" + e.getKey() + "' is still used by a published post (its stub would replace the real page)");
            }
            if (!tags.contains(e.getValue())) {
                errors.add("jbakeSite.tagRenames: '" + e.getKey() + "' -> '" + e.getValue() + "', but no published post uses '" + e.getValue() + "'");
            }
        }
        for (OldUrl o : oldTagUrls()) {
            String old = o.url();
            String name = old.substring(TAGS.length(), old.length() - HTML.length());
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
                pairs.put(old, TAGS + nu + HTML);
            }
        }
        if (!errors.isEmpty()) {
            throw ToolFailure.failed("ERROR: can't work out where these old tag URLs should point:\n  " + String.join("\n  ", errors));
        }
        return pairs;
    }

    /**
     * The text of the stub at an old URL.
     *
     * @param nu the new URL, such as {@code tags/boyd's-law.html}
     * @return the page, with the canonical link on the site's host
     */
    String stub(String nu) {
        String target = nu.substring(nu.lastIndexOf('/') + 1);
        String href = htmlEscape(urlPath(target).replace("'", "%27"));
        String canonical = htmlEscape(siteHost + "/" + urlPath(nu));
        String title = htmlEscape("Tag: " + target.substring(0, target.length() - HTML.length()));
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

    /** Where the stub for an old URL lives: with the case-redirects when only the case differs, else with the assets. */
    Path stubPath(String old, String nu) {
        return (old.equalsIgnoreCase(nu) ? site.caseRedirects() : site.assets()).resolve(old);
    }

    /**
     * Writes any missing or outdated stub; never deletes one, and says which committed stubs it no longer makes.
     *
     * @throws IOException if a file can't be read or written
     * @throws ToolFailure when the redirects can't be worked out
     */
    public void write() throws IOException {
        Map<String, String> pairs = redirects();
        int written = 0;
        Set<Path> expected = new LinkedHashSet<>();
        for (Map.Entry<String, String> e : pairs.entrySet()) {
            Path path = stubPath(e.getKey(), e.getValue());
            String text = stub(e.getValue());
            expected.add(path);
            if (Files.exists(path) && Files.readString(path, StandardCharsets.UTF_8).equals(text)) {
                continue;
            }
            Files.createDirectories(path.getParent());
            Files.writeString(path, text, StandardCharsets.UTF_8);
            written++;
            out.println("wrote " + site.display(path) + " -> " + e.getValue());
        }
        for (Path d : List.of(site.assets(), site.caseRedirects())) {
            for (Path p : FileTrees.regularFilesUnder(d.resolve("tags"))) {
                if (Text.read(p).contains(MARKER) && !expected.contains(p)) {
                    out.println("NOTE: " + site.display(p) + " is a stub this script no longer makes. It's a published URL: keep it unless approved.");
                }
            }
        }
        out.println(pairs.size() + " tag redirect stubs (" + written + " written, " + (pairs.size() - written) + " already up to date)");
    }

    /** True if site/rel exists with exactly this spelling (Files.exists ignores case on macOS). */
    private static boolean existsExactly(Path site, String rel) throws IOException {
        Path p = site.resolve(rel);
        Path dir = p.getParent();
        if (dir == null) {
            return false;
        }
        String name = p.getFileName().toString();
        return FileTrees.regularFilesIn(dir).stream().anyMatch(x -> x.getFileName().toString().equals(name));
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

    private static List<String> tagFiles(Path site) throws IOException {
        return FileTrees.regularFilesIn(site.resolve("tags")).stream().map(p -> p.getFileName().toString()).sorted().toList();
    }

    /**
     * Checks a baked site: every old tag URL is a tag page or a stub whose target is a real tag page with the right
     * canonical, and every real tag page shows its tag as the posts write it.
     *
     * @param bakedSite the site, {@code build/jbake} or CI's {@code build/site}
     * @throws IOException if a page can't be read
     * @throws ToolFailure exit 2 when the site has no {@code tags/} folder, exit 1 with the problem count when a
     *         problem was found (each is printed first)
     */
    public void check(Path bakedSite) throws IOException {
        String shownSite = site.display(bakedSite);
        if (!Files.isDirectory(bakedSite.resolve("tags"))) {
            throw ToolFailure.couldNotRun(shownSite + " has no tags/ folder. Bake first (./gradlew bake).");
        }
        boolean caseInsensitive = Files.exists(bakedSite.resolve("INDEX.HTML"));
        List<String> problems = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        int real = 0;
        int stubs = 0;
        Map<String, String> pairs = redirects();
        List<OldUrl> oldUrls = oldTagUrls();
        for (OldUrl o : oldUrls) {
            String old = o.url();
            if (!existsExactly(bakedSite, old)) {
                if (caseInsensitive && pairs.containsKey(old) && old.equalsIgnoreCase(pairs.get(old))) {
                    skipped.add(old);
                } else {
                    problems.add(old + ": missing from the build");
                }
                continue;
            }
            String text = Files.readString(bakedSite.resolve(old), StandardCharsets.UTF_8);
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
            } else if (!existsExactly(bakedSite, target)) {
                problems.add(old + ": its target " + target + " is missing");
            } else if (Files.readString(bakedSite.resolve(target), StandardCharsets.UTF_8).contains("http-equiv=\"refresh\"")) {
                problems.add(old + ": its target " + target + " is itself a redirect");
            }
            if (target.contains(" ")) {
                problems.add(old + ": its target " + target + " has a space");
            }
            if (!canonical.find() || !htmlUnescape(canonical.group(1)).equals(siteHost + "/" + urlPath(target))) {
                problems.add(old + ": rel=canonical is not " + siteHost + "/" + urlPath(target));
            }
            if (!text.contains("<meta name=\"robots\" content=\"noindex\">")) {
                problems.add(old + ": no robots noindex");
            }
        }
        for (String f : tagFiles(bakedSite)) {
            if (f.contains(" ") && !pairs.containsKey(TAGS + f)) {
                problems.add(TAGS + f + ": a tag page with a space in its URL that isn't a known stub");
            }
        }
        problems.addAll(shownNames(bakedSite));
        Map<String, Integer> bySource = new HashMap<>();
        for (OldUrl o : oldUrls) {
            bySource.merge(o.source(), 1, Integer::sum);
        }
        out.println(oldUrls.size() + " old tag URLs (" + bySource.getOrDefault("baseline", 0) + " baseline, "
                + bySource.getOrDefault("since T039", 0) + " hyphenated since T039): " + real + " real tag pages, "
                + stubs + " redirect stubs, " + skipped.size() + " not checkable here");
        for (String old : skipped) {
            out.println("  ~ " + old + " (case-insensitive filesystem: same file as " + pairs.get(old) + "; the stub is added only on Linux)");
        }
        for (String p : problems) {
            out.println("  ! " + p);
        }
        if (!problems.isEmpty()) {
            throw ToolFailure.failed("FAIL: " + problems.size() + " problems in " + shownSite);
        }
        out.println("OK: every old tag URL in " + shownSite + " is a tag page or a stub whose target is a tag page");
    }

    /** Problems with the tag name a real tag page shows (its og:title, "Tag: <name>"), compared with the posts. */
    private List<String> shownNames(Path bakedSite) throws IOException {
        Map<String, String> written = new HashMap<>();
        for (String t : writtenTags()) {
            written.put(t.replace(' ', '-'), t);
        }
        List<String> problems = new ArrayList<>();
        int checked = 0;
        for (String f : tagFiles(bakedSite)) {
            if (!f.endsWith(HTML) || f.equals("index.html")) {
                continue;
            }
            String stem = f.substring(0, f.length() - HTML.length());
            if (!written.containsKey(stem)) {
                continue;
            }
            String text = Files.readString(bakedSite.resolve("tags").resolve(f), StandardCharsets.UTF_8);
            if (text.contains("http-equiv=\"refresh\"")) {
                continue;
            }
            Matcher title = OG_TITLE.matcher(text);
            String shown = title.find() ? htmlUnescape(title.group(1)) : null;
            String want = written.get(stem);
            checked++;
            if (!want.equals(shown)) {
                problems.add(TAGS + f + ": shows the tag as " + pyRepr(shown) + ", the posts write " + pyRepr(want)
                        + " (list it in " + site.display(site.templates().resolve("tag-names.ftl")) + ")");
            }
        }
        out.println(checked + " tag pages checked for the tag name they show: " + (checked - problems.size()) + " as written in the posts");
        return problems;
    }
}
