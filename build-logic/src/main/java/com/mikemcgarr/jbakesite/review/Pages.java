package com.mikemcgarr.jbakesite.review;

import com.mikemcgarr.jbakesite.FileTrees;
import com.mikemcgarr.jbakesite.internal.FrontMatter;
import com.mikemcgarr.jbakesite.internal.SiteLayout;
import com.mikemcgarr.jbakesite.internal.Text;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/** Which baked page a content file renders to, and back; and which pages a change pulls into the review. */
final class Pages {

    private final SiteLayout site;
    private final Path bakedSite;

    Pages(SiteLayout site, Path bakedSite) {
        this.site = site;
        this.bakedSite = bakedSite;
    }

    /** The site-relative page a content file renders to: blog/x.md -> blog/x.html, a draft -> blog/drafts/x-draft.html. */
    String pageFor(Path source, FrontMatter fm) {
        String rel = SiteLayout.relativePath(site.content(), source);
        String stem = rel.substring(0, rel.length() - ".md".length());
        return stem + (fm.isDraft() ? "-draft.html" : ".html");
    }

    /** The Markdown a page is baked from (pageFor in reverse), or null for a template-only page such as index.html. */
    Path sourceOf(String page) {
        if (!page.endsWith(".html")) {
            return null;
        }
        String stem = page.substring(0, page.length() - ".html".length());
        if (stem.startsWith("blog/drafts/") && stem.endsWith("-draft")) {
            stem = stem.substring(0, stem.length() - "-draft".length());
        }
        Path source = site.content().resolve(stem + ".md");
        return Files.isRegularFile(source) ? source : null;
    }

    /** For a template change: the four pages the Definition of Done names, plus the newest and the oldest post. */
    List<String> samplePages() throws IOException {
        List<String> pages = new ArrayList<>(List.of("index.html", "archive.html", "tags/index.html", "about.html"));
        TreeMap<String, String> byDate = new TreeMap<>();
        for (Path p : FileTrees.regularFilesIn(site.blog())) {
            if (!p.toString().endsWith(".md")) {
                continue;
            }
            FrontMatter fm = FrontMatter.parse(Text.read(p));
            if (fm.get("status").equals("published") && fm.get("type").equals("post")) {
                byDate.put(fm.get("date") + " " + p.getFileName(), pageFor(p, fm));
            }
        }
        if (!byDate.isEmpty()) {
            pages.add(byDate.lastEntry().getValue());
            pages.add(byDate.firstEntry().getValue());
        }
        return pages;
    }

    /** Pages in the baked site whose HTML mentions this asset (by its site path, with or without the leading /). */
    List<String> referencing(Path asset) throws IOException {
        String sitePath = SiteLayout.relativePath(site.assets(), asset);
        List<String> pages = new ArrayList<>();
        for (Path p : FileTrees.regularFilesUnder(bakedSite)) {
            if (!p.toString().endsWith(".html")) {
                continue;
            }
            String html = Files.readString(p, StandardCharsets.UTF_8);
            if (html.contains("\"/" + sitePath) || html.contains("\"" + sitePath) || html.contains("/" + sitePath + "\"")) {
                pages.add(SiteLayout.relativePath(bakedSite, p));
            }
        }
        return pages;
    }
}
