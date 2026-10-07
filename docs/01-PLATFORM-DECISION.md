# Platform Decision: Stay on JBake or Migrate

**Branch**: `docs/T033-platform-decision` | **Created**: 2026-10-07 | **Status**: Draft (proposed decision below, awaiting the owner)

Revival task T033 (M4). Compares staying on JBake with four alternatives: **Roq**, **Hugo**, **Eleventy**, and
**Astro**. Every fact below about this site was measured in the repo on 2026-10-07. Facts about the tools
come from their release pages, registries, and source on the same date (see [Sources](#sources)).

## Context

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
| AsciiDoc (`.asciidoc`) | **14**: 11 posts (2014–2020) + About, Talks, Speaker Bio | **The owner's format of choice since 2014.** Light usage: images (9), links (14), lists, quotes, two listing blocks, one passthrough. **No** source highlighting, includes, attributes, tables, or admonitions |
| Markdown (`.md`) | **1** (2019) | |
| Drafts | 12 (10 HTML, 2 AsciiDoc) | Preview only, never published (T009) |
| Front matter | `key=value` + `~~~~~~` | `title`, `date`, `type`, `status`, `tags` on all. Custom: `summary` (15), `masthead` (12), `mastheadCredit` (10), `subtitle` (2) |
| Templates | **12 FreeMarker files, 415 lines** | Plus 10 generated archived-comment includes (T045). Uses only `published_posts`, `posts`, `tag_posts`, `published_pages`, `content.*`, `config.*` |
| JBake features used | tags, sitemap, feed, archive, drafts | **Not used:** pagination, tag index, data files, other template engines. A small surface, so it's easy to keep *and* easy to port |
| Build | Gradle plugin `org.jbake.site` 5.5.0 | Custom tasks: `preview` (jwebserver, T024) and the old `liveEdit` |

### What any platform must preserve

These are the hard constraints from [AGENTS.md](../AGENTS.md) Rule 1. Any migration must prove it meets them:

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
  - **It can't run JBake 2.7.0** (a missing `commons-configuration` class, tested 2026-10-07).

**Tested 2026-10-07 for this record, in a scratch clone (not committed):**
- The current plugin setup **also works on Gradle 9.8.0**, with output byte-identical to Gradle 8.14.5. So the
  next Gradle major doesn't break the site.
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

### A. Stay on JBake

- **Java fit:** ✅ It's the incumbent. Java, Gradle, AsciidoctorJ, FreeMarker.
- **Fit:** ✅ Everything works today and every gate passes. Nothing to port.
- **Risks:**
  - The Gradle plugin is unmaintained. That's mitigated: the `JavaExec` route above is tested.
  - JBake core is slow-moving, and it's single-maintainer-ish.
  - It still embeds OrientDB, and its AsciidoctorJ is a major version behind (2.5.x).
- **Writing loop:** no built-in live reload.
  - JBake's own CLI can bake, serve, and watch (`-b -s`).
  - Gradle continuous build (`./gradlew -t bake`) plus a browser refresh, or a small live-reload script,
    covers M7.
- **Effort:** none now. Optional hardening is small (see Consequences).

### B. Roq (Quarkus)

Full assessment and phased plan: [02-ROQ-MIGRATION.md](02-ROQ-MIGRATION.md).

- **Java fit:** ✅ Java 21 and Quarkus. Templates in **Qute**, built with **Maven**, which is Roq's documented path.
- **Health:** active. Release 2.1.9 came out 2026-09-02, with 569 commits by about 25 authors in the past year.
  quarkus.io itself runs on it. But it's **young** (created 2024-05), and one maintainer wrote most of the code
  (about 674 commits, against 68 for the next contributor).
- **Fit:**
  - Markdown is built in. AsciiDoc comes through a plugin, either pure-Java or full Asciidoctor via JRuby.
  - HTML posts with front matter are supported.
  - Plugins exist for tagging, aliases (redirect stubs), sitemap, series, and RSS.
  - **Biggest risk: tag URLs.** The tagging plugin's default pattern is `/posts/tag/<tag>/`. Reproducing
    `/tags/<Tag With Spaces>.html`, the case pairs, and `.NET` needs a spike (T076–T081).
  - Feed `<guid>` format also needs checking.
- **Writing loop:** ✅ the best of the five for a Java developer. Quarkus dev mode gives **live reload**, and the
  `roq` CLI scaffolds posts.
- **Effort:** **L.**
  - Rewrite 415 lines of FreeMarker in Qute.
  - Convert front matter on 89 files.
  - Move the build to Maven.
  - Port the mastheads, archived comments, and feed.
  - Do the URL spike and parity checks.
  - Phases 1–7 of the Roq plan.

### C. Hugo

- **Java fit:** ❌ Go. Templates are Go `html/template`.
- **Health:** ✅ very active (v0.167.0 on 2026-09-28) and the largest community. It's still pre-1.0, and
  releases often carry breaking changes, so pin the version.
- **Fit:**
  - It's a single binary, and fast.
  - It supports `.html` and Markdown content files.
  - **AsciiDoc is the catch:** Hugo calls the external **Ruby `asciidoctor`** program, which must be installed on
    every machine and in CI. That brings back the Ruby toolchain from 2012.
  - Tag URLs are normalized by default: lowercased, with spaces turned into hyphens. Keeping the 48 spaced and
    9 capitalized tag URLs exact needs non-default settings or redirect stubs. Spike needed.
  - Post URLs as `/blog/<name>.html` are configurable (`uglyURLs` / permalinks).
- **Writing loop:** ✅ excellent live reload (`hugo server`).
- **Effort:** **L.**
  - Rewrite the templates in Go templates.
  - Convert front matter.
  - Install Ruby Asciidoctor locally and in CI.
  - Prove the tag URLs.

### D. Eleventy (being renamed "Build Awesome")

- **Java fit:** ❌ JavaScript on Node and npm.
- **Health:** the stable release is 3.1.6 (2026-06-02), and 4.0 is in alpha.
  - Font Awesome acquired the project in 2024. In **March 2026** it announced a rename to
    **Build Awesome**, and the repo is now `11ty/buildawesome`.
  - The rename and a funding campaign were then **paused after community backlash**, so its direction is
    unsettled.
- **Fit:**
  - ✅ **The best URL control of any option.** Permalinks are fully programmable, so every URL, including
    tags with spaces, the case pairs, and `.NET`, can be reproduced exactly.
  - AsciiDoc comes through `eleventy-plugin-asciidoc`: Asciidoctor.js, v6.1.3 (2026-08-25), one maintainer.
  - Legacy `.html` files are run through Liquid by default, which must be turned off for them.
  - Nunjucks templates are close in style to FreeMarker.
- **Writing loop:** ✅ live reload (`--serve`).
- **Effort:** **M–L.**
  - Rewrite the templates (Nunjucks).
  - Convert front matter.
  - Add an npm toolchain and Dependabot npm updates.

### E. Astro

- **Java fit:** ❌ JavaScript and TypeScript, Vite, a component framework.
- **Health:** ✅ very active (7.3.6 on 2026-10-06). But it has had **six major versions since 1.0 (2022)**,
  which means a regular upgrade treadmill for a site that changes rarely.
- **Fit:**
  - Markdown and MDX are first-class.
  - **AsciiDoc and raw-HTML posts need community loaders.** `astro-asciidoc` is at 0.2.0, pre-1.0.
  - `build.format: 'file'` gives `/blog/<name>.html`. Tag pages come from `getStaticPaths`, and spaced tag
    names need a spike.
  - Much of its power (islands, components, server rendering) is irrelevant to this blog.
- **Writing loop:** ✅ excellent (Vite dev server).
- **Effort:** **L.**
  - Rewrite every template as `.astro` components.
  - Add loaders for HTML and AsciiDoc.
  - Convert front matter.
  - Add an npm toolchain.

### Comparison

| Criterion | A. JBake | B. Roq | C. Hugo | D. Eleventy | E. Astro |
|---|---|---|---|---|---|
| **Java ecosystem** (owner's preference) | ✅ Java | ✅ Java | ❌ Go | ❌ JS | ❌ JS/TS |
| Legacy HTML posts (62) | ✅ as-is | ✅ needs YAML front matter | ✅ needs YAML front matter | ⚠️ turn off Liquid | ⚠️ custom loader |
| AsciiDoc (owner's format) | ✅ AsciidoctorJ | ✅ plugin (Java or JRuby) | ⚠️ external **Ruby** gem | ✅ Asciidoctor.js plugin | ⚠️ pre-1.0 loader |
| Keeps all 272 URLs (tags!) | ✅ today | ⚠️ spike | ⚠️ spike, likely stubs | ✅ programmable | ⚠️ spike |
| Feed `<guid>`s unchanged | ✅ | ⚠️ spike | ✅ own template | ✅ own template | ✅ own template |
| Live reload while writing | ⚠️ add-on (watch, refresh) | ✅ dev mode | ✅ | ✅ | ✅ |
| Project momentum | ⚠️ slow (3 people/yr) | ✅ active, young, one main maintainer | ✅ very high | ⚠️ unsettled rebrand | ✅ high, frequent majors |
| Upkeep for a rarely-changed site | ✅ low (tested to Gradle 9) | ✅ Java LTS cadence | ✅ low (pin a binary) | ⚠️ npm churn | ❌ major-version churn |
| Migration effort | **none** | L | L | M–L | L |

## Decision

> **Proposed (awaiting the owner): A. Stay on JBake, and name Roq as the planned successor.**
>
> Revisit when a trigger below fires. If it's accepted, set **Status: Accepted** and tick T033 and CHK023.

**Why:**

1. **It matches the owner's preference and usage.**
   - JBake is Java, and AsciiDoc through AsciidoctorJ is a first-class path, not a bolt-on.
   - Of the alternatives, only Roq is also Java. Hugo would bring back the Ruby toolchain just for AsciiDoc.
2. **It works today, proven by the revival.**
   - M3 made it build on JDK 21 / Gradle 8 with identical output.
   - This record showed Gradle 9 works too, and that JBake 2.7.0 runs without the dead plugin.
   - The biggest risk ("the plugin dies and strands the site") now has a tested way out.
3. **Every migration risks the hard constraints for little gain.**
   - Each option needs a template rewrite, front-matter conversion of 89 files, and a spike to prove the
     181 tag URLs, feed `<guid>`s, and Giscus paths.
   - The main gain, live reload, can be added to JBake (M7) for far less.
4. **Roq is the right successor, just not yet.** It keeps Java, has the best writing loop of the Java options,
   and has an alias plugin for redirect stubs. But it's two years old with one dominant maintainer. Waiting
   costs nothing, because the content (HTML, AsciiDoc, Markdown) moves just as easily later.

**Not chosen:**
- **Hugo:** Go templates, and AsciiDoc needs Ruby.
- **Eleventy:** the best URL fidelity, but a JS toolchain and an unsettled rebrand.
- **Astro:** a heavyweight component framework with major-version churn, for a site that changes a few times
  a year.

**Re-evaluate (and probably start the Roq spike, T076–T081) when any of these happens:**

- JBake or the `JavaExec` route breaks on a new Java LTS or Gradle major, with no small fix.
- A security issue in JBake's embedded dependencies (OrientDB, AsciidoctorJ 2.x) goes unfixed.
- The owner starts writing regularly, and M7's add-on live reload on JBake isn't good enough.
- Roq shows maturity: a 3.x line, a broader maintainer base, and documented support for custom tag URL patterns.

## Consequences

- **M4 closes without a spike.** T034 and CHK024 don't apply. Mark them "not applicable: staying on JBake".
- **M5 (polish) proceeds on JBake and FreeMarker** as written. Template work in M5 would have to be redone
  in a later migration. Keeping changes small and CSS-first limits that.
- **Proposed follow-up tasks, to add to M5 or M7 when this is accepted:**
  - *Replace the unmaintained Gradle plugin* with the tested `JavaExec` task, and upgrade to **JBake 2.7.0**:
    - `db.path` change
    - keep `bake` and `bakePreview` as commands
    - prove identical output and passing gates
  - *Gradle 9* wrapper upgrade (tested compatible on 2026-10-07).
  - *M7 live reload on JBake:* continuous build plus automatic browser refresh. This narrows Roq's biggest
    advantage.
- [02-ROQ-MIGRATION.md](02-ROQ-MIGRATION.md) stays the ready plan, and its decision criteria still hold. If a
  trigger fires, start at its Phase 1.
- **What we accept:**
  - A slow-moving upstream.
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
- **Scratch-clone experiments (not committed):**
  - Gradle 9.8.0 with plugin 5.5.0: identical output.
  - Plugin 5.5.0 with JBake 2.7.0: fails (`org/apache/commons/configuration/MapConfiguration`).
  - `JavaExec` with `org.jbake:jbake-core:2.7.0`: identical output apart from the version string.
- **JBake:** GitHub `jbake-org/jbake` (release v2.7.0, 2025-12-26; commit activity) and
  `jbake-org/jbake-gradle-plugin` (v5.5.0, 2021-05-19). Maven Central `org.jbake:jbake-core` 2.7.0 POM
  (OrientDB, AsciidoctorJ 2.5.7, FreeMarker 2.3.31).
- **Roq:** GitHub `quarkiverse/quarkus-roq` (2.1.9, 2026-09-02; contributors; created 2024-05-07).
  https://iamroq.dev. See also [02-ROQ-MIGRATION.md](02-ROQ-MIGRATION.md#sources).
- **Hugo:** GitHub `gohugoio/hugo` (v0.167.0, 2026-09-28). AsciiDoc via the external `asciidoctor` helper:
  https://gohugo.io/content-management/formats/
- **Eleventy / Build Awesome:**
  - npm `@11ty/eleventy` (3.1.6, 2026-06-02; canary 4.0.0-alpha.10). GitHub `11ty/buildawesome`.
  - https://www.11ty.dev/blog/build-awesome/
  - https://disassociated.com/font-awesome-cans-eleventy-renaming/
  - https://github.com/saneef/eleventy-plugin-asciidoc (npm 6.1.3, 2026-08-25).
- **Astro:** npm `astro` (7.3.6, 2026-10-06). https://www.npmjs.com/package/astro-asciidoc (0.2.0, 2026-09-21).
