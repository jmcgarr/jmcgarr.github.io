# Option: Migrate from JBake to Roq (Quarkus)

**Branch**: `docs/roq-migration-plan` | **Created**: 2026-10-06 | **Status**: Draft (not scheduled: the first successor if a trigger in [01-PLATFORM-DECISION.md](01-PLATFORM-DECISION.md) fires, Accepted 2026-10-07)

This is a plan for **possibly** replacing JBake with [Roq](https://iamroq.dev), the Quarkus static site
generator. The owner isn't ready to switch. This doc lets the M4 platform decision
([T033](00-REVIVAL.md#m4-chart-the-future)) weigh Roq on facts, and makes the switch low-risk if chosen.
Nothing here runs until the owner decides to migrate.

Task IDs continue the revival plan's sequence (T076+) and follow its conventions
([`AGENTS.md`](../AGENTS.md)). Every task has a `Test:` line, and published URLs are permanent (Rule 1).

---

## Why consider Roq

- **Java, like the current stack.** Roq is a set of Quarkus extensions (Apache-2.0, `quarkiverse/quarkus-roq`).
  It's actively released (2.1.9 on 2026-09-02) and **quarkus.io itself moved from Jekyll to Roq**.
- **It removes the pain points behind M3 and M7.** It requires a modern JDK (its GitHub Action defaults to 21),
  so JBake's JDK 1.8 / Gradle 5.6 constraints and the OrientDB `sun.misc.VM` noise (issue #3) go away.
  Quarkus dev mode gives **live reload while writing**, which covers most of issues **#9–#12**. The `roq` CLI has
  `create`/`add`/`blog`/`serve`/`generate` commands, which may cover **#8** (new-post task).
- **Same deploy model as T027.** Roq's official GitHub Action "sets up JDK, builds the Roq site, and uploads the
  artifact … for `actions/deploy-pages`", which is exactly the publishing approach T027 introduces.
- **Plugins for what this site needs:** Tagging (with a `lowercase` option, relevant to T055), Aliases
  (redirects, which matter for URL permanence), Sitemap, Series ("Three Horizons" part 1/2), TOC, AsciiDoc
  (pure-Java or full Asciidoctor via JRuby), Markdown (built in), Sass, and an RSS template built in.

**Costs:** every FreeMarker template is rewritten in **Qute**, all ~89 content files get a new front-matter
format, and the build moves from Gradle to Maven (Roq's docs and Action use `./mvnw`). Roq is also young
(created 2024-05), so expect to read its source now and then.

## Fit assessment

Confidence key: **Docs** = stated in Roq's documentation, **Source** = confirmed in Roq's code, **Spike** = must be proven in Phase 1.

| Requirement (current site) | JBake today | Roq | Confidence |
|---|---|---|---|
| Post URLs `/blog/<name>.html` (74 posts) | path-based | collection link pattern with `:name`/`:ext`, plus per-page `link:` override (default `/:collection/:slug/`) | Docs → **Spike** |
| Page URLs `/about.html`, `/talks.html`, `/speaker-bio.html` | path-based | `site.page-link` default `/:path:ext` | Docs → **Spike** |
| **Tag pages `/tags/<tag>.html`** (181, including spaces and case-only pairs like `DevOps`/`devops`) | `render.tags` | Tagging plugin generates `<collection>/tag/<tag>` with default link `/:collection/`, so **`/posts/tag/<tag>/` by default** | Source → **Spike (highest risk)** |
| `/archive.html`, `/index.html` (6 newest) | templates | content pages + Qute | Docs |
| `/feed.xml`, RSS 2.0, **same `<guid>` values** (`blog/<name>.html`) | `feed.ftl` | built-in `fm/rss` template; a content file named `feed.xml` keeps the URL; guid format to be checked | Source → **Spike** |
| `/sitemap.xml` | `render.sitemap` | Sitemap plugin | Docs → Spike (URL) |
| Drafts never published, previewable locally | publish-time exclude (T009) | `draft:` front matter or a `drafts/` dir, excluded unless `site.draft=true` (enable only in dev) | Docs |
| Markdown | ✓ | built in (CommonMark) | Docs |
| AsciiDoc (~15 posts: image macros with sizes, roles, links with `window=_blank`) | AsciidoctorJ | AsciiDoc plugin (pure Java) or AsciiDoc JRuby (full Asciidoctor) | Docs → **Spike** (pure-Java fidelity) |
| Legacy WordPress HTML (63 posts, CR-only line endings, `key=value` header + `~~~~~~`) | ✓ | HTML with front matter is supported; headers must be converted to YAML | Docs + converter (T079) |
| Custom front matter (`masthead`, `mastheadCredit`, `summary`, `subtitle`) | `content.x` | front matter data available in Qute | Docs |
| Responsive mastheads (960/1440 variants + preload, T071) | script + FreeMarker | reuse the script and port the template logic to Qute | Spike |
| Archived Disqus comments (42 on 10 posts, T045) | generated `.ftl` includes | regenerate as data (`data/` YAML/JSON via Roq Data) or Qute includes from the saved export | Plan (T082) |
| Giscus, GoatCounter, share links, self-hosted fonts (T073), Font Awesome | templates and assets | Qute layout + `public/` assets | Straightforward |
| `/.nojekyll`, `CNAME`, `/fonts/…`, `/img/…`, `/css/…`, `/vendor/…` | `assets/` copied as-is | `public/` served as-is | Docs |
| Redirect stubs (T039 tags, T054 slugs) | hand-made | **Aliases plugin** (`aliases` / `redirect_from` front matter) | Source |
| CI guards (docs, drafts, URL check, link checker, masthead variants) | run on `build/jbake` | same scripts on Roq's output dir | Straightforward |
| Deploy | git-publish → `master` (T027 replaces) | official Roq Action → `actions/deploy-pages` | Source |

## Decision criteria (go / no-go after the spike)

Migrate only if the Phase 1 spike shows **all** of these:
1. **Every URL in `docs/baseline/urls.txt` still resolves** (directly or through an Aliases redirect), including all 181 tag pages.
2. **Feed `<guid>`s unchanged**, so subscribers don't see old posts reappear as new.
3. AsciiDoc posts render with no visible loss against JBake (images, sizes, roles, headings, code).
4. The owner prefers the writing workflow (dev mode, live reload, CLI) enough to justify the template rewrite.

If (1) or (2) fails and can't be fixed cleanly, **stay on JBake** and do M3's T023–T025 instead.

## What changes in the revival plan if Roq is chosen

| Plan item | Effect |
|---|---|
| T023/T024/T025 (JDK, Gradle, JBake upgrades) | **Replaced** by the migration. Issue #3 closes with the cutover PR (T090) |
| T027 (Pages via Actions) | **Kept.** Do it first; Roq's Action plugs into the same deploy job |
| T028 (remove git-publish) | Becomes part of removing JBake/Gradle (T091) |
| T071 masthead variants script | Kept, with the template logic ported to Qute |
| M5 template tasks (T035–T047, T056–T058) | Re-scoped to Qute. Several get easier: T039 and T054 via Aliases, T057 via Tagging, T055 via `lowercase` |
| M7 (T060–T065, issues #8–#12) | Mostly covered by dev mode, live reload, and the `roq` CLI. Re-scope to configure and verify |
| Backlog T069/T075 | Do in the new templates (`<article>` structure, `loading="lazy"`) |

---

## Phase 1: Spike (proves the decision criteria). About 1–2 days

Work on a throwaway branch `spike/roq` in a separate folder (`roq-spike/`), so the JBake site is untouched.

- [ ] T076 [IMP] Scaffold a Roq site (`roq create` or the Quarkus CLI) with the Tagging, Aliases, Sitemap, and AsciiDoc plugins, the base theme, Java 21, and `./mvnw` (`roq-spike/`)
  - **Test:** dev mode serves the site locally with live reload (edit a page, browser refreshes), and the static generation command writes the site to an output folder.
- [ ] T077 [IMP] Port four representative posts plus `about`, `archive`, and `index`: one legacy WordPress HTML post with CR line endings (`sonar`), one AsciiDoc post with sections and images (`three-horizons-part1`), one Markdown post (`relections-and-projections-2019`), and the longest AsciiDoc post (`the-modern-tech-resume`). Use a minimal Qute layout (`roq-spike/content/`)
  - **Test:** each renders at **exactly** its current URL (`/blog/<name>.html`, `/about.html`, `/archive.html`, `/index.html`), checked with `scripts/check-urls.sh` pointed at the spike output with a reduced baseline.
- [ ] T078 [IMP] **Tag URL compatibility:** generate tag pages at `/tags/<tag>.html`, including a tag with a space (`acceptance test`) and a case-only pair (`DevOps`/`devops`). If the Tagging plugin's link can't be set to that pattern, prove the fallback: Aliases redirects (or generated stub pages) at every old tag URL (`roq-spike/`)
  - **Test:** the 181 tag URLs from `docs/baseline/urls.txt` all resolve in the spike output (page or redirect). Document which mechanism worked.
- [ ] T079 [IMP] Write `scripts/jbake-to-roq.py`, which converts JBake headers (`key=value` … `~~~~~~`) to YAML front matter. It maps `date`, `tags`, `status=draft` → `draft: true`, and `summary`/`masthead`/`mastheadCredit`/`subtitle`; it sets `link` where needed; it normalizes CR-only line endings to LF (doing T051 at the same time); and it never changes the body (`scripts/`)
  - **Test:** run it on all ~89 content files. A round-trip check shows every header field preserved and every body byte-identical apart from line endings.
- [ ] T080 [IMP] Feed and sitemap parity: `/feed.xml` (RSS 2.0) and `/sitemap.xml` from Roq (`roq-spike/`)
  - **Test:** for the ported posts, `<guid>` values equal today's (`blog/<name>.html`, `isPermaLink="false"`) and `<link>`s are the same absolute URLs. `xmllint` passes. The W3C validator passes on a spike deploy or a local file.
- [ ] T081 [IMP] AsciiDoc fidelity: render the AsciiDoc posts with the pure-Java plugin, and with the JRuby plugin if needed (`roq-spike/`)
  - **Test:** side-by-side screenshots (JBake vs Roq) show the same images and sizes, roles (`role="right"`), headings, links (`window=_blank`), and code blocks. Record which plugin is required.

**Checkpoint, decide:** record the results in `docs/01-PLATFORM-DECISION.md` (T033) against the decision criteria above. Stop here if the answer is no.

## Phase 2: Content migration

- [ ] T082 [IMP] Convert all content with `jbake-to-roq.py` and move it into Roq's layout: posts → `content/posts/` (link pattern keeps `/blog/<name>.html`), drafts → `content/posts/drafts/` or `draft: true`, pages → `content/`, assets → `public/`. Regenerate the archived Disqus comments from the saved export (`~/Downloads/mikemcgarr-…-all.xml.gz`; **keep that file**) as Roq data or Qute includes (`content/`, `public/`, `data/`)
  - **Test:** file counts match (74 posts, 12 drafts, 3 pages). `scripts/check-urls.sh` on the Roq output reports 0 missing. 42 archived comments appear on the same 10 posts.

## Phase 3: Templates and features (Qute)

- [ ] T083 [IMP] Port the Clean Blog layout to Qute: head (meta, `fonts.css` + preload, Bootstrap, Font Awesome, site CSS), nav, **responsive masthead with preloads** (T071 logic), footer (social links, license, GoatCounter) (`templates/layouts/`)
  - **Test:** pixel comparison of the nav, masthead, and footer against the live site at 1280px and 500px shows no differences beyond documented ones.
- [ ] T084 [IMP] Port the page types: post (share links, archived comments, Giscus with `pathname` mapping unchanged), page, index (6 newest with summaries), archive (grouped by month), tag pages (`templates/`)
  - **Test:** the four baseline pages and one tag page look the same as live (screenshot comparison). Giscus loads the **same** discussion thread for a post (same `pathname` term).

## Phase 4: Build and CI

- [ ] T085 [IMP] Build with the official Roq GitHub Action (Java 21) feeding `actions/deploy-pages` (T027's deploy job), and point every guard at Roq's output: docs guard, drafts check (no draft pages in the artifact), `check-urls.sh`, `xmllint`, lychee, masthead variants (`.github/workflows/`)
  - **Test:** a PR run passes every gate without deploying. Negative tests: a draft in the artifact fails, and a missing image fails.

## Phase 5: Parity verification (before cutover)

- [ ] T086 [IMP] Full parity check of Roq output against the live JBake site: all 272 baseline URLs (pages or redirects), all image/font/CSS URLs, feed `<guid>`s for all items, sitemap URL set, `.nojekyll`/`CNAME` as needed (`docs/reports/roq-parity-<date>.md`)
  - **Test:** the report shows 0 missing URLs, an identical guid set, and every intentional difference listed and approved by the owner.
- [ ] T087 [IMP] Performance and accessibility no worse than "After M2" in `docs/baseline/metrics.md` (`docs/reports/`)
  - **Test:** Lighthouse mobile (median of 5) on the four baseline pages ≥ the After-M2 numbers (96/93/96/81), with accessibility, best practices, and SEO not lower.

## Phase 6: Cutover and rollback

- [ ] T088 [IMP] Tag the last JBake source (`jbake-final`) and the last JBake-built deploy, so rollback is one redeploy away (git tags)
  - **Test:** both tags exist on the remote, and re-running the tagged deploy workflow reproduces the JBake site.
- [ ] T089 [IMP] Cut over: merge the Roq site into the default branch in one PR, so the next deploy is Roq (`main`/`source`)
  - **Test:** the live sweep (URLs, assets, feed, sitemap, Giscus, GoatCounter) passes. If anything fails, roll back by redeploying `jbake-final` (T088), then fix forward.
- [ ] T090 [IMP] Close the issues the migration resolves, with `Closes #N` lines in the cutover PR: #3 (JDK/OrientDB, JBake removed), and #9/#10/#11 if dev mode and live reload are verified (GitHub issues)
  - **Issue:** Closes #3 (and #9, #10, #11 if verified)
  - **Test:** those issues show as closed by the merged PR.

## Phase 7: Cleanup

- [ ] T091 [IMP] Remove JBake and Gradle (`build.gradle`, `settings.gradle`, the wrapper, `src/jbake/`, the git-publish config if still present), and update `AGENTS.md`, `README.md`, and the plan for the Roq layout, commands, and URL rules (repo root)
  - **Test:** a fresh clone builds and previews with the README's new commands. No `jbake`/`gradle` references remain outside history and docs.

## Open questions for the owner

1. **Theme:** port Clean Blog as-is (lowest risk, same look), or adopt Roq's default theme (Tailwind, dark mode) as a redesign? This plan assumes **port as-is**.
2. **Gradle vs Maven:** Roq's docs and Action use Maven. This plan assumes switching to Maven.
3. **Timing relative to M3:** do **T027 first** (it applies either way), and skip T023–T025 if the decision is Roq.

## Sources

- Roq documentation: https://docs.quarkiverse.io/quarkus-roq/dev/ (configuration: links, drafts, themes)
- Roq marketplace (plugins and themes): https://iamroq.dev/marketplace/
- "The quarkus.io site is now built with Quarkus Roq": https://quarkus.io/blog/jekyll-to-roq/
- Source checked 2026-10-06 in `quarkiverse/quarkus-roq` (release 2.1.9): `action.yml` (GitHub Action, JDK 21 default, uploads the Pages artifact), `roq-plugin/tagging` (`/:collection/` link template, `<collection>/tag/<tag>`, `lowercase` option), `roq-plugin/aliases` (`aliases` / `redirect_from` front matter), `roq-frontmatter/.../templates/fm/rss.html` (built-in RSS), `roq-cli` (create/add/blog/serve/generate/start/update commands).
