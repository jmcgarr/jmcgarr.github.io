package com.mikemcgarr.jbakesite.review;

import com.mikemcgarr.jbakesite.internal.SiteLayout;
import com.mikemcgarr.jbakesite.internal.Text;
import com.mikemcgarr.jbakesite.review.HtmlTags.Tag;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The checks on a rendered page: E2 (brackets left in the text, outside code and the archived comments) and E3
 * (every local link and image resolves in the baked site, fragments included). A finding points at the Markdown
 * line when the excerpt is found there, else at the HTML line.
 */
final class PageChecks {

    private static final Set<String> SKIP_TEXT_IN = Set.of("code", "pre", "script", "style", "textarea", "svg");
    private static final Set<String> VOID = Set.of("area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta",
            "param", "source", "track", "wbr");
    private static final Pattern LEFTOVER = Pattern.compile("\\]\\(|\\]\\s*(?:https?://|www\\.)|\\[[^\\]\\n]*\\]");
    private static final Set<String> LINK_ATTRS = Set.of("href", "src", "srcset", "poster", "data");
    private static final Pattern ID_ATTR = Pattern.compile("\\s(?:id|name)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')");

    private final SiteLayout site;
    private final Path bakedSite;
    private final Set<String> publishedUrls;
    private final List<String> ownHosts;
    private final Pages pages;
    private final Findings findings;
    private final Set<String> reviewedPages = new LinkedHashSet<>();
    private final Map<Path, Set<String>> idCache = new HashMap<>();

    PageChecks(SiteLayout site, Path bakedSite, String siteHost, Set<String> publishedUrls, Pages pages, Findings findings) {
        this.site = site;
        this.bakedSite = bakedSite;
        this.publishedUrls = publishedUrls;
        this.ownHosts = ownHosts(siteHost);
        this.pages = pages;
        this.findings = findings;
    }

    /** The site's own address in every spelling a link may use: https or http, with or without www. */
    static List<String> ownHosts(String siteHost) {
        String host = siteHost.replaceFirst("^https?://", "").toLowerCase(Locale.ROOT);
        String bare = host.startsWith("www.") ? host.substring(4) : host;
        return List.of("https://www." + bare + "/", "https://" + bare + "/", "http://www." + bare + "/", "http://" + bare + "/");
    }

    /** The pages checked so far, in order. */
    Set<String> reviewedPages() {
        return reviewedPages;
    }

    void checkPage(String page, Change source) throws IOException {
        Path file = bakedSite.resolve(page);
        if (!Files.isRegularFile(file)) {
            findings.add(true, source != null ? "E4" : "E3", source != null ? site.display(source.path()) : page, 1,
                    "JBake produced no page " + page + " in " + site.display(bakedSite) + ". Check the header, then ./gradlew bake.", null);
            return;
        }
        reviewedPages.add(page);
        String html = Files.readString(file, StandardCharsets.UTF_8);
        // The page's Markdown, when it has one: the change that made it, or, for a page pulled in by a template,
        // image or asset change, the file it is baked from. Without it, brackets the author escaped as \[…\] were
        // reported as E2 (found in T134, when deleting an asset pulled in such a page), and a finding couldn't
        // point at its source line.
        Path sourcePath = source != null ? source.path() : pages.sourceOf(page);
        String[] sourceLines = sourcePath == null ? new String[0] : Text.lines(Text.read(sourcePath));
        String sourceFile = sourcePath == null ? null : site.display(sourcePath);
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
                String text = HtmlTags.decode(html.substring(pos, lt));
                if (skipDepth < 0 && commentsDepth < 0 && !text.isBlank()) {
                    Matcher m = LEFTOVER.matcher(text);
                    while (m.find()) {
                        String excerpt = m.group();
                        if (sourceLine(sourceLines, excerpt.replace("[", "\\[").replace("]", "\\]")) > 0) {
                            continue;   // written as \[…\] in the Markdown: the brackets are meant to show
                        }
                        int at = sourceLine(sourceLines, excerpt);
                        findings.unlessIgnored(ignore, true, "E2", at > 0 ? sourceFile : page, at > 0 ? at : line,
                                "The page shows \"" + Report.shorten(text.strip()) + "\" with the brackets as text. A Markdown link"
                                        + " didn't form (see E1), or a bracket needs escaping as \\[." + (at > 0 ? "" : " (On " + page + ".)"), null);
                    }
                }
                line += HtmlTags.count(html, pos, lt, '\n');
                pos = lt;
                continue;
            }
            if (html.startsWith("<!--", pos)) {
                int end = html.indexOf("-->", pos);
                end = end < 0 ? len : end + 3;
                Set<String> suppression = Suppressions.in(html.substring(pos, end));
                if (suppression != null) {
                    pendingIgnore = suppression;
                }
                line += HtmlTags.count(html, pos, end, '\n');
                pos = end;
                continue;
            }
            int gt = HtmlTags.endOfTag(html, pos);
            String inside = html.substring(pos + 1, gt);
            Tag tag = HtmlTags.parseTag(inside);
            if (tag == null) {
                line += HtmlTags.count(html, pos, gt + 1, '\n');
                pos = gt + 1;
                continue;
            }
            if (tag.closing()) {
                int depth = openTags.lastIndexOf(tag.name());
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
                boolean leaf = tag.selfClosing() || VOID.contains(tag.name());
                if (pendingIgnore != null) {
                    ignore = pendingIgnore;
                    ignoreDepth = leaf ? -1 : openTags.size();
                    pendingIgnore = null;
                }
                if (skipDepth < 0 && commentsDepth < 0) {
                    checkLinks(tag, page, pageDir, ids, line, ignore, sourceLines, sourceFile);
                }
                if (!leaf) {
                    openTags.add(tag.name());
                    if (skipDepth < 0 && SKIP_TEXT_IN.contains(tag.name())) {
                        skipDepth = openTags.size() - 1;
                    }
                    if (commentsDepth < 0 && tag.name().equals("details")
                            && tag.attrs().getOrDefault("class", "").contains("archived-comments")) {
                        commentsDepth = openTags.size() - 1;
                    }
                    if (tag.name().equals("script") || tag.name().equals("style")) {
                        int close = html.toLowerCase(Locale.ROOT).indexOf("</" + tag.name(), gt);
                        close = close < 0 ? len : close;
                        line += HtmlTags.count(html, pos, close, '\n');
                        pos = close;
                        continue;
                    }
                }
                if (leaf && ignoreDepth < 0 && pendingIgnore == null && !ignore.isEmpty()) {
                    ignore = Set.of();   // a suppression aimed at a void tag (an <img>) is spent
                }
            }
            line += HtmlTags.count(html, pos, gt + 1, '\n');
            pos = gt + 1;
        }
    }

    private void checkLinks(Tag tag, String page, String pageDir, Set<String> ids, int line, Set<String> ignore,
            String[] sourceLines, String sourceFile) throws IOException {
        for (Map.Entry<String, String> a : tag.attrs().entrySet()) {
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
                    findings.unlessIgnored(ignore, true, "E3", at > 0 ? sourceFile : page, at > 0 ? at : line, "<" + tag.name() + " " + a.getKey()
                            + "=\"" + Report.shorten(url) + "\">: " + problem + (at > 0 ? "" : " (On " + page + ".)"), null);
                }
            }
        }
    }

    /** Returns null when the URL resolves (or isn't ours to check), else what's wrong. */
    String resolve(String url, String pageDir, Set<String> pageIds, String page) throws IOException {
        if (url.isEmpty()) {
            return "the URL is empty.";
        }
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.startsWith("mailto:") || lower.startsWith("tel:") || lower.startsWith("javascript:") || lower.startsWith("data:")) {
            return null;
        }
        String path = url;
        String own = ownHosts.stream().filter(lower::startsWith).findFirst().orElse(null);
        if (own != null) {
            path = "/" + url.substring(own.length());
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
        path = URLDecoder.decode(path.replace("+", "%2B"), StandardCharsets.UTF_8);
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
        Path target = bakedSite.resolve(sitePath);
        boolean exists = Files.isRegularFile(target)
                || Files.isRegularFile(site.caseRedirects().resolve(sitePath))
                || publishedUrls.contains(sitePath);
        // A bake never deletes: a file removed from src/jbake/assets keeps a stale copy in the site until a clean bake.
        // So anything that isn't rendered or generated must still have its source.
        boolean generated = sitePath.endsWith(".html") || sitePath.endsWith(".xml") || sitePath.startsWith("css/");
        boolean stale = exists && !generated && !Files.isRegularFile(site.assets().resolve(sitePath))
                && !Files.isRegularFile(site.caseRedirects().resolve(sitePath)) && !publishedUrls.contains(sitePath);
        if (stale) {
            return "nothing at /" + sitePath + " in " + site.display(site.assets()) + " any more (the copy in " + site.display(bakedSite)
                    + " is left over from an earlier bake).";
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
            Set<String> ids = sitePath.equals(page) ? pageIds : idsOf(bakedSite.resolve(sitePath));
            if (!ids.contains(fragment)) {
                return "/" + sitePath + " has no id=\"" + fragment + "\" to jump to.";
            }
        }
        return null;
    }

    private Set<String> idsOf(Path htmlFile) throws IOException {
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
}
