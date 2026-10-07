# Platform Decision: Stay on JBake or Migrate

**Branch**: `docs/T033-platform-decision` | **Created**: 2026-10-07 | **Status**: Accepted (2026-10-07)

Revival task T033 (M4).
- **Compared in depth:** staying on **JBake**, moving to **Roq** (Java), or moving to **Hugo** (Go).
- **Assessed, then excluded by the owner's language rule:** **Eleventy** and **Astro** (JavaScript).

Every fact below about this site was measured in the repo on 2026-10-07. Facts about the tools come from their
release pages, registries, docs, and source on the same date (see [Sources](#sources)). Results marked
**tested** come from builds in a scratch clone, and nothing from them was committed.

## Context

### The owner's requirements (2026-10-07)

1. **Language:**
   - **Java first, Go second.**
   - **No frameworks written in dynamic languages** (Ruby, JavaScript/TypeScript, Python).
2. **Markdown:** the owner is **considering moving from AsciiDoc to Markdown** for new writing. The platform must
   handle modern Markdown well:
   - normal line wrapping
   - tables
   - fenced code with syntax highlighting
   - footnotes, task lists, strikethrough
3. **URLs:** every published URL is permanent ([AGENTS.md](../AGENTS.md) Rule 1). Details under
   [What any platform must preserve](#what-any-platform-must-preserve).

### Why JBake in the first place

- The owner is a **long-time Java developer and a fan of Java**. JBake is a Java generator, built with the
  Java tools he already used (Gradle, Asciidoctor via AsciidoctorJ, FreeMarker).
- He moved the blog to JBake in **April 2014** (first commit 2014-04-13, "Copying from github.com/jmcgarr/blog")
  and wrote in a 2014 post: "*I am loving JBake*".
- He'd tried the Ruby route first. In May 2012, setting up Octopress led to
  [Ruby Broke my $PATH](../src/jbake/content/blog/ruby-broke-my-path.html), when RVM hijacked his Grails setup.

### How this site uses JBake (measured)

| What | How much | Notes |
|---|---|---|
| Legacy WordPress posts (`.html`) | **62** (2009–2013) | Imported as-is. 43 files have CR-only line endings. 23 use `<img>`, 16 inline `style=`, 5 `<iframe>` embeds (video, slides). 7 embed gists or SlideShare |
| AsciiDoc (`.asciidoc`) | **14**: 11 posts (2014–2020) + About, Talks, Speaker Bio | The main format since 2014. Light usage: images (9), links (14), lists, quotes, two listing blocks, one passthrough. **No** source highlighting, includes, attributes, tables, or admonitions, so converting these to Markdown later would be easy |
| Markdown (`.md`) | **1** (2019) | Renders correctly today, but see the next section |
| Drafts | 12 (10 HTML, 2 AsciiDoc) | Preview only, never published (T009) |
| Front matter | `key=value` + `~~~~~~` | Not YAML. `title`, `date`, `type`, `status`, `tags` on all. Custom: `summary` (15), `masthead` (12), `mastheadCredit` (10), `subtitle` (2) |
| Templates | **12 FreeMarker files, 415 lines** | Plus 10 generated archived-comment includes (T045). Uses only `published_posts`, `posts`, `tag_posts`, `published_pages`, `content.*`, `config.*` |
| JBake features used | tags, sitemap, feed, archive, drafts | **Not used:** pagination, tag index, data files, other template engines. A small surface, so it's easy to keep *and* easy to port |
| Build | Gradle plugin `org.jbake.site` 5.5.0 | Custom tasks: `preview` (jwebserver, T024) and the old `liveEdit` |

**A note on "no dynamic languages":** JBake's AsciiDoc support is AsciidoctorJ, which runs **Asciidoctor (Ruby)
on JRuby inside the JVM**. It's invisible day to day, but it's where the stray `jffi*.dylib` files fixed in T024
came from. Writing in Markdown takes JRuby out of the writing path. Removing AsciidoctorJ from the build entirely
would also mean converting the 14 AsciiDoc files.

### Markdown on JBake today (tested)

The site has no `markdown.extensions` setting, so JBake 2.6.6 (and 2.7.0) use their defaults:
`HARDWRAPS, AUTOLINKS, FENCED_CODE_BLOCKS, DEFINITIONS`. Markdown runs on flexmark 0.62 in pegdown-compatible mode.

A test post written the usual way:

| Markdown feature | JBake defaults | With one config line (below) |
|---|---|---|
| Line break inside a paragraph | ❌ becomes a `<br>` (`HARDWRAPS`) | ✅ joined into one paragraph |
| Tables | ❌ printed as literal `\| … \|` text | ✅ `<table>` |
| Task lists `- [ ]` | ❌ literal `[ ]` | ✅ checkboxes |
| `~~strikethrough~~` | ❌ literal | ✅ `<del>` |
| Footnotes `[^1]` | ❌ literal | ✅ linked footnotes |
| Smart quotes and dashes | ❌ | ✅ (`SMARTYPANTS`, optional) |
| Fenced code ```` ```java ```` | ✅ `<code class="language-java">` | ✅ (highlighting still needs a client-side highlighter, issue #15) |
| Autolinks, inline HTML | ✅ | ✅ |

The fix is a single line in `jbake.properties` (tested):

```properties
markdown.extensions=AUTOLINKS,FENCED_CODE_BLOCKS,DEFINITIONS,TABLES,STRIKETHROUGH,TASKLISTITEMS,FOOTNOTES,SMARTYPANTS
```

- **Effect on existing content:** no other page changes. The one existing Markdown post
  (`relections-and-projections-2019`) changes only typographically (`it's` → `it’s`, `...` → `…`) because of
  `SMARTYPANTS`. Leave that one out and nothing changes.
- **Remaining rough edges on JBake:**
  - The front matter stays `key=value` + `~~~~~~`, not YAML. GitHub's and editors' Markdown previews show it as
    text.
  - flexmark 0.62 is from 2020.
- **Found while testing:** a post without `summary=` **breaks the whole build**. `index.ftl` requires it for the
  newest six posts. The dead Gradle plugin then crashes while logging the error, so it surfaces as a misleading
  **"Java heap space"**. The README's new-post template includes `summary=`, so the documented path works, but
  it's a trap (see Consequences).

### What any platform must preserve

Any migration must prove it meets all of these:

1. **All 272 published URLs** in [`baseline/urls.txt`](baseline/urls.txt), served at the same paths:
   - posts `/blog/<name>.html`
   - pages such as `/about.html`
   - **181 tag pages** `/tags/<tag>.html`, of which **48 have spaces** (`tags/acceptance test.html`), **9 have
     capitals**, and some are odd cases: **`tags/.NET.html`**, **`tags/boyd's law.html`**, and the case-only pairs
     `DevOps`/`devops` and `Groovy`/`groovy` (T055)
2. **Feed `<guid>` values unchanged** (`blog/<name>.html`, `isPermaLink="false"`). Otherwise every RSS reader
   shows 15 years of posts as new.
3. **Giscus comment threads** are mapped by `pathname`, so a changed URL also loses its comments.
   GoatCounter statistics are also kept per path.
4. Drafts never published. `docs/` never published. Image URLs never removed (option B).
5. The CI gates (drafts, docs guard, URL check, XML, lychee, masthead variants) and the post-deploy smoke test
   keep working on the new output.

### Where JBake stands today

**What the revival fixed (M3):**
- The site builds on **Gradle 8.14.5 + JDK 21** on Apple Silicon and in CI, with output identical to 2014-era tooling.
- It deploys through GitHub Pages Actions, with no secrets.

**Project health:**
- **JBake itself:** 2.7.0 was released 2025-12-26. In the past year it had 9 commits by 3 people, so it's
  maintained, but slowly. It still embeds OrientDB, the source of issue #3's noise, and uses AsciidoctorJ 2.5.7.
- **The Gradle plugin is unmaintained:** the last release was 5.5.0 (2021-05-19) and the last commit 2022-01-02.
  - Its preview server already broke on Gradle 8. T024 replaced it with `jwebserver`.
  - **It can't run JBake 2.7.0** (a missing `commons-configuration` class).

**Tested:**
- The current plugin setup **also works on Gradle 9.8.0**, with output byte-identical to Gradle 8.14.5.
- **JBake 2.7.0 runs without the plugin**, through a plain Gradle `JavaExec` task of about 20 lines:
  - It needed one config change (`db.path=cache`, because 2.7.0 rejects `build/cache`) plus the existing
    jffi and JNA settings.
  - **Output: the same 442 files, byte-identical except the footer's "JBake v2.7.0".**
  - So the dead plugin is **not a trap**: there's a tested way off it that keeps JBake.

### How much the site changes

The last post was published **2020-01-03**. The owner's own commits ran 88 in 2014, 22 in 2018, 8 in 2020,
and 1 in 2025. The revival (M0–M3) exists to make writing again easy and safe. So the platform matters for
**(a) not breaking** in the years between posts and **(b) a pleasant writing loop** (M7 and issues #8–#12:
new-post scaffold, watch, live reload).

## Options

Effort assumes one person, the current Clean Blog look ported as-is, and the URL constraints above.

### A. Stay on JBake (Java)

- **Language:** ✅ Java, Gradle, FreeMarker. AsciiDoc runs on JRuby (see the note above). Markdown doesn't.
- **Markdown:** ✅ **with one config line** (tested above). Without it, modern Markdown renders badly.
  - Front matter stays non-YAML.
  - Syntax highlighting needs a small client-side library. That's a script on the page, not a build framework.
- **Fit:** ✅ everything works today and every gate passes. Nothing to port, and the URLs are safe by definition.
- **Risks:**
  - The Gradle plugin is unmaintained. That's mitigated: the `JavaExec` route is tested.
  - JBake core is slow-moving, and it's single-maintainer-ish.
  - It still embeds OrientDB, and its flexmark (0.62) and AsciidoctorJ (2.5) are old.
- **Writing loop:** no built-in live reload.
  - JBake's own CLI can bake, serve, and watch (`-b -s`).
  - Gradle continuous build (`./gradlew -t bake`) plus a browser refresh, or a small live-reload script,
    covers M7.
- **Effort:** none now. The Markdown fix is one line, and the other optional hardening is small (see Consequences).

### B. Roq (Java, Quarkus)

Full assessment and phased plan: [02-ROQ-MIGRATION.md](02-ROQ-MIGRATION.md).

- **Language:** ✅ Java 21 and Quarkus.
  - Templates in **Qute**, built with **Maven**, which is Roq's documented path.
  - AsciiDoc can be **pure Java**, so there's no JRuby, or full Asciidoctor via JRuby.
- **Markdown:** ✅ built in (CommonMark-based), with **standard YAML front matter**. Which extensions are on by
  default (tables, footnotes, and so on) is a spike check. The 14 AsciiDoc files can stay as they are.
- **Health:** active. Release 2.1.9 came out 2026-09-02, with 569 commits by about 25 authors in the past year.
  quarkus.io itself runs on it. But it's **young** (created 2024-05), and one maintainer wrote most of the code
  (about 674 commits, against 68 for the next contributor).
- **Fit:**
  - HTML posts with front matter are supported.
  - Plugins exist for tagging, aliases (redirect stubs), sitemap, series, and RSS.
  - **Biggest risk: tag URLs.** The tagging plugin's default pattern is `/posts/tag/<tag>/`. Reproducing
    `/tags/<Tag With Spaces>.html`, the case pairs, and `.NET` needs a spike (T076–T081).
  - Feed `<guid>` format also needs checking.
- **Writing loop:** ✅ the best Java option. Quarkus dev mode gives **live reload**, and the `roq` CLI scaffolds
  posts.
- **Effort:** **L.**
  - Rewrite 415 lines of FreeMarker in Qute.
  - Convert front matter on 89 files to YAML.
  - Move the build to Maven.
  - Port the mastheads, archived comments, and feed.
  - Do the URL spike and parity checks.

### C. Hugo (Go), the owner's second-choice language

- **Language:** ✅ Go, a **single compiled binary**, with no runtime to install. Templates are Go
  `html/template`.
- **Markdown:** ✅ **the strongest of the three.**
  - Goldmark (CommonMark) with tables, footnotes, task lists, strikethrough, linkify, definition lists, and smart
    typography **on by default**. Line wraps are not turned into `<br>`.
  - **Build-time syntax highlighting** (Chroma) with no JavaScript, which would settle issue #15.
  - Standard YAML/TOML front matter.
  - Render hooks for custom image and link output.
- **AsciiDoc is the catch, and Markdown removes it.** Hugo renders AsciiDoc by calling the external **Ruby
  `asciidoctor`** program, which would bring Ruby back to every machine and to CI. That breaks the language rule.
  - **So choosing Hugo means converting the 14 AsciiDoc files to Markdown first.** Their light feature use makes
    that an S–M task.
  - Each conversion keeps the file's base name, so URLs don't change. Each one needs a side-by-side rendering
    check, because posts are the author's voice (Rule 3).
- **Health:** ✅ very active (v0.167.0 on 2026-09-28) and the largest community of any generator. It's still
  pre-1.0, and releases sometimes carry breaking changes, so pin the version in CI.
- **Fit:**
  - `.html` content files with front matter are supported, which covers the 62 legacy posts.
  - Post URLs as `/blog/<name>.html` are configurable (`uglyURLs` / permalinks).
  - **Tag URLs are the risk:** by default Hugo lowercases term URLs and turns spaces into hyphens. Keeping the
    48 spaced and 9 capitalized tag URLs exact needs per-term URL overrides or redirect stubs (aliases), and a
    case-only pair can only be one page plus a stub. Spike needed.
  - The feed is a custom template, so `<guid>`s can be kept.
- **Writing loop:** ✅ excellent. `hugo server` gives instant live reload, and `hugo new` scaffolds posts.
- **Effort:** **L.**
  - Rewrite the templates in Go templates.
  - Convert front matter to YAML.
  - Convert AsciiDoc to Markdown.
  - Prove the tag URLs.
  - Replace Gradle with a pinned `hugo` binary in CI.

### Excluded by the language rule

Both were assessed, and both fail requirement 1 (JavaScript/TypeScript on Node). They're kept here for the record:

- **D. Eleventy** (being renamed "Build Awesome"):
  - **Version and governance:** stable 3.1.6 (2026-06-02), with 4.0 in alpha. Font Awesome acquired the project
    in 2024 and announced a rename to Build Awesome in March 2026. The rename was then paused after community
    backlash, so its direction is unsettled.
  - **URL control:** the best of any option. Permalinks are fully programmable.
  - **AsciiDoc:** via Asciidoctor.js.
- **E. Astro:**
  - **Version and churn:** very active (7.3.6, 2026-10-06), but six major versions since 1.0 (2022).
  - **Content:** AsciiDoc and raw-HTML posts need community loaders (`astro-asciidoc` 0.2.0, pre-1.0).
  - **Overall:** a component framework that's far more than this blog needs.

### Comparison

| Criterion | A. JBake | B. Roq | C. Hugo |
|---|---|---|---|
| **Language** (Java ▸ Go ▸ no dynamic) | ✅ Java (AsciiDoc via JRuby) | ✅ Java (pure-Java AsciiDoc available) | ✅ Go, single binary (if AsciiDoc is converted) |
| **Modern Markdown** | ✅ after a one-line config fix (tested) | ✅ built in (extensions: spike) | ✅✅ best: all extensions by default |
| Markdown front matter | ⚠️ `key=value` + `~~~~~~` | ✅ YAML | ✅ YAML/TOML |
| Syntax highlighting (#15) | ⚠️ client-side library | ⚠️ to check | ✅ build-time, no JS |
| Existing AsciiDoc (14 files) | ✅ as-is | ✅ as-is | ⚠️ convert to Markdown (or accept Ruby) |
| Legacy HTML posts (62) | ✅ as-is | ✅ add YAML front matter | ✅ add YAML front matter |
| Keeps all 272 URLs (tags!) | ✅ today | ⚠️ spike | ⚠️ spike, likely some stubs |
| Feed `<guid>`s unchanged | ✅ | ⚠️ spike | ✅ own template |
| Live reload while writing | ⚠️ add-on (watch, refresh) | ✅ dev mode | ✅ `hugo server` |
| Project momentum | ⚠️ slow (3 people/yr) | ✅ active, young, one main maintainer | ✅ very high |
| Upkeep for a rarely-changed site | ✅ low (tested to Gradle 9) | ✅ Java LTS cadence | ✅ low (pin one binary) |
| Migration effort | **none** | L | L (+ AsciiDoc conversion) |

## Decision

> **Accepted by the owner on 2026-10-07: A. Stay on JBake now, and turn on modern Markdown.**
> **Successor order if a trigger fires: Roq (Java) first, then Hugo (Go).**

**Why:**

1. **It meets all three requirements today.**
   - It's Java.
   - With one tested config line it writes modern Markdown well, and the existing AsciiDoc keeps working
     untouched.
   - Every URL is safe by definition.
2. **It works, proven by the revival.**
   - M3 made it build on JDK 21 / Gradle 8 with identical output.
   - This record showed Gradle 9 works too, and that JBake 2.7.0 runs without the dead plugin.
   - The biggest risk ("the plugin dies and strands the site") now has a tested way out.
3. **Every migration risks the hard constraints for gains JBake can mostly match.**
   - Both alternatives need a template rewrite, front-matter conversion of 89 files, and a spike to prove the
     181 tag URLs, feed `<guid>`s, and Giscus paths.
   - Their main gains, live reload and polished Markdown, are reachable on JBake for far less: M7, plus the config
     line, plus a highlighter.
4. **The successor order follows the owner's language order.**
   - **Roq** keeps Java, can keep the AsciiDoc files (pure Java), and has the best Java writing loop. It's two
     years old with one dominant maintainer, so it's not yet the safer choice.
   - **Hugo** is the most mature and has the best Markdown, but it means Go templates and converting all
     AsciiDoc to Markdown.
   - If the owner fully commits to Markdown, Hugo's AsciiDoc drawback disappears. Then Roq vs. Hugo comes down to
     **Java preference against Hugo's maturity**, and the URL spike should decide it.

**Re-evaluate when any of these happens.** Run the Roq spike (T076–T081) first, and a Hugo spike if Roq fails
the [decision criteria](02-ROQ-MIGRATION.md#decision-criteria-go--no-go-after-the-spike):

- JBake or the `JavaExec` route breaks on a new Java LTS or Gradle major, with no small fix.
- A security issue in JBake's embedded dependencies (OrientDB, AsciidoctorJ 2.x, flexmark 0.62) goes unfixed.
- The owner starts writing regularly, and M7's add-on live reload on JBake isn't good enough.
- Roq shows maturity: a 3.x line, a broader maintainer base, and documented support for custom tag URL patterns.

## Consequences

- **M4 closes without a spike.** T034 and CHK024 don't apply. Mark them "not applicable: staying on JBake".
- **M5 (polish) proceeds on JBake and FreeMarker** as written. Keeping template changes small and CSS-first
  limits rework if a migration happens later.
- **Follow-up tasks, added to [the plan](00-REVIVAL.md) on 2026-10-07:**
  - **T094 [IMP] Modern Markdown on JBake** (`src/jbake/jbake.properties`, `README.md`):
    - set the tested `markdown.extensions` line (with or without `SMARTYPANTS`, the owner's call)
    - add a Markdown example to the README's new-post section
    - **Test:** a fixture post with wrapped lines, a table, a task list, a footnote, and fenced code renders
      correctly; `check-urls.sh` passes; no other page changes
  - **T095 [BUG] A missing `summary=` breaks the build with a misleading "Java heap space"** (`index.ftl`): make it
    optional (`${post.summary!""}`) or fail with a clear message.
  - **T096 [IMP] Replace the unmaintained Gradle plugin** with the tested `JavaExec` task, and upgrade to **JBake 2.7.0**:
    - `db.path` change
    - keep `bake` and `bakePreview` as commands
    - this also fixes the misleading error above
    - prove identical output and passing gates
  - **T097 [IMP] Gradle 9** wrapper upgrade (tested compatible on 2026-10-07).
  - **M7 live reload on JBake** (existing T062 and T064): continuous build plus automatic browser refresh. This narrows the alternatives'
    biggest advantage.
  - **Issue #15** (code snippet look, existing T056): add a client-side highlighter for fenced code.
- **AsciiDoc stays supported.** Moving new writing to Markdown is the owner's choice per post, with no
  conversion needed on JBake. Converting old AsciiDoc posts is only needed if Hugo is ever chosen.
- **T098 (Backlog):** a yearly check of the re-evaluation triggers, next due 2027-10.
- [02-ROQ-MIGRATION.md](02-ROQ-MIGRATION.md) stays the ready plan for the first successor. A Hugo plan would be
  written only if Roq's spike fails.
- **What we accept:**
  - A slow-moving upstream.
  - Non-YAML front matter.
  - No built-in live reload until M7.
  - Owning about 20 lines of Gradle glue if the plugin is replaced.

## Sources

Checked 2026-10-07.

- **This repo:**
  - Content and template inventory (`src/jbake/`).
  - `docs/baseline/urls.txt` (272 URLs, 181 tags).
  - Git history (first commit 2014-04-13).
  - Posts: [the-modern-tech-resume.asciidoc](../src/jbake/content/blog/the-modern-tech-resume.asciidoc) (line 69),
    [ruby-broke-my-path.html](../src/jbake/content/blog/ruby-broke-my-path.html),
    [octopress-on-os.html](../src/jbake/content/blog/octopress-on-os.html).
- **Scratch-clone tests (not committed):**
  - Gradle 9.8.0 with plugin 5.5.0: identical output.
  - Plugin 5.5.0 with JBake 2.7.0: fails (`org/apache/commons/configuration/MapConfiguration`).
  - `JavaExec` with `org.jbake:jbake-core:2.7.0`: identical output apart from the version string.
  - Markdown fixture post under the default and the proposed `markdown.extensions`.
  - Missing-`summary` failure (`index.ftl`, "Failed to render masterindex").
- **JBake:**
  - GitHub `jbake-org/jbake` (release v2.7.0, 2025-12-26; commit activity) and `jbake-org/jbake-gradle-plugin`
    (v5.5.0, 2021-05-19).
  - `default.properties` in `jbake-core` 2.6.6 and 2.7.0 (default `markdown.extensions`, `header.separator`).
  - Maven Central `org.jbake:jbake-core` 2.7.0 POM (OrientDB, AsciidoctorJ 2.5.7, FreeMarker 2.3.31,
    flexmark 0.62.2).
- **Roq:** GitHub `quarkiverse/quarkus-roq` (2.1.9, 2026-09-02; contributors; created 2024-05-07).
  https://iamroq.dev. See also [02-ROQ-MIGRATION.md](02-ROQ-MIGRATION.md#sources).
- **Hugo:**
  - GitHub `gohugoio/hugo` (v0.167.0, 2026-09-28).
  - Content formats (Goldmark default; AsciiDoc via the external `asciidoctor` helper):
    https://gohugo.io/content-management/formats/
  - Syntax highlighting: https://gohugo.io/content-management/syntax-highlighting/
- **Eleventy / Build Awesome:**
  - npm `@11ty/eleventy` (3.1.6, 2026-06-02; canary 4.0.0-alpha.10). GitHub `11ty/buildawesome`.
  - https://www.11ty.dev/blog/build-awesome/
  - https://disassociated.com/font-awesome-cans-eleventy-renaming/
- **Astro:** npm `astro` (7.3.6, 2026-10-06). https://www.npmjs.com/package/astro-asciidoc (0.2.0, 2026-09-21).
