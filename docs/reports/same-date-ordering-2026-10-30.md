# Same-date post ordering: design report

**Branch**: `fix/same-date-ordering` | **Created**: 2026-10-30 | **Status**: Done (point-in-time report)

**Mode:** implementation plan (with a short option evaluation). **Date:** 2026-10-30. Designed read-only against `origin/main` (`9b7b731`); the implementation on this branch follows it, see section 6.

**Recommendation (option b):** one new include, `src/jbake/templates/newest-first.ftl`, defining `newestFirst(docs)` = `docs?sort_by("uri")?reverse?sort_by("date")?reverse`, called from the five templates that list content. Verified on a scratch copy: the bake becomes identical on 1, 4 and 12 OrientDB clusters, and it reproduces today's live order for every tied post group. The only visible change is two sitemap lines (`about.html`/`talks.html` swap). Index pagination is deterministic today (no tie in the top 6) but is only protected by this fix.

## 1. Root cause (corrected)

The crawl order is **not** the cause. JBake 2.7's `Crawler.crawl()` calls `Arrays.sort()` on `File.listFiles()` (verified with `javap` on `jbake-core-2.7.0.jar`), so files are inserted alphabetically on every OS.

The tie-break comes from OrientDB 3.1.20 (`db.store=memory`). Every list query is `select * from Documents where ... order by date desc` (`ContentStore.STATEMENT_GET_PUBLISHED_CONTENT_BY_DOCTYPE`, `..._POSTS_BY_TAG`). `OrderByStep` uses `List.sort` (stable), so ties keep the **scan order**, and `FetchFromClassExecutionStep` scans the class cluster by cluster. `class.minimumClusters` defaults to `0` = "automatic" = **one cluster per CPU core**. The Mac has 12 cores; GitHub's `ubuntu-latest` has 4.

Proof: running `org.jbake.launcher.Main` directly on the worktree's `src/jbake`:

| JVM flag | tie order (archive.html) |
|---|---|
| none (12 cores) | phoenix, dc-tech; build-script, boyds (= macOS Gradle bake) |
| `-Dclass.minimumClusters=12` | same |
| `-Dclass.minimumClusters=1` | same |
| `-Dclass.minimumClusters=4` | **dc-tech, phoenix; boyds, build-script (= live artifact)** |

So the live order is an accident of the runner's vCPU count; a bigger runner would reorder the live site.

## 2. Inventory

| Template | Output | Data | Tie-sensitive today |
|---|---|---|---|
| `index.ftl` | `index.html` | `published_posts[0..*6]` (no `index.paginate`; one page of 6) | no tie in ranks 1-7 |
| `archive.ftl` | `archive.html` | `published_posts` | yes |
| `feed.ftl` | `feed.xml` | `published_posts` | yes |
| `sitemap.ftl` | `sitemap.xml` | `published_pages` then `published_posts` | yes (both) |
| `tags.ftl` | `tags/<tag>.html` | `tag_posts` (`getPublishedPostsByTag`) | yes, `tags/devops.html` only |
| `tags-index.ftl` | `tags/index.html` | `tags[].tagged_posts?size` (counts only) | no |
| `head-meta.ftl` | all pages | `published_posts?last.date` (year only) | no |
| `post.ftl`, `page.ftl`, `archived-comments/*` | - | no lists, no prev/next | no |

Document map keys (`org.jbake.model.ModelAttributes`): `uri`, `file`, `title`, `date`, `type`, `status`, `tags`, `sourceuri`, `rootpath`, `noExtensionUri`, `body`. `uri` is the published path (`blog/<slug>.html`), unique and stable.

**Tied groups** (`grep '^date=' src/jbake/content/blog/*.md`; 75 posts, 2 groups; plus the two pages):

| date | rank (newest = 1) | slugs, live (Linux) order | macOS order | on index? |
|---|---|---|---|---|
| 2013-02-21 | 18, 19 | `dc-tech-ops-meetup`, `the-phoenix-project` | reversed | no |
| 2013-02-15 | 20, 21 | `boyds-law-of-iteration`, `build-script-best-practice` | reversed | no |
| 2026-10-21 (pages) | - | `talks.html`, `about.html` (sitemap lines 4/8) | same as live | n/a |

Live evidence: run 40909296876 (`gh run download --name github-pages`; Pages artifacts expire after about a day, so later readers compare against https://www.mikemcgarr.com instead, which the review of this branch did), `archive.html` lines 227-233, `sitemap.xml` lines 80-92, `feed.xml` `<link>` lines 904/922/949/969, `tags/devops.html` (read with `tar -xOf`; the extracted directory collapses `devops.html`/`DevOps.html` on APFS).

**Key choice:** `uri` ascending reproduces the live order for both post groups (every reordered file). `title` fails ("Book Review..." < "DC Tech..."). No key reproduces the pages' live order too (`talks` before `about` is Z-A), so the sitemap's two page entries swap. Acceptable: sitemap order carries no meaning, and the current order is itself a cluster-scan accident.

## 3. Options

- **(a) Inline `?sort_by` in each template.** Same expression pasted five times with a comment each. Works, but the double-`?reverse` idiom is non-obvious and would drift.
- **(b) Shared function in one include - recommended.** Matches the `tag-names.ftl` precedent and the idiom already used in `tags-index.ftl` (`topics?reverse?sort_by("count")?reverse`). FreeMarker's `sort_by` is `Collections.sort` (stable, verified in `freemarker-2.3.35.jar`); it has no descending mode, hence: sort by `uri` A-Z, reverse, stable sort by `date`, reverse -> date newest first, ties A-Z.
- **(c) Plugin.** Crawler order cannot be influenced without forking (`Arrays.sort` is hard-coded) and isn't the problem. OrientDB's scan order **can**: `spec.systemProperty("class.minimumClusters", "4")` in `JbakeTask.runJbake` (needed for `BakeTask` and `BakePreviewTask`) reproduces the live output byte for byte, pages included. Rejected as the fix: it pins the order to an undocumented OrientDB internal and today's runner size, and nobody reading a template could tell why ties fall as they do. It would make a cheap belt-and-braces addition, but one mechanism is enough.
- **(d) Change the posts' `date=`.** Rejected: Rule 3.

## 4. Implementation (branch `fix/same-date-ordering`)

1. New `src/jbake/templates/newest-first.ftl`:
   ```ftl
   <#-- Newest first; posts with the same date by URL, A-Z. JBake's lists come sorted by date only; same-date
        order depends on the CPU-core count of the baking machine (OrientDB clusters). sort_by is stable and
        has no descending mode, hence the two ?reverse. docs/reports/same-date-ordering-2026-10-30.md -->
   <#function newestFirst docs>
     <#return docs?sort_by("uri")?reverse?sort_by("date")?reverse>
   </#function>
   ```
2. `index.ftl`, `archive.ftl`, `tags.ftl`: add `<#include "newest-first.ftl">` after `<#include "header.ftl">`; wrap the list: `newestFirst(published_posts)[0..*6]`, `newestFirst(published_posts)`, `newestFirst(tag_posts)`.
3. `feed.ftl`, `sitemap.ftl`: add the include on line 2 (after the `<?xml ...?>` line; FreeMarker strips the tag-only line, so the declaration stays byte 0, verified); wrap `published_posts` and, in the sitemap, `published_pages`.
4. Report: `docs/reports/same-date-ordering-2026-10-30.md` (point-in-time; the root cause, the tables above, before/after diffs). No plan task exists; if the owner wants one, add it to `docs/archive/05-REVIEW-FLOW.md` with `Test: diff -rq` against the artifact.
5. Optional follow-up (not in this PR): a `build-logic` fixture test with two same-date posts asserting order; and a one-line note in README's template list.

## 5. Verification

- Before (done, main on macOS vs live artifact): `diff -rq` lists `archive.html`, `feed.xml`, `sitemap.xml`, plus `blog/drafts`, and `tags/DevOps.html` vs `tags/devops.html` (APFS case folding). `tags/devops.html` also differs when read from the tar.
- After (done on the scratch copy, to be repeated with `./gradlew clean bake` on the branch): `archive.html`, `index.html`, `tags/devops.html` identical to live; `feed.xml` differs only in `<pubDate>`/`<lastBuildDate>` lines 9-10; `sitemap.xml` differs only in lines 4 and 8 (page swap); the drafts/case-stub entries as before. Same output with `class.minimumClusters` 1, 4 and 12.
- `./gradlew check` (bake, reviewChanges, checkTagRedirects, checkMastheadVariants) and `test ! -e build/jbake/docs`. CI's "Validate feed and sitemap XML" step covers the XML declaration.
- Post-merge: download the new `github-pages` artifact and `diff -rq` against a local bake: only the feed timestamps and the two case stubs may remain. CI does not upload artifacts for PRs, so the Linux check happens after merge.

**Feed risk:** none. `<guid isPermaLink="false">${post.uri}</guid>` is unchanged, items keep their `pubDate`, and the deployed feed's item order is already the one the fix produces; readers see only the usual timestamp change.

## 6. Result (branch `fix/same-date-ordering`, 2026-10-30)

Implemented as in section 4: `newest-first.ftl` plus the five template edits (byte-identical to the scratch copy that
sections 1 and 5 were verified on). README has no list of the template includes, so it was left alone. All checks
below ran on macOS (12 cores) in a worktree of `origin/main` at `9b7b731`.

**Before/after on the same machine** (`./gradlew clean bake` of `main`, then of the branch; `diff -rq`): exactly
`archive.html`, `feed.xml`, `sitemap.xml` and `tags/devops.html` differ; `index.html` is identical.
`diff <(sort a) <(sort b)` is empty for `archive.html`, `sitemap.xml` and `tags/devops.html`; for `feed.xml` it is
empty once the two build-timestamp lines (9-10) are dropped. `archive.html`:

```
227,228d226
< 					<li>21 - <a href="blog/the-phoenix-project.html">Book Review: The Phoenix Project</a></li>
<
231c229
< 					<li>15 - <a href="blog/build-script-best-practice.html">Build Scripts and External Dependencies</a></li>
---
> 					<li>21 - <a href="blog/the-phoenix-project.html">Book Review: The Phoenix Project</a></li>
233a232,233
>
> 					<li>15 - <a href="blog/build-script-best-practice.html">Build Scripts and External Dependencies</a></li>
```

`sitemap.xml` swaps lines 4/8 (`talks.html`/`about.html`), 80/84 (`the-phoenix-project`/`dc-tech-ops-meetup`) and
88/92 (`build-script-best-practice`/`boyds-law-of-iteration`); `tags/devops.html` moves the Phoenix Project line
(131-132 to 133-134) below DC Tech Ops; `feed.xml` moves the DC Tech Ops item above the Phoenix Project item and the
Boyd's Law item above the build-script item (`<link>`/`<guid>` lines 904-974), nothing else.

**Against the live Linux artifact** (run 40909296876, the `main` deploy of `9b7b731`, read from the tar with
`tar -xOf`): `archive.html`, `index.html` and `tags/devops.html` are byte-identical. `feed.xml` differs in lines 9-10
(build time) and in the time-zone suffix of every item's `<pubDate>` (`+0000` on the runner, `-0700`/`-0800` here, the
environment difference CHK047 in `06-GRADLE-PLUGIN.md` already records); with the zone suffix normalized, only lines
9-10 differ, so the item order now matches live. `sitemap.xml` differs only in lines 4 and 8:

```
4c4
<         <loc>https://www.mikemcgarr.com/talks.html</loc>
---
>         <loc>https://www.mikemcgarr.com/about.html</loc>
8c8
<         <loc>https://www.mikemcgarr.com/about.html</loc>
---
>         <loc>https://www.mikemcgarr.com/talks.html</loc>
```

That is the only change the live site will show after the merge.

**Cluster experiment, repeated on the branch's `src/jbake`** (`org.jbake.launcher.Main -b` on the classpath of
section 1, `-Dclass.minimumClusters=` 1, 4 and 12): the three outputs are identical apart from the feed's build
timestamp, and the 4-cluster output equals the Gradle bake apart from `feed.xml`'s timestamp and the `css/` directory
that Sass adds. The bake no longer depends on the core count.

**Checks:** `./gradlew check` passes (`reviewChanges`: 6 pages from 6 changed files, 0 errors, 0 warnings;
`checkUrls`: all 272 baseline URLs accounted for; `checkTagRedirects` and `checkMastheadVariants` OK);
`test ! -e build/jbake/docs` holds; no CR line endings in the edited files; `./gradlew bake` twice prints "Reusing
configuration cache"; `./gradlew clean bakePreview` served http://localhost:8080/archive.html and
`/tags/devops.html` with DC Tech Ops before The Phoenix Project and Boyd's Law before the build-script post, and the
home page's first post is the newest. `od -c` shows both XML files still start with `<?xml` at byte 0.

**Not done here:** the Linux `diff -rq` of the next deploy's artifact against a local bake (section 5, post-merge:
only the feed timestamps and the two case stubs may remain) and the optional `build-logic` fixture test.
