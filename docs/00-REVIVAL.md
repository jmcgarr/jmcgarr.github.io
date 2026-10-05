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
  - **Issue:** Closes #N        (only when the task resolves a GitHub issue)
  - **Test:** how to prove the task is done
```

- **T000**: Task ID, unique across the whole plan. IDs are never renumbered or reused; tasks added later take the next free ID (see `AGENTS.md`).
- **[P]**: Can run in parallel. It touches different files from the other `[P]` tasks in the same section and doesn't depend on them.
- **[BUG]**: Something that is broken, wrong, or publishing something it shouldn't.
- **[IMP]**: Improvement. The current behavior works, but it could be faster, cleaner, safer, or easier to maintain.
- **Test:** Every task has a concrete check: a command, a URL, or a manual step with a clear pass/fail result.
- **Issue:** Links the task to a GitHub issue. `Closes #N` means the PR that completes the task closes the issue. `Refs #N` means the task contributes but doesn't finish it. Copy the line into that commit message and PR description (see [GitHub Issues](#github-issues) and `AGENTS.md` Rule 5).
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
| M1 | Stop the Bleeding      | Everything the site does, it does correctly              | 9    | 1            |
| M2 | Lighten the Load       | Fast on a phone; site shrinks ~90%                       | 2    | 5            |
| M3 | Modern Toolchain       | Builds anywhere, deploys safely, no personal tokens      | 1    | 9            |
| M4 | Chart the Future       | Decide the platform before investing in polish           | 0    | 2            |
| M5 | Polished Presentation  | Valid HTML, discoverable, shares well                    | 8    | 9            |
| M6 | Content Care           | Accurate content; every link and image works             | 3    | 5            |
| M7 | Writing Flow           | Writing a post is pleasant: scaffold, watch, live reload | 0    | 6            |

### Dependencies & Order

- **M0 blocks everything.** You need the baseline and guardrails before changing anything.
- **M1** comes next because it fixes things that are wrong on the live site right now.
- **M2 and M3** don't depend on each other and can run in either order or overlap.
- **M4 is a decision gate.** It should finish before **M5**, because M5 is template work for the current JBake platform and would be thrown away if the site migrates.
- **M6** depends only on M0 and can be picked up at any time. It's good work for spare evenings.
- **M7** comes after M4, because its tasks depend on the platform decision. T061 also needs T008 (M1). It doesn't depend on M5 or M6.

### GitHub Issues

Open issues on [jmcgarr/jmcgarr.github.io](https://github.com/jmcgarr/jmcgarr.github.io/issues), reviewed 2026-10-04, and the tasks that resolve them.

| Issue | Title | GitHub label | Task(s) | Milestone | Put in the resolving PR |
|---|---|---|---|---|---|
| [#3](https://github.com/jmcgarr/jmcgarr.github.io/issues/3) | OrientDB errors when using OpenJDK | bug | T025 (fix), T023 | M3 | `Closes #3` (T025), `Refs #3` (T023) |
| [#4](https://github.com/jmcgarr/jmcgarr.github.io/issues/4) _(closed)_ | TravisCI: pushing a branch still publishes the site | bug | T008 (the same problem came back with GitHub Actions) | M1 | `Refs #4` |
| [#6](https://github.com/jmcgarr/jmcgarr.github.io/issues/6) | Update all existing links to open in a new tab | enhancement | T058 | M5 | `Closes #6` |
| [#7](https://github.com/jmcgarr/jmcgarr.github.io/issues/7) | CSS should be generated per build | enhancement | T043 | M5 | `Closes #7` |
| [#8](https://github.com/jmcgarr/jmcgarr.github.io/issues/8) | [Gradle] Add a task that creates a new post | enhancement | T060, T061 | M7 | `Refs #8` (T060), `Closes #8` (T061) |
| [#9](https://github.com/jmcgarr/jmcgarr.github.io/issues/9) | [Gradle] Re-add the watch/rebuild feature | enhancement | T062 | M7 | `Closes #9` |
| [#10](https://github.com/jmcgarr/jmcgarr.github.io/issues/10) | [Gradle] Improve incremental build times | enhancement | T063 | M7 | `Closes #10` |
| [#11](https://github.com/jmcgarr/jmcgarr.github.io/issues/11) | [Gradle] Live reload? | enhancement | T064 | M7 | `Closes #11` |
| [#12](https://github.com/jmcgarr/jmcgarr.github.io/issues/12) | [Gradle] Add toast messages when a build is complete | enhancement | T065 | M7 | `Closes #12` |
| [#13](https://github.com/jmcgarr/jmcgarr.github.io/issues/13) | Add a tags page to browse by topics | enhancement | T057 | M5 | `Closes #13` |
| [#14](https://github.com/jmcgarr/jmcgarr.github.io/issues/14) | Add mastheads for older posts | enhancement | T059 | M6 | `Closes #14` |
| [#15](https://github.com/jmcgarr/jmcgarr.github.io/issues/15) | Fix the code snippet look and feel | bug, enhancement | T056 | M5 | `Closes #15` |
| [#16](https://github.com/jmcgarr/jmcgarr.github.io/issues/16) | Masthead should be able to include remote images | bug | T037 | M5 | `Closes #16` |

Open pull requests [#5](https://github.com/jmcgarr/jmcgarr.github.io/pull/5) (`[WIP] Three Horizons Part 2`) and
[#17](https://github.com/jmcgarr/jmcgarr.github.io/pull/17) (draft, `The Five Disciplines of Management`) are covered by T032.

**How issues get closed:**
- Put the task's `Closes #N` line, one line per issue, in both the commit message and the PR description. `Closes #3, #16` closes only #3.
- GitHub closes the issue when that commit or PR is merged into `source`, the default branch. Pushing a feature branch or opening a PR doesn't close anything.
- Use `Refs #N` for partial work. It links the issue without closing it.
- If M4 decides to migrate and that makes an issue moot (for example the generator has watch mode built in), close the issue from the PR that delivers the replacement, and say so in the PR.

---

## M0: Baseline & Guardrails

**Goal**: Snapshot exactly what's live today and add the safety checks that every later milestone uses.

**Independent Test**: On an unmodified `source` checkout, `./gradlew clean bake` followed by both
check scripts reports **zero** differences from the live site, and no planning docs appear in the build.

### Bugs

_None. This milestone is groundwork._

### Improvements

- [x] T001 [IMP] Tag the currently-live site so it can always be compared or restored: `git tag live-2026-10 origin/master && git push origin live-2026-10`
  - **Test:** `git ls-remote --tags origin live-2026-10` returns a ref that points at the current `origin/master` commit.
  - _Done 2026-10-04: annotated tag, peels to `dd49867` (= `origin/master`)._
- [x] T002 [P] [IMP] Record the URL inventory of the live site (`docs/baseline/urls.txt`): `git ls-tree -r -z --name-only live-2026-10 | tr '\0' '\n' | grep '\.html$' | LC_ALL=C sort > docs/baseline/urls.txt` (`-z` keeps paths with spaces or non-ASCII characters from being quoted)
  - **Test:** `wc -l docs/baseline/urls.txt` equals the number of `.html` files in `live-2026-10`.
  - _Done 2026-10-04: 272 URLs, matching the tag (181 are tag pages, 48 of those with spaces in the name)._
- [x] T003 [P] [IMP] Record baseline metrics (`docs/baseline/metrics.md`): published size (`du -sh` of a `live-2026-10` checkout, about 125 MB), Lighthouse mobile scores (Performance, Accessibility, Best Practices, SEO) for `/`, `/about.html`, `/archive.html`, and one post, plus the count of `http://` links in content (559)
  - **Test:** The file has all four pages × four scores, the site size, and the link count, with the date measured.
  - _Done 2026-10-04: median of 3 Lighthouse 12.8.2 mobile runs per page. Note that SEO already scores 100, so M5 is judged by its task tests rather than that score._
- [x] T004 [IMP] **Guarantee `docs/` is never published.** Verify that the bake root is `src/jbake` and that `gitPublish.contents` copies only `build/jbake`, and confirm that GitHub Pages is **not** set to "Deploy from branch → `source` → `/docs`". Then add `scripts/check-docs-not-published.sh`, which fails if `build/jbake` contains a `docs/` directory, any `*.md` file outside `vendor/` (third-party packages ship their own READMEs, e.g. `vendor/fontawesome-free/README.md`), or the string `REVIVAL` anywhere
  - **Test:**
    - `./gradlew clean bake && scripts/check-docs-not-published.sh` exits 0.
    - `gh api repos/jmcgarr/jmcgarr.github.io/pages --jq '.source'` returns `{"branch":"master","path":"/"}`.
    - After the next publish, `curl -s -o /dev/null -w '%{http_code}' https://www.mikemcgarr.com/docs/00-REVIVAL.md` returns `404`.
    - Negative test: `mkdir build/jbake/docs && scripts/check-docs-not-published.sh` exits non-zero.
  - _Done 2026-10-04: bake root and publish contents confirmed, Pages source is `master` `/` (and `https_enforced` is already `true`). Guard passes on a clean bake. Negative tests fail as expected for a `docs/` dir, a `.md` file outside `vendor/`, the `REVIVAL` marker in HTML, and a planning doc hidden under `vendor/`. The live URL check is already 404 (still 404 after the `cbca98c` publish)._
- [x] T005 [IMP] Run the docs guard in CI as a step after "Bake with Gradle" and before "Publish content" (`.github/workflows/gradle.yml`)
  - **Test:** The Actions log shows the guard step passing. On a throwaway branch, temporarily copying `docs/` into `build/jbake` makes the workflow fail **before** publishing.
  - _2026-10-04: step added between bake and publish. **CI verified (positive):** in PR #18's run the guard printed `OK: no docs/ content in build/jbake` before the (skipped) publish step. **Negative CI test passed** in throwaway PR [#19](https://github.com/jmcgarr/jmcgarr.github.io/pull/19), which swapped publish for a dry-run `echo` with the same kind of `if:`. Control run 37226118033: guard passed, dry-run publish **ran**. Leak run 37226203986 (`docs/` copied into the build): guard **failed** and dry-run publish was **skipped**. GitHub adds an implicit `success() &&` to an `if:` with no status function, so a failing guard blocks the real publish too._
- [x] T006 [IMP] Add `scripts/check-urls.sh`. It diffs the `.html` files in `build/jbake` against `docs/baseline/urls.txt`, prints any **missing** and **new** URLs, and takes an optional allowlist of expected removals (`docs/baseline/expected-removals.txt`)
  - **Test:** On a clean bake of unmodified `source`, it reports 0 missing. Deleting one file in `build/jbake/blog/` makes it exit non-zero and name that file.
  - _Done 2026-10-04: a clean bake reports 0 missing. Deleting `blog/roadmaps.html` or `tags/acceptance test.html` fails and names the file, an allowlisted removal passes, and a new page is reported without failing. On a case-insensitive filesystem (macOS), pages that differ only by case (`tags/DevOps.html` vs `tags/devops.html`) overwrite each other during the bake, so the script reports them as warnings there. CI on Linux stays strict. See T055._

### Checkpoint: M0

- [x] CHK001 Tag `live-2026-10` exists on the remote
- [x] CHK002 `docs/baseline/urls.txt` and `docs/baseline/metrics.md` are committed
- [x] CHK003 The docs guard passes locally **and** runs in CI before the publish step _(CI: PR #18, run 37214338446)_
- [x] CHK004 `scripts/check-urls.sh` reports 0 missing URLs on an unmodified clean bake
- [x] CHK005 `https://www.mikemcgarr.com/docs/00-REVIVAL.md` returns 404 _(2026-10-04, and again after the `cbca98c` publish)_

---

## M1: Stop the Bleeding

**Goal**: Everything the site does today, it does correctly. Nothing private or broken gets published.

**Independent Test**: The sitemap and feed validate. No drafts, dead widgets, or plain-HTTP self-links
are in the build. A pull request can no longer publish to the live site.

### Bugs

- [ ] T007 [BUG] Fix the missing `/` between host and path in the sitemap and feed URLs. They currently render as `http://www.mikemcgarr.comblog/...` (`src/jbake/jbake.properties`, `src/jbake/templates/sitemap.ftl`, `src/jbake/templates/feed.ftl`)
  - **Test:** `grep -cE 'mikemcgarr\.com[a-z]' build/jbake/sitemap.xml build/jbake/feed.xml` returns `0` for both files. `xmllint --noout` passes. The feed validates at https://validator.w3.org/feed/. Google Search Console accepts the sitemap with 0 errors.
  - _2026-10-04 (`fix/T007-T010-site-host`): templates now join host and path with `/`. **Local checks pass:** the glued-URL grep returns 0 for both files, `xmllint` passes, the sitemap validates against the sitemaps.org XSD, all 74 feed items and 77 sitemap URLs resolve to built pages, and the feed `<guid>`s are unchanged (so readers won't see old posts reappear). **Still to do after publish:** the W3C feed validator (it blocks scripted checks behind a Cloudflare challenge, so use a browser with the live URL) and Search Console. Tick then. **Live (after PR #22 published `277d64f`):** 0 glued URLs, the sitemap validates against the XSD, and all 78 sitemap and feed URLs return 200 over https._
- [x] T008 [BUG] Run the publish step only on `push`, not on `pull_request`. Today a same-repo PR would deploy unmerged content (`.github/workflows/gradle.yml`, add `if: github.event_name == 'push'`)
  - **Issue:** Refs #4. This is closed issue #4 (Travis published from branches) coming back with GitHub Actions.
  - **Test:** Open a throwaway PR. The Actions log shows "Publish content" as **skipped**, and `git ls-remote origin master` is unchanged before and after.
  - _Done 2026-10-04: the step is gated on `github.event_name == 'push' && github.ref == 'refs/heads/source'`. Draft PR [#18](https://github.com/jmcgarr/jmcgarr.github.io/pull/18) ran CI as `pull_request` (run 37214338446): bake and docs guard succeeded, "Publish content" was **skipped**, and `master` stayed at `dd49867`. Before merging, CI's Linux build was compared with the live site and matches except for `feed.xml` timestamps (see [`reports/linux-build-vs-live-2026-10-04.md`](reports/linux-build-vs-live-2026-10-04.md))._
- [ ] T009 [BUG] Stop publishing drafts. All 12 are live under `/blog/drafts/*-draft.html`. JBake 2.6.4 always renders `status=draft` posts (as `<name>-draft.html`) and has no setting to skip them, only `draft.suffix`. So keep rendering them for local preview, and exclude `**/*-draft.html` from the publish contents (`gitPublish.contents`). Add a CI check on the publish contents. Drafts stay in `src/jbake/content/blog/drafts/` (`build.gradle`, `.github/workflows/gradle.yml`)
  - **Test:** `./gradlew gitPublishCopy && find build/gitPublish -name '*-draft.html'` is empty, while `build/jbake` still has the 12 drafts. The CI step "Check drafts are not published" passes. After the next publish, `curl -sI https://www.mikemcgarr.com/blog/drafts/specflow-selenium-draft.html` returns `404`. The 12 draft URLs are listed in `docs/baseline/expected-removals.txt`, so `check-urls.sh` passes.
  - _2026-10-04 (`fix/T009-drafts-not-published`, removal approved by the owner): **local checks pass.** The publish contents have 0 drafts and the preview still has 12. A local publish commit (not pushed) differs from live `master` only by the 12 deleted drafts, plus noise: the `feed.xml` timestamp, the macOS tag case collision (T055), and two same-date posts (2013-02-15) swapping order. `check-urls.sh build/gitPublish` lists exactly the 12 expected removals. The CI check fails (12 drafts) with the exclude removed. **Still to do after publish:** the live 404 check. Tick then._
- [x] T010 [BUG] Serve the site over HTTPS everywhere. Set `site.host=https://www.mikemcgarr.com`, remove the hardcoded `http://www.mikemcgarr.com/` share URLs, and turn on **Enforce HTTPS** in the Pages settings (`src/jbake/jbake.properties`, `src/jbake/templates/post.ftl`). _Note (2026-10-04): Enforce HTTPS is already on (`https_enforced: true`); the template and config changes are still needed._
  - **Test:** `grep -r 'http://www.mikemcgarr.com' build/jbake` returns nothing. `curl -sI http://www.mikemcgarr.com/` returns `301` to `https://`. `gh api repos/jmcgarr/jmcgarr.github.io/pages --jq .https_enforced` returns `true`.
  - _Done 2026-10-04 (`fix/T007-T010-site-host`): `site.host` is `https://`. The 4 share URLs in `post.ftl` now use `${config.site_host}`, and 4 self-links in `the-modern-tech-resume.asciidoc` changed scheme only. The build has 0 files with `http://(www.)mikemcgarr.com`. `http://www.` and `http://` (apex) both return `301` to `https://www.mikemcgarr.com/`, and `https_enforced` is `true`. A before/after bake diff shows only those changes (86 post pages, feed, sitemap) and no URLs added or removed._
- [ ] T011 [P] [BUG] Remove the dead social widgets: Google+ (`g-plusone`, `apis.google.com/js/platform.js`), LinkedIn `in.js` and `IN/Share`, and the Facebook `fb-like` div, which has no SDK loaded (`src/jbake/templates/header.ftl`, `src/jbake/templates/footer.ftl`, `src/jbake/templates/post.ftl`)
  - **Test:** `grep -rE 'apis.google.com|g-plusone|platform.linkedin.com|IN/Share|fb-like' build/jbake` returns nothing. The browser console on a post page shows no errors from third-party scripts.
- [ ] T012 [P] [BUG] Fix the French Twitter button (`data-lang="fr"`, label "Tweeter"). Replace the share bar with plain share links that need no third-party JS (X, LinkedIn, Bluesky, email) (`src/jbake/templates/post.ftl`)
  - **Test:** `grep -rE 'data-lang="fr"|Tweeter' build/jbake` returns nothing. In preview, each share link opens a pre-filled composer with the post's `https://` URL.
- [ ] T013 [P] [BUG] Remove the dead Universal Analytics snippet (`ga.js`, `UA-49993013-1`), which stopped collecting data in July 2023. Decide whether to replace it with GA4, Plausible, GoatCounter, or nothing (`src/jbake/templates/header.ftl`)
  - **Test:** `grep -rE 'ga\.js|UA-49993013' build/jbake` returns nothing. If you pick a replacement, its real-time dashboard records a visit to the live site after publish.
- [ ] T014 [P] [BUG] Fix the README. Remove the dead Travis badge, change the publish command from `bake publish` to the real task (`gitPublishPush`, or whatever M3 replaces it with), and document the preview steps (`README.md`)
  - **Test:** Follow the README step by step from a fresh clone. The preview works at http://localhost:8080 and the badge renders.
- [x] T066 [BUG] Publishing is broken: the first publish since 2024 (the PR #18 merge, run 37229952098) failed at `gitPublishPush` with `TransportException: … not authorized`. The personal access token in `GRGIT_PASS` (set 2024-05-22) has expired. Publish with the built-in `GITHUB_TOKEN` instead (`GRGIT_USER: x-access-token`, job permission `contents: write`), and request a Pages build explicitly (`pages: write`), because a push made with `GITHUB_TOKEN` might not start one (`.github/workflows/gradle.yml`)
  - **Test:** After merging, the push run's "Publish content" and "Request a GitHub Pages build" steps succeed. `git fetch origin master && git diff --stat live-2026-10 origin/master` lists only `feed.xml` (see [`reports/linux-build-vs-live-2026-10-04.md`](reports/linux-build-vs-live-2026-10-04.md)). `gh api repos/jmcgarr/jmcgarr.github.io/pages/builds/latest --jq '.status + " " + .commit'` shows `built` for the new `master` commit. `curl -s https://www.mikemcgarr.com/feed.xml | grep -m1 lastBuildDate` shows the new build time.
  - _Done 2026-10-04: PR #20 merged, and push run 37231079943 published `master` `cbca98c`. `git diff --stat live-2026-10 origin/master` showed only `feed.xml` (2 lines), Pages showed `built` for `cbca98c`, and the live feed showed `lastBuildDate` Sun, 4 Oct 2026 20:11:32. That run proved a `GITHUB_TOKEN` push **does** start a Pages build by itself (at 20:11:49). The explicit request started a second build that cancelled the first, which Pages then recorded as "errored: Page build failed". The follow-up PR removes the request step and `pages: write`, and makes `github-actions[bot]` the author of the publish commit instead of `runner <runner@…>` (checked locally with `-Duser.home` pointing at a bot-only `.gitconfig`)._

### Improvements

- [ ] T015 [IMP] Add a GitHub Actions status badge to the README (`README.md`)
  - **Test:** The badge renders on the GitHub repo page and reflects the latest `source` run.

### Checkpoint: M1

- [ ] CHK006 The sitemap is accepted in Search Console and the feed passes the W3C validator
- [ ] CHK007 No drafts are reachable on the live site
- [ ] CHK008 HTTPS is enforced, and the build has no `http://www.mikemcgarr.com` self-links
- [ ] CHK009 Home, a post, About, and Archive load with **zero** console errors
- [x] CHK010 A test PR ran CI without publishing _(PR #18, 2026-10-04)_
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
  - **Issue:** Refs #3
  - **Test:** In the repo, `java -version` reports 21. The workflow has `java-version: '21'`. The bake succeeds locally and in CI.

### Improvements

- [ ] T024 [IMP] Upgrade the Gradle wrapper from 5.6.4 to the latest 8.x, going through 6.9 and 7.6 and fixing deprecations at each step (`gradle/wrapper/gradle-wrapper.properties`, `build.gradle`)
  - **Test:** `./gradlew --version` reports 8.x. `./gradlew clean bake --warning-mode all` has no deprecation warnings. **Output-equivalence check:** before upgrading, copy a clean bake to `/tmp/bake-before`. After upgrading, `diff -r /tmp/bake-before build/jbake` is empty, or every difference is explained.
- [ ] T025 [IMP] Upgrade `org.jbake.site` from 5.0.0 to the latest 5.x, along with its bundled JBake version (`build.gradle`)
  - **Issue:** Closes #3. JBake 2.6.x's OrientDB calls `sun.misc.VM`, which doesn't exist after JDK 8. Reproduced 2026-10-04: a bake on JDK 11 (Corretto 11.0.23, the version CI uses) succeeds but logs `ClassNotFoundException: sun.misc.VM` stack traces. JDK 1.8 logs none. CI shows it too: 6 occurrences in PR #18's Temurin 11 bake.
  - **Test:** The same output-equivalence `diff -r` as T024. Tags, archive, feed, and sitemap are all present. On JDK 11 **and** JDK 21, `./gradlew clean bake --info 2>&1 | grep -c 'sun.misc.VM'` returns `0`.
- [ ] T026 [P] [IMP] Delete the dead build files: `.travis.yml` (travis-ci.org shut down in 2021), plus `ci.gradle` and `publish.gradle`, which are never applied and use jcenter and gradle-git 0.8 (repo root)
  - **Test:** `./gradlew clean bake` still works. `grep -r 'ci.gradle\|publish.gradle' .` finds no references.
- [ ] T027 [IMP] Move deploys to native GitHub Pages: `actions/upload-pages-artifact` (path `build/jbake`) plus `actions/deploy-pages`, triggered only on push to `source`. Set Pages to "GitHub Actions" and confirm the custom domain `www.mikemcgarr.com` is still set in Settings → Pages (`.github/workflows/gradle.yml`)
  - **Test:** The deploy job succeeds. `gh api repos/jmcgarr/jmcgarr.github.io/pages --jq .build_type` returns `workflow`. The live site serves the new build and `check-urls.sh` reports 0 missing against the live URLs. **Re-run the T004 docs guard**: the uploaded artifact has no `docs/`. **Keep T009's exclusion:** `build/jbake` contains drafts, so upload a copy without `**/*-draft.html`, and point the drafts check at the artifact. The artifact must have no `-draft.html`.
- [ ] T028 [IMP] Remove the `org.ajoberstar.git-publish` plugin and its `gitPublish` block now that Actions deploys the site (`build.gradle`)
  - **Test:** `./gradlew tasks --all | grep -i gitPublish` returns nothing. A push to `source` still deploys, and still without drafts (T009's exclusion lives in `gitPublish.contents` today, so move it to T027's artifact step first).
- [ ] T029 [IMP] Delete the `GRGIT_USER` and `GRGIT_PASS` repo secrets and revoke the personal access token behind them (GitHub settings)
  - **Test:** `gh secret list` no longer shows them. The next deploy still succeeds.
  - _Note (2026-10-04): the workflow stopped using these secrets in T066, and the token had already expired. This can be done as soon as T066's publish succeeds, ahead of the rest of M3. **2026-10-04: both secrets deleted** with the owner's approval (`gh secret list` is empty). Still to do: the next deploy succeeds (the T009 merge), and the owner deletes the expired token in GitHub settings._
- [ ] T030 [P] [IMP] Add Dependabot for `github-actions` and `gradle` (`.github/dependabot.yml`)
  - **Test:** The Insights → Dependency graph → Dependabot tab shows both ecosystems being checked.
- [ ] T031 [P] [IMP] Add CI quality gates on every PR: `xmllint` on the feed and sitemap, `lychee --offline build/jbake`, `scripts/check-urls.sh`, and `scripts/check-docs-not-published.sh` (`.github/workflows/gradle.yml`)
  - **Test:** A throwaway PR that deletes an image used by a post fails CI and names the missing file.
- [ ] T032 [IMP] Clean up the branches. Decide what to do with `post/five-disciplines` (finish and merge, or park it; it has diverged from origin). Tag each stale branch as `archive/<name>` before deleting it: `blog/*`, `guard`, `feature/update-look-and-feel`, `post/three-horizons-part2`. After T027, retire `master` (it's already preserved by `live-2026-10`). Also resolve the two open content PRs, [#5](https://github.com/jmcgarr/jmcgarr.github.io/pull/5) `[WIP] Three Horizons Part 2` (last updated 2021-02-14) and draft [#17](https://github.com/jmcgarr/jmcgarr.github.io/pull/17) `The Five Disciplines of Management`: finish and merge, or close. Merging publishes the post, so that's the owner's call, and it should wait until T008 and T009 are done (git remotes, GitHub PRs)
  - **Test:** `git branch -r` lists only `origin/source` plus any branches you're actively working on. `git ls-remote --tags origin 'archive/*'` lists every removed branch. `gh pr list --state open` shows no stale PRs.

### Checkpoint: M3

- [ ] CHK017 Fresh clone + JDK 21 on Apple Silicon: `./gradlew clean bake` succeeds with no deprecation warnings
- [ ] CHK018 The rendered output matches the pre-upgrade build (or every difference has been reviewed)
- [ ] CHK019 Push to `source` deploys through GitHub Pages Actions. PRs run the quality gates and never deploy
- [ ] CHK020 No PAT-based secrets remain
- [ ] CHK021 The docs guard still passes against the Pages artifact
- [ ] CHK022 Only active branches remain, and old ones are preserved as `archive/*` tags
- [ ] CHK037 Issue #3 is closed by the merged T025 PR, and bakes on JDK 11/21 log no `sun.misc.VM` errors

---

## M4: Chart the Future

**Goal**: Decide deliberately whether to stay on JBake or move to a mainstream generator before
spending effort on template polish.

**Independent Test**: An accepted decision record exists. If the decision is to migrate, a spike shows
that representative content can move with its URLs unchanged.

### Bugs

_None. This milestone is a decision gate._

### Improvements

- [ ] T033 [IMP] Write a decision record comparing staying on JBake with Hugo, Astro, and Eleventy (`docs/01-PLATFORM-DECISION.md`). Criteria: support for the existing formats (legacy HTML, AsciiDoc, Markdown, front matter), keeping URLs the same, theme effort, toolchain upkeep, community health, and the writing workflow: new-post scaffolding, watch/rebuild, incremental builds, and live reload (issues #8–#12, M7)
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
  - **Issue:** Closes #16. Remote mastheads keep large images out of the repo, which is what the issue asks for.
  - **Test:** `grep -l '<span class="subheading"></span>' build/jbake -r` returns nothing. `grep -r 'masthead=\.\./' src/jbake/content` returns nothing. The affected pages still show their mastheads. A test post with `masthead=https://…` shows that remote image as its header background.
- [ ] T038 [P] [BUG] The footer still says "Copyright 2009–2018" and "Bootstrap v4.1". Derive the year from `published_date` and fix or remove the version text (`src/jbake/templates/footer.ftl`)
  - **Test:** After a bake, the footer shows the current year. `grep -r '2009-2018' build/jbake` returns nothing.
- [ ] T039 [P] [BUG] Tag page URLs contain spaces (for example `tags/acceptance test.html`). Turn on JBake tag sanitizing and generate redirect stubs (meta refresh + `rel=canonical`) at the old paths (`src/jbake/jbake.properties`, redirect stub template or script)
  - **Test:** New tag URLs have no spaces. Every old tag URL in `docs/baseline/urls.txt` still loads and redirects to the new one. `check-urls.sh` passes.
- [ ] T040 [P] [BUG] `<meta name="author">` is set to a Twitter URL. Set it to the author's name (`src/jbake/templates/header.ftl`)
  - **Test:** `grep -h 'name="author"' build/jbake/index.html` shows a name, not a URL.
- [ ] T055 [P] [BUG] Inconsistent tag casing creates duplicate tag pages that each list only some of the posts: `DevOps` (1 post) vs `devops` (5), and `Groovy` (5) vs `groovy` (1). Both versions are live (`tags/DevOps.html` and `tags/devops.html`). On macOS (case-insensitive filesystem) they overwrite each other, so a local bake doesn't match CI. Normalize the tags in front matter to lowercase, and leave redirect stubs at `tags/DevOps.html` and `tags/Groovy.html` (`src/jbake/content/blog/`)
  - **Test:** `git ls-files -z src/jbake/content | xargs -0 cat | tr '\r' '\n' | grep '^tags=' | tr ',' '\n' | sed 's/^tags=//;s/^ *//;s/ *$//' | sort -u | sort -f | uniq -di` prints nothing (no tag differs from another only by case). `tags/devops.html` and `tags/groovy.html` list all 6 posts each. In CI (Linux), `check-urls.sh` passes, with the old capitalized URLs served by redirect stubs.
- [ ] T056 [P] [BUG] Code snippets have no highlighting and poor styling. Legacy posts mark 12 code blocks with Google code-prettify classes (`prettyprint`, `language-*`, `linenums`), but no template loads prettify, and 28 more are bare `<pre>`. Add self-hosted syntax highlighting that understands those classes (or maps them), and give `pre` and `code` readable styles (monospace, background, `overflow-x: auto`) that work with both `clean-blog.css` and `asciidoctor.css`. Fix the misspelled class `languague-groovy` (a markup fix only, no prose changes) (`src/jbake/templates/footer.ftl`, `src/jbake/assets/css/`, `src/jbake/assets/js/`)
  - **Issue:** Closes #15
  - **Test:** In preview, the code blocks on `blog/improving-my-shell-fu-oh-my-zsh.html` (the example in #15) are monospaced and highlighted, and at 375px width they scroll sideways instead of overflowing the page. A post with bare `<pre>` blocks also looks right. `grep -rl languague src/jbake/content` returns nothing.

### Improvements

- [ ] T041 [IMP] Give each page its own `<meta name="description">` (from `summary`, falling back to the site description) and a `<link rel="canonical">` (`src/jbake/templates/header.ftl`)
  - **Test:** `grep -L 'rel="canonical"' build/jbake/blog/*.html` returns nothing. `grep -h 'name="description"' build/jbake/blog/*.html | sort -u | wc -l` is close to the post count.
- [ ] T042 [P] [IMP] Add Open Graph and Twitter card tags (title, summary, absolute `https://` masthead image, URL) (`src/jbake/templates/header.ftl`)
  - **Test:** For two posts, LinkedIn Post Inspector and https://www.opengraph.xyz show the right title, description, and image.
- [ ] T043 [P] [IMP] Generate the theme CSS from SCSS on every build, as #7 asks. Today `clean-blog.css` is hand-edited (for example the May 2024 font changes), while `clean-blog.min.css` and the SCSS are stale. Port the hand edits back into the SCSS (it came from Start Bootstrap Clean Blog v5.0.1, which you can diff against), add a Sass compile step to the build, and stop committing generated CSS (`src/jbake/assets/css/`, SCSS sources moved by T020, `build.gradle`)
  - **Issue:** Closes #7
  - **Test:** `git ls-files 'src/jbake/assets/css/clean-blog*'` returns nothing. `./gradlew clean bake` produces `build/jbake/css/clean-blog.css`. Screenshots of the four baseline pages before and after look the same. Changing one SCSS variable changes the rendered site.
- [ ] T044 [P] [IMP] Make `index.ftl` iterate `published_posts` instead of `posts` with a status check and a manual break (`src/jbake/templates/index.ftl`)
  - **Test:** The home page shows the same 6 posts, in the same order, as the baseline live home page.
- [ ] T045 [P] [IMP] Decide on comments: keep Disqus, switch to Giscus (GitHub Discussions), or remove them (`src/jbake/templates/post.ftl`)
  - **Test:** If kept or replaced, comments load on a post in preview. If removed, `grep -r disqus build/jbake` returns nothing.
- [ ] T046 [P] [IMP] Do an accessibility pass: alt text on images, contrast for `.photo-credit` (`#d2d1d1`), and heading order (`src/jbake/templates/`, `src/jbake/assets/css/extra.css`)
  - **Test:** Lighthouse Accessibility ≥ 95 on the four baseline pages.
- [ ] T047 [P] [IMP] Update the footer social links (Twitter → X; consider Bluesky or Mastodon) (`src/jbake/templates/footer.ftl`)
  - **Test:** `lychee build/jbake/index.html` reports every footer link as reachable.
- [ ] T057 [P] [IMP] Add a page for browsing posts by topic: a tag index listing every tag with its post count, linked from the menu. Use JBake's tag index rendering: `render.tagsindex` already exists in 2.6.4 (it's off by default), so this may not need T025. Do this after T055 and T039, so the index doesn't show duplicate or space-filled tags (`src/jbake/templates/`, `src/jbake/templates/menu.ftl`, `src/jbake/jbake.properties`)
  - **Issue:** Closes #13
  - **Test:** The tag index lists every tag exactly once, and each count matches the number of posts on that tag's page. The menu links to it. `lychee --offline build/jbake` is clean. `check-urls.sh` lists the index as a new URL and reports nothing missing.
- [ ] T058 [P] [IMP] Open off-site links in a new tab across all posts, including the legacy HTML posts that #6 doesn't want to edit by hand. Do it once in the template: a small script that adds `target="_blank" rel="noopener noreferrer"` to links pointing outside `www.mikemcgarr.com`. That avoids editing each post and means new posts don't need a linter. Consider a visually hidden "(opens in new tab)" hint for screen readers (`src/jbake/templates/footer.ftl`, `src/jbake/assets/js/`)
  - **Issue:** Closes #6
  - **Test:** In preview, an external link in a legacy HTML post (`blog/sonar.html`) and in an AsciiDoc post opens a new tab, and DevTools shows `rel="noopener noreferrer"`. Menu links, archive links, links between posts, and `#` anchors still open in the same tab.

### Checkpoint: M5

- [ ] CHK025 `vnu --skip-non-html build/jbake` reports 0 errors that come from templates (errors inside legacy post bodies get logged for M6)
- [ ] CHK026 Lighthouse SEO ≥ 95 and Accessibility ≥ 95 on the four baseline pages
- [ ] CHK027 Social previews are verified for two posts
- [ ] CHK028 `check-urls.sh` passes, including the old tag URLs (via redirects)
- [ ] CHK038 Issues #6, #7, #13, #15, and #16 are closed by merged PRs

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
- [ ] T059 [P] [IMP] Give older posts their own mastheads. On 2026-10-04, 65 of 74 published posts used the default image. This is manual, creative work: choose an image for each post, with credit, that follows the image size rule in `AGENTS.md`, or use a remote image once T037 supports it. Only add `masthead=` and `mastheadCredit=` lines, and keep each file's existing line endings (`src/jbake/content/blog/`)
  - **Issue:** Closes #14
  - **Test:** `for f in src/jbake/content/blog/*.*; do tr '\r' '\n' < "$f" | grep -q '^masthead=' || echo "$f"; done | wc -l` returns `0` (baseline: 65). No newly added image in `src/jbake/assets/img/masthead/` is over 400 KB. Every new masthead has a `mastheadCredit=`.

### Checkpoint: M6

- [ ] CHK029 `lychee build/jbake` reports 0 broken links, or every exception is documented in `docs/reports/`
- [ ] CHK030 About, Talks, and Speaker Bio have been reviewed and are current
- [ ] CHK031 `grep -rlU $'\r' src/jbake/content` returns nothing
- [ ] CHK032 Every draft has a triage decision
- [ ] CHK039 Issue #14 is closed by a merged PR

---

## M7: Writing Flow

**Goal**: Writing a post is pleasant again. One command starts a post on its own branch, and the preview
rebuilds and reloads by itself while you write.

**Independent Test**: From a clean checkout, run the new-post command, open the preview, and edit the
post. The browser shows the change within about 2 seconds with no manual step, and none of the preview
tooling ends up in a production bake.

_Do this after M4. If the site migrates, mainstream generators include most of this (for example
`hugo server` watches, rebuilds, and live-reloads). In that case, re-scope these tasks to configuring and verifying that._

### Bugs

_None. The owner filed all of these (#8–#12) as enhancements._

### Improvements

- [ ] T060 [IMP] Add a Gradle task that starts a new post. It creates `<slug>.asciidoc` with the front matter filled in (`title`, today's `date`, `type=post`, `tags`, `status=draft`, `summary`, then `~~~~~~`), and creates and switches to a `post/<slug>` branch. It must refuse to overwrite an existing file, because that URL may already be published. Create it with `status=draft` in `src/jbake/content/blog/drafts/`. Per T009, drafts render for local preview but are never published (`build.gradle`)
  - **Issue:** Refs #8 (steps 1–3 of the issue)
  - **Test:** `./gradlew newPost -Ptitle="Hello World"` creates `hello-world.asciidoc` with every header field, and `git branch --show-current` prints `post/hello-world`. Running it again fails with "already exists". After `./gradlew bake`, the draft previews at `/blog/drafts/hello-world-draft.html`. It isn't in the archive or feed, and `./gradlew gitPublishCopy` leaves it out of the publish contents.
- [ ] T061 [IMP] Optional follow-up: let the new-post task also commit the file, push the branch, and open a draft `[WIP]` PR with `gh` (steps 4–6 of the issue). This needs T008 first, so the PR's CI run can't publish (`build.gradle`)
  - **Issue:** Closes #8
  - **Test:** `./gradlew newPost -Ptitle="Hello World" -Ppr` leaves a commit on `post/hello-world`, pushes the branch, and opens a draft PR titled `[WIP] Hello World` (`gh pr view --json isDraft` returns `true`). That PR's CI run shows "Publish content" as skipped. Delete the test PR and branch afterwards.
- [ ] T062 [IMP] Bring back watch-and-rebuild: while the preview runs, changes to content, templates, or assets rebuild the site automatically. First check whether the upgraded JBake plugin (T025) has a watch task. Otherwise, run Gradle continuous build (`./gradlew -t bake`) next to the preview server (`build.gradle`)
  - **Issue:** Closes #9
  - **Test:** With the preview running, edit a post and save. Within 5 seconds, with no restart, `build/jbake/blog/<post>.html` contains the change. Repeat with a template and a CSS file.
- [ ] T063 [IMP] Make rebuilds after a small edit fast. Baseline 2026-10-04 (JDK 1.8, warm Gradle daemon): a clean bake takes about 6 s. Measure the time from saving one post to the rebuilt page in T062's watch mode. If it's over 2 s, speed it up, for example by re-rendering only changed content if the JBake version supports it (`build.gradle`, `src/jbake/jbake.properties`)
  - **Issue:** Closes #10
  - **Test:** Change one post's text and time the rebuild. Use a real edit, not `touch`, because Gradle checks file contents. It should take ≤ 2 s, recorded in `docs/baseline/metrics.md` at the M7 checkpoint. If it already meets the target before any change, record that and close the issue with the numbers as evidence.
- [ ] T064 [IMP] Add live reload, so the browser refreshes itself after each rebuild. Replace the `liveEdit` task (AppleScript that only works on macOS with Chrome) with a cross-browser approach, for example a livereload script injected only into preview builds. It must never ship to the live site (`build.gradle`, `src/jbake/templates/`)
  - **Issue:** Closes #11
  - **Test:** With the preview open in Chrome and in Safari or Firefox, saving a post refreshes the page automatically. After a normal `./gradlew clean bake`, `grep -rli livereload build/jbake` returns nothing.
- [ ] T065 [P] [IMP] Show a notification when a rebuild finishes. In watch mode on macOS, show a notification when a rebuild succeeds or fails (for example with `osascript -e 'display notification …'`). Do nothing on other systems and in CI. The issue itself says this may be unnecessary if builds are fast, so decide after T063 (`build.gradle`)
  - **Issue:** Closes #12
  - **Test:** In watch mode on macOS, saving a post shows a "Site rebuilt" notification, and breaking a template shows a failure notification. CI logs show no notification errors. If you decide not to build this, record the decision and T063's numbers here, and close #12 from that PR.

### Checkpoint: M7

- [ ] CHK033 The new-post task creates a draft on its own `post/` branch and never overwrites an existing post
- [ ] CHK034 Saving a post updates the open browser within about 2 s, with no manual steps
- [ ] CHK035 `grep -rli livereload build/jbake` returns nothing after a production bake
- [ ] CHK036 Issues #8–#12 are closed by merged PRs (`gh issue list --state open` shows none of them)

---

## Notes

- **Keep URLs stable.** This site has links pointing at it from about 15 years of the web. Any task that changes a URL must leave a redirect stub, and `check-urls.sh` is what enforces that.
- **Ship each milestone separately.** Publish after each checkpoint passes, then update `docs/baseline/metrics.md` so the next milestone measures against the new numbers.
- **Keep the issue map current.** Check `gh issue list` at each milestone checkpoint. Any new issue gets a task and a row in [GitHub Issues](#github-issues).
- **Number future planning docs** after this one (`01-PLATFORM-DECISION.md`, `02-...`). They all live in `docs/`, which T004/T005 keep off the live site.
