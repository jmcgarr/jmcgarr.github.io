# Site Revival Plan: mikemcgarr.com

**Branch**: `docs/site-revival` | **Created**: 2026-10-04 | **Status**: Draft
**Input**: Repository review of 2026-10-03 (JBake 2.6 / Gradle 5.6.4 / Clean Blog theme)

This plan brings the site back to a healthy state over seven milestones. Each milestone has one
theme and can be tested on its own, so you can ship it to the live site before starting the next.

---

## Conventions

Task format (adapted from Spec Kit `tasks.md`):

```
- [ ] T000 [P] [BUG|IMP] Description (`path/to/file`)
  - **Test:** how to prove the task is done
```

- **T000**: Task ID, unique across the whole plan. IDs are never renumbered or reused; tasks added later take the next free ID (see `AGENTS.md`).
- **[P]**: Can run in parallel. It touches different files from the other `[P]` tasks in the same section and doesn't depend on them.
- **[BUG]**: Something that is broken, wrong, or publishing something it shouldn't.
- **[IMP]**: Improvement. The current behavior works, but it could be faster, cleaner, safer, or easier to maintain.
- **Test:** Every task has a concrete check: a command, a URL, or a manual step with a clear pass/fail result.
- **CHK000**: Checkpoint items (Spec Kit checklist style). A milestone is done only when all of its CHK items are checked.

### Definition of Done (applies to every task)

- [ ] `./gradlew clean bake` succeeds with no new warnings
- [ ] `./gradlew bakePreview` renders correctly at http://localhost:8080 (home, one post, About, Archive)
- [ ] `scripts/check-urls.sh` reports no **unexpected** missing URLs (see T006)
- [ ] `scripts/check-docs-not-published.sh` passes (see T004)

### Common test commands

```sh
./gradlew clean bake                                  # build into build/jbake
./gradlew clean bakePreview                           # preview at http://localhost:8080
xmllint --noout build/jbake/sitemap.xml build/jbake/feed.xml
lychee --offline --no-progress build/jbake            # missing local files/assets
lychee --no-progress build/jbake                      # all links, including external
vnu --skip-non-html build/jbake                       # HTML validation
npx lighthouse https://www.mikemcgarr.com --view      # perf / a11y / SEO scores
du -sh build/jbake                                    # published site size
```

---

## Milestone Overview

| #  | Milestone              | Theme                                                    | Bugs | Improvements |
|----|------------------------|----------------------------------------------------------|------|--------------|
| M0 | Baseline & Guardrails  | Know exactly what's live; make change safe               | 0    | 6            |
| M1 | Stop the Bleeding      | Everything the site does, it does correctly              | 8    | 1            |
| M2 | Lighten the Load       | Fast on a phone; site shrinks ~90%                       | 2    | 5            |
| M3 | Modern Toolchain       | Builds anywhere, deploys safely, no personal tokens      | 1    | 9            |
| M4 | Chart the Future       | Decide the platform before investing in polish           | 0    | 2            |
| M5 | Polished Presentation  | Valid HTML, discoverable, shares well                    | 6    | 7            |
| M6 | Content Care           | Accurate content; every link and image works             | 3    | 4            |

### Dependencies & Order

- **M0 blocks everything.** You need the baseline and guardrails before changing anything.
- **M1** comes next because it fixes things that are wrong on the live site right now.
- **M2 and M3** don't depend on each other and can run in either order or overlap.
- **M4 is a decision gate.** It should finish before **M5**, because M5 is template work for the current JBake platform and would be thrown away if the site migrates.
- **M6** depends only on M0 and can be picked up at any time. It's good work for spare evenings.

---

## M0: Baseline & Guardrails

**Goal**: Snapshot exactly what's live today and add the safety checks that every later milestone uses.

**Independent Test**: On an unmodified `source` checkout, `./gradlew clean bake` followed by both
check scripts reports **zero** differences from the live site, and no planning docs appear in the build.

### Bugs

_None. This milestone is groundwork._

### Improvements

- [ ] T001 [IMP] Tag the currently-live site so it can always be compared or restored: `git tag live-2026-10 origin/master && git push origin live-2026-10`
  - **Test:** `git ls-remote --tags origin live-2026-10` returns a ref that points at the current `origin/master` commit.
- [ ] T002 [P] [IMP] Record the URL inventory of the live site (`docs/baseline/urls.txt`): `git ls-tree -r --name-only live-2026-10 | grep '\.html$' | sort > docs/baseline/urls.txt`
  - **Test:** `wc -l docs/baseline/urls.txt` equals the number of `.html` files in `live-2026-10`.
- [ ] T003 [P] [IMP] Record baseline metrics (`docs/baseline/metrics.md`): published size (`du -sh` of a `live-2026-10` checkout, about 125 MB), Lighthouse mobile scores (Performance, Accessibility, Best Practices, SEO) for `/`, `/about.html`, `/archive.html`, and one post, plus the count of `http://` links in content (559)
  - **Test:** The file has all four pages × four scores, the site size, and the link count, with the date measured.
- [ ] T004 [IMP] **Guarantee `docs/` is never published.** Verify that the bake root is `src/jbake` and that `gitPublish.contents` copies only `build/jbake`, and confirm that GitHub Pages is **not** set to "Deploy from branch → `source` → `/docs`". Then add `scripts/check-docs-not-published.sh`, which fails if `build/jbake` contains a `docs/` directory, any `*.md` file, or the string `REVIVAL`
  - **Test:**
    - `./gradlew clean bake && scripts/check-docs-not-published.sh` exits 0.
    - `gh api repos/jmcgarr/jmcgarr.github.io/pages --jq '.source'` returns `{"branch":"master","path":"/"}`.
    - After the next publish, `curl -s -o /dev/null -w '%{http_code}' https://www.mikemcgarr.com/docs/00-REVIVAL.md` returns `404`.
    - Negative test: `mkdir build/jbake/docs && scripts/check-docs-not-published.sh` exits non-zero.
- [ ] T005 [IMP] Run the docs guard in CI as a step after "Bake with Gradle" and before "Publish content" (`.github/workflows/gradle.yml`)
  - **Test:** The Actions log shows the guard step passing. On a throwaway branch, temporarily copying `docs/` into `build/jbake` makes the workflow fail **before** publishing.
- [ ] T006 [IMP] Add `scripts/check-urls.sh`. It diffs the `.html` files in `build/jbake` against `docs/baseline/urls.txt`, prints any **missing** and **new** URLs, and takes an optional allowlist of expected removals (`docs/baseline/expected-removals.txt`)
  - **Test:** On a clean bake of unmodified `source`, it reports 0 missing. Deleting one file in `build/jbake/blog/` makes it exit non-zero and name that file.

### Checkpoint: M0

- [ ] CHK001 Tag `live-2026-10` exists on the remote
- [ ] CHK002 `docs/baseline/urls.txt` and `docs/baseline/metrics.md` are committed
- [ ] CHK003 The docs guard passes locally **and** runs in CI before the publish step
- [ ] CHK004 `scripts/check-urls.sh` reports 0 missing URLs on an unmodified clean bake
- [ ] CHK005 `https://www.mikemcgarr.com/docs/00-REVIVAL.md` returns 404

---

## M1: Stop the Bleeding

**Goal**: Everything the site does today, it does correctly. Nothing private or broken gets published.

**Independent Test**: The sitemap and feed validate. No drafts, dead widgets, or plain-HTTP self-links
are in the build. A pull request can no longer publish to the live site.

### Bugs

- [ ] T007 [BUG] Fix the missing `/` between host and path in the sitemap and feed URLs. They currently render as `http://www.mikemcgarr.comblog/...` (`src/jbake/jbake.properties`, `src/jbake/templates/sitemap.ftl`, `src/jbake/templates/feed.ftl`)
  - **Test:** `grep -cE 'mikemcgarr\.com[a-z]' build/jbake/sitemap.xml build/jbake/feed.xml` returns `0` for both files. `xmllint --noout` passes. The feed validates at https://validator.w3.org/feed/. Google Search Console accepts the sitemap with 0 errors.
- [ ] T008 [BUG] Run the publish step only on `push`, not on `pull_request`. Today a same-repo PR would deploy unmerged content (`.github/workflows/gradle.yml`, add `if: github.event_name == 'push'`)
  - **Test:** Open a throwaway PR. The Actions log shows "Publish content" as **skipped**, and `git ls-remote origin master` is unchanged before and after.
- [ ] T009 [BUG] Stop publishing drafts. All 12 are live under `/blog/drafts/*-draft.html`. First check whether the JBake version in use can skip rendering drafts. If it can't, move `src/jbake/content/blog/drafts/` outside the bake root (for example `drafts/` at the repo root)
  - **Test:** `find build/jbake -name '*-draft.html'` is empty. After the next publish, `curl -sI https://www.mikemcgarr.com/blog/drafts/specflow-selenium-draft.html` returns `404`. Add the draft URLs to `docs/baseline/expected-removals.txt` so `check-urls.sh` passes.
- [ ] T010 [BUG] Serve the site over HTTPS everywhere. Set `site.host=https://www.mikemcgarr.com`, remove the hardcoded `http://www.mikemcgarr.com/` share URLs, and turn on **Enforce HTTPS** in the Pages settings (`src/jbake/jbake.properties`, `src/jbake/templates/post.ftl`)
  - **Test:** `grep -r 'http://www.mikemcgarr.com' build/jbake` returns nothing. `curl -sI http://www.mikemcgarr.com/` returns `301` to `https://`. `gh api repos/jmcgarr/jmcgarr.github.io/pages --jq .https_enforced` returns `true`.
- [ ] T011 [P] [BUG] Remove the dead social widgets: Google+ (`g-plusone`, `apis.google.com/js/platform.js`), LinkedIn `in.js` and `IN/Share`, and the Facebook `fb-like` div, which has no SDK loaded (`src/jbake/templates/header.ftl`, `src/jbake/templates/footer.ftl`, `src/jbake/templates/post.ftl`)
  - **Test:** `grep -rE 'apis.google.com|g-plusone|platform.linkedin.com|IN/Share|fb-like' build/jbake` returns nothing. The browser console on a post page shows no errors from third-party scripts.
- [ ] T012 [P] [BUG] Fix the French Twitter button (`data-lang="fr"`, label "Tweeter"). Replace the share bar with plain share links that need no third-party JS (X, LinkedIn, Bluesky, email) (`src/jbake/templates/post.ftl`)
  - **Test:** `grep -rE 'data-lang="fr"|Tweeter' build/jbake` returns nothing. In preview, each share link opens a pre-filled composer with the post's `https://` URL.
- [ ] T013 [P] [BUG] Remove the dead Universal Analytics snippet (`ga.js`, `UA-49993013-1`), which stopped collecting data in July 2023. Decide whether to replace it with GA4, Plausible, GoatCounter, or nothing (`src/jbake/templates/header.ftl`)
  - **Test:** `grep -rE 'ga\.js|UA-49993013' build/jbake` returns nothing. If you pick a replacement, its real-time dashboard records a visit to the live site after publish.
- [ ] T014 [P] [BUG] Fix the README. Remove the dead Travis badge, change the publish command from `bake publish` to the real task (`gitPublishPush`, or whatever M3 replaces it with), and document the preview steps (`README.md`)
  - **Test:** Follow the README step by step from a fresh clone. The preview works at http://localhost:8080 and the badge renders.

### Improvements

- [ ] T015 [IMP] Add a GitHub Actions status badge to the README (`README.md`)
  - **Test:** The badge renders on the GitHub repo page and reflects the latest `source` run.

### Checkpoint: M1

- [ ] CHK006 The sitemap is accepted in Search Console and the feed passes the W3C validator
- [ ] CHK007 No drafts are reachable on the live site
- [ ] CHK008 HTTPS is enforced, and the build has no `http://www.mikemcgarr.com` self-links
- [ ] CHK009 Home, a post, About, and Archive load with **zero** console errors
- [ ] CHK010 A test PR ran CI without publishing
- [ ] CHK011 `check-urls.sh` reports only the expected draft removals

---

## M2: Lighten the Load

**Goal**: A first visit is fast on a phone. No page ships more than ~1 MB of images, and the
published site shrinks from ~130 MB to ≤ 15 MB.

**Independent Test**: `du -sh build/jbake` ≤ 15 MB. Lighthouse mobile Performance ≥ 90 on the four
baseline pages. An offline link check finds no missing assets.

### Bugs

- [ ] T016 [BUG] Re-encode the oversized masthead backgrounds. Pages currently download 13–16 MB hero images: `hawaii.png`, `london-alley.png`, `london-view.png`, `Los-Gatos_02.jpg`, plus anything else over 500 KB. Target ≤ 1920px wide, JPEG or WebP, ≤ 400 KB. If an extension changes, update the `masthead=` front matter and `archive.ftl` (`src/jbake/assets/img/masthead/`)
  - **Test:** `find src/jbake/assets/img/masthead -size +500k` is empty. A side-by-side visual check in preview looks right. Lighthouse LCP on `/about.html` and `/archive.html` improves against `docs/baseline/metrics.md`.
- [ ] T017 [P] [BUG] Re-encode oversized inline post images: `mike-oscon-1.png` (8 MB), `mike-oscoon-2.jpg` (3 MB), `three-horizons-lean-enterprise.png`, the `qcon-*.png` files, and anything else over 500 KB (`src/jbake/assets/img/`)
  - **Test:** `find src/jbake/assets/img -maxdepth 1 -size +500k` is empty, or lists only exceptions you've documented. The affected posts render correctly in preview.

### Improvements

- [ ] T018 [P] [IMP] Delete the ~24 unused images (~25 MB), for example `masthead/santa-cruz.png`, the unused `me_qcon_*.png` files, `bg-*.png`, `glyphicons-*`, and `webicon-*.svg` (`src/jbake/assets/img/`)
  - **Test:** Re-running the unused-image scan (every image basename grepped against content, templates, and CSS) returns nothing. `lychee --offline build/jbake` reports 0 missing local files.
- [ ] T019 [P] [IMP] Cut Font Awesome (13 MB) down to `css/all.min.css` and `webfonts/`, or replace it with inline SVGs for the handful of icons in use (footer circles, Twitter, LinkedIn, GitHub, menu bars) (`src/jbake/assets/vendor/fontawesome-free/`)
  - **Test:** The footer and menu icons render in preview. `du -sh src/jbake/assets/vendor/fontawesome-free` is under 1 MB, or the directory is gone. Offline lychee is clean.
- [ ] T020 [P] [IMP] Remove unused JS and CSS: unminified and slim jQuery/Bootstrap variants, `*.map` files, `contact_me*.js`, and `jqBootstrapValidation*.js`. Also stop publishing `scss/` by moving the SCSS sources out of `assets/` (`src/jbake/assets/vendor/`, `src/jbake/assets/js/`, `src/jbake/assets/scss/`)
  - **Test:** At mobile width the navbar collapse and toggle work. On desktop the scroll-up navbar reveal works. `test ! -e build/jbake/scss` passes. Offline lychee is clean.
- [ ] T021 [P] [IMP] Limit the RSS feed to the 20 most recent posts. It's currently 440 KB with all 75 (`src/jbake/templates/feed.ftl`)
  - **Test:** `grep -c '<item>' build/jbake/feed.xml` returns `20`. The file is under 150 KB and still validates.
- [ ] T022 [IMP] Self-host Lora and Open Sans, or drop them for the system font stack (this continues the May 2024 font changes) (`src/jbake/templates/header.ftl`, `src/jbake/assets/css/clean-blog.css`)
  - **Test:** The DevTools Network tab shows no requests to `fonts.googleapis.com` or `fonts.gstatic.com`. Headings and body text look right on home and on a post.

### Checkpoint: M2

- [ ] CHK012 `du -sh build/jbake` ≤ 15 MB (baseline ~130 MB)
- [ ] CHK013 Lighthouse mobile Performance ≥ 90 on `/`, `/about.html`, `/archive.html`, and one post (recorded in `docs/baseline/metrics.md`)
- [ ] CHK014 `lychee --offline build/jbake` reports 0 missing local assets
- [ ] CHK015 No visual regressions on the four baseline pages at mobile and desktop widths
- [ ] CHK016 `check-urls.sh` reports 0 unexpected missing HTML URLs

---

## M3: Modern Toolchain

**Goal**: Any machine, including Apple Silicon with a current LTS JDK, builds the site with one command.
Deploys happen only from `source` and need no personal tokens.

**Independent Test**: A fresh clone on Apple Silicon with JDK 21 runs `./gradlew clean bake` successfully.
CI is green. Its output is identical to the pre-upgrade build, and deploying needs no repo secrets.

### Bugs

- [ ] T023 [BUG] Make local and CI use the same JDK. Today `.java-version` is `1.8` and CI uses `11`. After T024/T025, standardize on JDK 21 (LTS) (`.java-version`, `.github/workflows/gradle.yml`)
  - **Test:** In the repo, `java -version` reports 21. The workflow has `java-version: '21'`. The bake succeeds locally and in CI.

### Improvements

- [ ] T024 [IMP] Upgrade the Gradle wrapper from 5.6.4 to the latest 8.x, going through 6.9 and 7.6 and fixing deprecations at each step (`gradle/wrapper/gradle-wrapper.properties`, `build.gradle`)
  - **Test:** `./gradlew --version` reports 8.x. `./gradlew clean bake --warning-mode all` has no deprecation warnings. **Output-equivalence check:** before upgrading, copy a clean bake to `/tmp/bake-before`. After upgrading, `diff -r /tmp/bake-before build/jbake` is empty, or every difference is explained.
- [ ] T025 [IMP] Upgrade `org.jbake.site` from 5.0.0 to the latest 5.x, along with its bundled JBake version (`build.gradle`)
  - **Test:** The same output-equivalence `diff -r` as T024. Tags, archive, feed, and sitemap are all present.
- [ ] T026 [P] [IMP] Delete the dead build files: `.travis.yml` (travis-ci.org shut down in 2021), plus `ci.gradle` and `publish.gradle`, which are never applied and use jcenter and gradle-git 0.8 (repo root)
  - **Test:** `./gradlew clean bake` still works. `grep -r 'ci.gradle\|publish.gradle' .` finds no references.
- [ ] T027 [IMP] Move deploys to native GitHub Pages: `actions/upload-pages-artifact` (path `build/jbake`) plus `actions/deploy-pages`, triggered only on push to `source`. Set Pages to "GitHub Actions" and confirm the custom domain `www.mikemcgarr.com` is still set in Settings → Pages (`.github/workflows/gradle.yml`)
  - **Test:** The deploy job succeeds. `gh api repos/jmcgarr/jmcgarr.github.io/pages --jq .build_type` returns `workflow`. The live site serves the new build and `check-urls.sh` reports 0 missing against the live URLs. **Re-run the T004 docs guard**: the uploaded artifact has no `docs/`.
- [ ] T028 [IMP] Remove the `org.ajoberstar.git-publish` plugin and its `gitPublish` block now that Actions deploys the site (`build.gradle`)
  - **Test:** `./gradlew tasks --all | grep -i gitPublish` returns nothing. A push to `source` still deploys.
- [ ] T029 [IMP] Delete the `GRGIT_USER` and `GRGIT_PASS` repo secrets and revoke the personal access token behind them (GitHub settings)
  - **Test:** `gh secret list` no longer shows them. The next deploy still succeeds.
- [ ] T030 [P] [IMP] Add Dependabot for `github-actions` and `gradle` (`.github/dependabot.yml`)
  - **Test:** The Insights → Dependency graph → Dependabot tab shows both ecosystems being checked.
- [ ] T031 [P] [IMP] Add CI quality gates on every PR: `xmllint` on the feed and sitemap, `lychee --offline build/jbake`, `scripts/check-urls.sh`, and `scripts/check-docs-not-published.sh` (`.github/workflows/gradle.yml`)
  - **Test:** A throwaway PR that deletes an image used by a post fails CI and names the missing file.
- [ ] T032 [IMP] Clean up the branches. Decide what to do with `post/five-disciplines` (finish and merge, or park it; it has diverged from origin). Tag each stale branch as `archive/<name>` before deleting it: `blog/*`, `guard`, `feature/update-look-and-feel`, `post/three-horizons-part2`. After T027, retire `master` (it's already preserved by `live-2026-10`) (git remotes)
  - **Test:** `git branch -r` lists only `origin/source` plus any branches you're actively working on. `git ls-remote --tags origin 'archive/*'` lists every removed branch.

### Checkpoint: M3

- [ ] CHK017 Fresh clone + JDK 21 on Apple Silicon: `./gradlew clean bake` succeeds with no deprecation warnings
- [ ] CHK018 The rendered output matches the pre-upgrade build (or every difference has been reviewed)
- [ ] CHK019 Push to `source` deploys through GitHub Pages Actions. PRs run the quality gates and never deploy
- [ ] CHK020 No PAT-based secrets remain
- [ ] CHK021 The docs guard still passes against the Pages artifact
- [ ] CHK022 Only active branches remain, and old ones are preserved as `archive/*` tags

---

## M4: Chart the Future

**Goal**: Decide deliberately whether to stay on JBake or move to a mainstream generator before
spending effort on template polish.

**Independent Test**: An accepted decision record exists. If the decision is to migrate, a spike shows
that representative content can move with its URLs unchanged.

### Bugs

_None. This milestone is a decision gate._

### Improvements

- [ ] T033 [IMP] Write a decision record comparing staying on JBake with Hugo, Astro, and Eleventy (`docs/01-PLATFORM-DECISION.md`). Criteria: support for the existing formats (legacy HTML, AsciiDoc, Markdown, front matter), keeping URLs the same, theme effort, toolchain upkeep, community health, and local preview experience
  - **Test:** The record has Context, Options, Decision, and Consequences sections, and its status is **Accepted**.
- [ ] T034 [IMP] _(Only if migrating)_ Run a spike: port three representative posts (one HTML, one AsciiDoc, one Markdown) plus the archive page to the chosen platform (separate branch)
  - **Test:** `scripts/check-urls.sh`, run against the spike output, keeps all four URLs. A side-by-side visual check against the live pages looks right.

### Checkpoint: M4

- [ ] CHK023 `docs/01-PLATFORM-DECISION.md` exists with status Accepted
- [ ] CHK024 If migrating: the spike preserves URLs, and M5 has been re-scoped for the new platform (M5 tasks assume JBake/FreeMarker)

---

## M5: Polished Presentation

**Goal**: Every page is valid HTML, search engines can find it, and links to it share nicely on social media.

**Independent Test**: The HTML validator reports 0 template-originated errors. Lighthouse SEO ≥ 95 and
Accessibility ≥ 95. Social preview validators show a title, description, and image for posts.

### Bugs

- [ ] T035 [BUG] `<p>${content.body}</p>` wraps block-level HTML in a paragraph, which is invalid HTML. Use `<article>` for posts and `<div>` for pages (`src/jbake/templates/post.ftl`, `src/jbake/templates/page.ftl`)
  - **Test:** `vnu build/jbake/about.html build/jbake/blog/roadmaps.html` reports no "element not allowed as child of `p`" or "no `p` element in scope" errors.
- [ ] T036 [P] [BUG] `post.ftl` has a grid column with no `row` around it, so its alignment doesn't match pages (`src/jbake/templates/post.ftl`)
  - **Test:** In preview at the md and lg breakpoints, a post's text column lines up with the About page's text column.
- [ ] T037 [P] [BUG] `masthead.ftl` always renders an empty `<span class="subheading">` because `pageSubtitle` is always defined. Also finish or remove the commented-out "TODO fix this" block and support full-URL or arbitrary-path mastheads, so the `masthead=../qcon_crowd.png` workaround can go (`src/jbake/templates/masthead.ftl`, affected content front matter)
  - **Test:** `grep -l '<span class="subheading"></span>' build/jbake -r` returns nothing. `grep -r 'masthead=\.\./' src/jbake/content` returns nothing. The affected pages still show their mastheads.
- [ ] T038 [P] [BUG] The footer still says "Copyright 2009–2018" and "Bootstrap v4.1". Derive the year from `published_date` and fix or remove the version text (`src/jbake/templates/footer.ftl`)
  - **Test:** After a bake, the footer shows the current year. `grep -r '2009-2018' build/jbake` returns nothing.
- [ ] T039 [P] [BUG] Tag page URLs contain spaces (for example `tags/acceptance test.html`). Turn on JBake tag sanitizing and generate redirect stubs (meta refresh + `rel=canonical`) at the old paths (`src/jbake/jbake.properties`, redirect stub template or script)
  - **Test:** New tag URLs have no spaces. Every old tag URL in `docs/baseline/urls.txt` still loads and redirects to the new one. `check-urls.sh` passes.
- [ ] T040 [P] [BUG] `<meta name="author">` is set to a Twitter URL. Set it to the author's name (`src/jbake/templates/header.ftl`)
  - **Test:** `grep -h 'name="author"' build/jbake/index.html` shows a name, not a URL.

### Improvements

- [ ] T041 [IMP] Give each page its own `<meta name="description">` (from `summary`, falling back to the site description) and a `<link rel="canonical">` (`src/jbake/templates/header.ftl`)
  - **Test:** `grep -L 'rel="canonical"' build/jbake/blog/*.html` returns nothing. `grep -h 'name="description"' build/jbake/blog/*.html | sort -u | wc -l` is close to the post count.
- [ ] T042 [P] [IMP] Add Open Graph and Twitter card tags (title, summary, absolute `https://` masthead image, URL) (`src/jbake/templates/header.ftl`)
  - **Test:** For two posts, LinkedIn Post Inspector and https://www.opengraph.xyz show the right title, description, and image.
- [ ] T043 [P] [IMP] Keep one source of truth for CSS. Today `clean-blog.css` is hand-edited, while `clean-blog.min.css` and the SCSS are stale. Either keep SCSS and add a compile step, or keep plain CSS and delete the rest (`src/jbake/assets/css/`, SCSS sources)
  - **Test:** Change one color in the chosen source, bake, and the change shows up in preview. No orphaned or stale stylesheet remains in the repo.
- [ ] T044 [P] [IMP] Make `index.ftl` iterate `published_posts` instead of `posts` with a status check and a manual break (`src/jbake/templates/index.ftl`)
  - **Test:** The home page shows the same 6 posts, in the same order, as the baseline live home page.
- [ ] T045 [P] [IMP] Decide on comments: keep Disqus, switch to Giscus (GitHub Discussions), or remove them (`src/jbake/templates/post.ftl`)
  - **Test:** If kept or replaced, comments load on a post in preview. If removed, `grep -r disqus build/jbake` returns nothing.
- [ ] T046 [P] [IMP] Do an accessibility pass: alt text on images, contrast for `.photo-credit` (`#d2d1d1`), and heading order (`src/jbake/templates/`, `src/jbake/assets/css/extra.css`)
  - **Test:** Lighthouse Accessibility ≥ 95 on the four baseline pages.
- [ ] T047 [P] [IMP] Update the footer social links (Twitter → X; consider Bluesky or Mastodon) (`src/jbake/templates/footer.ftl`)
  - **Test:** `lychee build/jbake/index.html` reports every footer link as reachable.

### Checkpoint: M5

- [ ] CHK025 `vnu --skip-non-html build/jbake` reports 0 errors that come from templates (errors inside legacy post bodies get logged for M6)
- [ ] CHK026 Lighthouse SEO ≥ 95 and Accessibility ≥ 95 on the four baseline pages
- [ ] CHK027 Social previews are verified for two posts
- [ ] CHK028 `check-urls.sh` passes, including the old tag URLs (via redirects)

---

## M6: Content Care

**Goal**: The writing is accurate and easy to read, and every link and image still works.
_(This milestone can be done at any time after M0.)_

**Independent Test**: A full `lychee` run reports 0 broken links (or documents each exception), the
About, Talks, and Bio pages are current, and all content files use LF line endings.

### Bugs

- [ ] T048 [BUG] The About page is out of date (it says "currently at Slack", from 2018). Review the Talks and Speaker Bio pages too (`src/jbake/content/about.asciidoc`, `talks.asciidoc`, `speaker-bio.asciidoc`)
  - **Test:** A manual read-through confirms roles, employers, and dates are current, and each page's `date=` is updated.
- [ ] T049 [BUG] Fix images hotlinked from dead or insecure hosts (public Dropbox links, agilescout.com, the Hudson wiki, `http://farm*.staticflickr.com`). Recover what you can through the Wayback Machine and self-host it in `assets/img/`, and remove or replace the rest (`src/jbake/content/blog/`)
  - **Test:** `grep -rE '<img[^>]+src="https?://' src/jbake/content` returns only intentional, working external images. `lychee build/jbake` reports 0 broken image URLs.
- [ ] T050 [BUG] Fix broken outbound links. Update each one, point it to an archive.org snapshot, or remove it (`src/jbake/content/`)
  - **Test:** Save a `lychee build/jbake` report to `docs/reports/links-<date>.md` showing 0 errors, or with each remaining failure listed and justified.

### Improvements

- [ ] T051 [IMP] Convert the 63 content files with old Mac CR-only line endings (from the WordPress import) to LF (`src/jbake/content/`)
  - **Test:** `grep -rlU $'\r' src/jbake/content | wc -l` returns `0`. `diff -rw` between bakes from before and after the conversion shows only whitespace changes.
- [ ] T052 [P] [IMP] Upgrade outbound `http://` links to `https://` where the target supports it (`src/jbake/content/`)
  - **Test:** The `http://` link count drops from the 559 baseline in `docs/baseline/metrics.md`, and `lychee` is still clean.
- [ ] T053 [P] [IMP] Triage the 12-post drafts backlog: finish and publish, keep as drafts, or delete (`drafts/` after T009)
  - **Test:** Every draft has a recorded decision (a short table in `docs/reports/drafts-triage.md`).
- [ ] T054 [P] [IMP] _(Optional)_ Fix the slug typos (`relections-and-projections-2019`, `vagrant-cheatsheat`, `developer-reading-lis`), but **only** together with redirect stubs at the old URLs (`src/jbake/content/blog/`)
  - **Test:** Each old URL redirects to the new one. `check-urls.sh` passes with the old URLs still present as stubs.

### Checkpoint: M6

- [ ] CHK029 `lychee build/jbake` reports 0 broken links, or every exception is documented in `docs/reports/`
- [ ] CHK030 About, Talks, and Speaker Bio have been reviewed and are current
- [ ] CHK031 `grep -rlU $'\r' src/jbake/content` returns nothing
- [ ] CHK032 Every draft has a triage decision

---

## Notes

- **Keep URLs stable.** This site has links pointing at it from about 15 years of the web. Any task that changes a URL must leave a redirect stub, and `check-urls.sh` is what enforces that.
- **Ship each milestone separately.** Publish after each checkpoint passes, then update `docs/baseline/metrics.md` so the next milestone measures against the new numbers.
- **Number future planning docs** after this one (`01-PLATFORM-DECISION.md`, `02-...`). They all live in `docs/`, which T004/T005 keep off the live site.
