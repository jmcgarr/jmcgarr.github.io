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
| M1 | Stop the Bleeding      | Everything the site does, it does correctly              | 11   | 1            |
| M2 | Lighten the Load       | Fast on a phone; site shrinks ~90%                       | 3    | 9            |
| M3 | Modern Toolchain       | Builds anywhere, deploys safely, no personal tokens      | 1    | 10           |
| M4 | Chart the Future       | Decide the platform before investing in polish           | 0    | 2            |
| M5 | Polished Presentation  | Valid HTML, discoverable, shares well                    | 8    | 9            |
| M6 | Content Care           | Accurate content; every link and image works             | 3    | 5            |
| M7 | Writing Flow           | Writing a post is pleasant: scaffold, watch, live reload | 3    | 9            |
| —  | Backlog                | Unscheduled; not reproduced or waiting on evidence       | 1    | 2            |

### Dependencies & Order

- **M0 blocks everything.** You need the baseline and guardrails before changing anything.
- **M1** comes next because it fixes things that are wrong on the live site right now.
- **M2 and M3** don't depend on each other and can run in either order or overlap.
- **M4 is a decision gate.** It should finish before **M5**, because M5 is template work for the current JBake platform and would be thrown away if the site migrates.
- **M6** depends only on M0 and can be picked up at any time. It's good work for spare evenings.
- **M7** comes after M4, because its tasks depend on the platform decision (made 2026-10-07: stay on JBake). T061 also needs T008 (M1). It doesn't depend on M5 or M6.

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

> **Status: done 2026-10-06.** All 11 bugs and T015 are complete, and checkpoints CHK006–CHK011 pass. The last items were T007 (Search Console accepted the sitemap) and T068 (bare-domain certificate, Enforce HTTPS back on).

**Goal**: Everything the site does today, it does correctly. Nothing private or broken gets published.

**Independent Test**: The sitemap and feed validate. No drafts, dead widgets, or plain-HTTP self-links
are in the build. A pull request can no longer publish to the live site.

### Bugs

- [x] T007 [BUG] Fix the missing `/` between host and path in the sitemap and feed URLs. They currently render as `http://www.mikemcgarr.comblog/...` (`src/jbake/jbake.properties`, `src/jbake/templates/sitemap.ftl`, `src/jbake/templates/feed.ftl`)
  - **Test:** `grep -cE 'mikemcgarr\.com[a-z]' build/jbake/sitemap.xml build/jbake/feed.xml` returns `0` for both files. `xmllint --noout` passes. The feed validates at https://validator.w3.org/feed/. Google Search Console accepts the sitemap with 0 errors.
  - _2026-10-04 (`fix/T007-T010-site-host`): templates now join host and path with `/`. **Local checks pass:** the glued-URL grep returns 0 for both files, `xmllint` passes, the sitemap validates against the sitemaps.org XSD, all 74 feed items and 77 sitemap URLs resolve to built pages, and the feed `<guid>`s are unchanged (so readers won't see old posts reappear). **Still to do after publish:** the W3C feed validator (it blocks scripted checks behind a Cloudflare challenge, so use a browser with the live URL) and Search Console. Tick then. **2026-10-04: the owner ran the W3C feed validator on the live feed and it passed. Search Console reported "Sitemap could not be read". The live sitemap serves correctly (200, `application/xml`, well-formed, 77 URLs, also to a Googlebot user agent), so the likely causes are a bare-domain property hitting the broken apex HTTPS (T068) or normal Search Console lag. The owner deferred this. Re-submit `https://www.mikemcgarr.com/sitemap.xml` after T068, then tick.** **2026-10-05: done. The owner confirmed Search Console now reports the sitemap as Success.** The earlier "could not be read" was Search Console lag, not a sitemap problem. **Live (after PR #22 published `277d64f`):** 0 glued URLs, the sitemap validates against the XSD, and all 78 sitemap and feed URLs return 200 over https._
- [x] T008 [BUG] Run the publish step only on `push`, not on `pull_request`. Today a same-repo PR would deploy unmerged content (`.github/workflows/gradle.yml`, add `if: github.event_name == 'push'`)
  - **Issue:** Refs #4. This is closed issue #4 (Travis published from branches) coming back with GitHub Actions.
  - **Test:** Open a throwaway PR. The Actions log shows "Publish content" as **skipped**, and `git ls-remote origin master` is unchanged before and after.
  - _Done 2026-10-04: the step is gated on `github.event_name == 'push' && github.ref == 'refs/heads/source'`. Draft PR [#18](https://github.com/jmcgarr/jmcgarr.github.io/pull/18) ran CI as `pull_request` (run 37214338446): bake and docs guard succeeded, "Publish content" was **skipped**, and `master` stayed at `dd49867`. Before merging, CI's Linux build was compared with the live site and matches except for `feed.xml` timestamps (see [`reports/linux-build-vs-live-2026-10-04.md`](reports/linux-build-vs-live-2026-10-04.md))._
- [x] T009 [BUG] Stop publishing drafts. All 12 are live under `/blog/drafts/*-draft.html`. JBake 2.6.4 always renders `status=draft` posts (as `<name>-draft.html`) and has no setting to skip them, only `draft.suffix`. So keep rendering them for local preview, and exclude `**/*-draft.html` from the publish contents (`gitPublish.contents`). Add a CI check on the publish contents. Drafts stay in `src/jbake/content/blog/drafts/` (`build.gradle`, `.github/workflows/gradle.yml`)
  - **Test:** `./gradlew gitPublishCopy && find build/gitPublish -name '*-draft.html'` is empty, while `build/jbake` still has the 12 drafts. The CI step "Check drafts are not published" passes. After the next publish, `curl -sI https://www.mikemcgarr.com/blog/drafts/specflow-selenium-draft.html` returns `404`. The 12 draft URLs are listed in `docs/baseline/expected-removals.txt`, so `check-urls.sh` passes.
  - _2026-10-04 (`fix/T009-drafts-not-published`, removal approved by the owner): **local checks pass.** The publish contents have 0 drafts and the preview still has 12. A local publish commit (not pushed) differs from live `master` only by the 12 deleted drafts, plus noise: the `feed.xml` timestamp, the macOS tag case collision (T055), and two same-date posts (2013-02-15) swapping order. `check-urls.sh build/gitPublish` lists exactly the 12 expected removals. The CI check fails (12 drafts) with the exclude removed. **Live (PR #23 merged, publish run 37250459609, `master` `6b68c6e`):** the publish removed exactly the 12 drafts and touched `feed.xml`, with 0 drafts left. A sweep of all 272 baseline URLs found **all 12 drafts returning 404**, and 259 of the other 260 returning 200. The exception, `tags/.NET.html`, was already broken (see T067)._
  - _2026-10-06 (T027/T028): the draft exclusion moved from `gitPublish.contents` to the workflow step that stages `build/site` (`rsync --exclude '*-draft.html'`), and "Check drafts are not published" now inspects that staged folder._
- [x] T010 [BUG] Serve the site over HTTPS everywhere. Set `site.host=https://www.mikemcgarr.com`, remove the hardcoded `http://www.mikemcgarr.com/` share URLs, and turn on **Enforce HTTPS** in the Pages settings (`src/jbake/jbake.properties`, `src/jbake/templates/post.ftl`). _Note (2026-10-04): Enforce HTTPS is already on (`https_enforced: true`); the template and config changes are still needed._
  - **Test:** `grep -r 'http://www.mikemcgarr.com' build/jbake` returns nothing. `curl -sI http://www.mikemcgarr.com/` returns `301` to `https://`. `gh api repos/jmcgarr/jmcgarr.github.io/pages --jq .https_enforced` returns `true`.
  - _Done 2026-10-04 (`fix/T007-T010-site-host`): `site.host` is `https://`. The 4 share URLs in `post.ftl` now use `${config.site_host}`, and 4 self-links in `the-modern-tech-resume.asciidoc` changed scheme only. The build has 0 files with `http://(www.)mikemcgarr.com`. `http://www.` and `http://` (apex) both return `301` to `https://www.mikemcgarr.com/`, and `https_enforced` is `true`. A before/after bake diff shows only those changes (86 post pages, feed, sitemap) and no URLs added or removed._
- [x] T011 [P] [BUG] Remove the dead social widgets: Google+ (`g-plusone`, `apis.google.com/js/platform.js`), LinkedIn `in.js` and `IN/Share`, and the Facebook `fb-like` div, which has no SDK loaded (`src/jbake/templates/header.ftl`, `src/jbake/templates/footer.ftl`, `src/jbake/templates/post.ftl`)
  - **Test:** `grep -rE 'apis.google.com|g-plusone|platform.linkedin.com|IN/Share|fb-like' build/jbake` returns nothing. The browser console on a post page shows no errors from third-party scripts.
  - _Done 2026-10-05 (`fix/T011-T013-widgets-analytics`): the grep returns 0 files. Measured locally with Lighthouse 12 (preview, before → after): home Best Practices 82 → 100, requests 25 → 20, third-party hosts 8 → 6. Post (`three-horizons-part1`) Best Practices 57 → 82, requests 67 → 44, **console errors 1 → 0** (the Google+ frame CSP error), third-party hosts 21 → 13. The rest are mostly Disqus and the trackers it loads (Taboola, Criteo), which is T045._ **Live (PRs #26/#27 published, `master` `7185995`):** all 260 live pages return 200, and none contain any dead widget or old analytics code. Lighthouse on the live home and post pages shows **0 console errors**, and home Best Practices is 100._
- [x] T012 [P] [BUG] Fix the French Twitter button (`data-lang="fr"`, label "Tweeter"). Replace the share bar with plain share links that need no third-party JS (X, LinkedIn, Bluesky, email) (`src/jbake/templates/post.ftl`)
  - **Test:** `grep -rE 'data-lang="fr"|Tweeter' build/jbake` returns nothing. In preview, each share link opens a pre-filled composer with the post's `https://` URL.
  - _2026-10-05: replaced with plain links (LinkedIn, X, Bluesky, Email; owner's choice), no JavaScript, on all 86 post pages. The grep returns 0. Decoding the links on `git-for-fork-pullrequest` (title with apostrophes) gives the exact title and `https://` URL for each, with spaces as `%20` (not `+`, which mail clients would show literally). The X and Bluesky composer URLs return 200 with the parameters intact. LinkedIn redirects logged-out visitors to sign in and carries the URL through. **Still to do after publish:** the owner clicks each link once while logged in. Live: share links are on 74 of 74 published posts. **2026-10-05: the owner tested the share links on the live site, and they work.**_
- [x] T013 [P] [BUG] Remove the dead Universal Analytics snippet (`ga.js`, `UA-49993013-1`), which stopped collecting data in July 2023. Decide whether to replace it with GA4, Plausible, GoatCounter, or nothing (`src/jbake/templates/header.ftl`)
  - **Test:** `grep -rE 'ga\.js|UA-49993013' build/jbake` returns nothing. If you pick a replacement, its real-time dashboard records a visit to the live site after publish.
  - _2026-10-05: Universal Analytics removed. **GoatCounter** (owner's choice; site `mikemcgarr`) added to `footer.ftl` with an explicit `https://` script URL. It's on 270 of 270 pages and loads in the preview (`gc.zgo.at`), and it doesn't count `localhost`. The grep returns 0. **Still to do after publish:** a visit appears at https://mikemcgarr.goatcounter.com. Live: on 260 of 260 pages, and Lighthouse saw the script load and send a hit to `mikemcgarr.goatcounter.com`. **2026-10-05: the owner confirmed visits appear in the GoatCounter dashboard.**_
- [x] T014 [P] [BUG] Fix the README. Remove the dead Travis badge, change the publish command from `bake publish` to the real task (`gitPublishPush`, or whatever M3 replaces it with), and document the preview steps (`README.md`)
  - **Test:** Follow the README step by step from a fresh clone. The preview works at http://localhost:8080 and the badge renders.
  - _Done 2026-10-05 (`docs/T014-T015-readme`): rewrote the README (requirements, preview, drafts, writing a post, publishing through PRs, checks). **Tested from a fresh clone:** `.java-version` picked JDK 1.8. A draft made from the README's header template previewed at `/blog/drafts/readme-test-draft.html` and stayed out of the archive, feed, sitemap, and publish contents. `/` redirects to `/index.html` (200). Ctrl+C stopped the preview within 2 s (exit 130, tested by sending SIGINT to the process group as a terminal does). The Checks commands passed._
- [x] T066 [BUG] Publishing is broken: the first publish since 2024 (the PR #18 merge, run 37229952098) failed at `gitPublishPush` with `TransportException: … not authorized`. The personal access token in `GRGIT_PASS` (set 2024-05-22) has expired. Publish with the built-in `GITHUB_TOKEN` instead (`GRGIT_USER: x-access-token`, job permission `contents: write`), and request a Pages build explicitly (`pages: write`), because a push made with `GITHUB_TOKEN` might not start one (`.github/workflows/gradle.yml`)
  - **Test:** After merging, the push run's "Publish content" and "Request a GitHub Pages build" steps succeed. `git fetch origin master && git diff --stat live-2026-10 origin/master` lists only `feed.xml` (see [`reports/linux-build-vs-live-2026-10-04.md`](reports/linux-build-vs-live-2026-10-04.md)). `gh api repos/jmcgarr/jmcgarr.github.io/pages/builds/latest --jq '.status + " " + .commit'` shows `built` for the new `master` commit. `curl -s https://www.mikemcgarr.com/feed.xml | grep -m1 lastBuildDate` shows the new build time.
  - _Done 2026-10-04: PR #20 merged, and push run 37231079943 published `master` `cbca98c`. `git diff --stat live-2026-10 origin/master` showed only `feed.xml` (2 lines), Pages showed `built` for `cbca98c`, and the live feed showed `lastBuildDate` Sun, 4 Oct 2026 20:11:32. That run proved a `GITHUB_TOKEN` push **does** start a Pages build by itself (at 20:11:49). The explicit request started a second build that cancelled the first, which Pages then recorded as "errored: Page build failed". The follow-up PR removes the request step and `pages: write`, and makes `github-actions[bot]` the author of the publish commit instead of `runner <runner@…>` (checked locally with `-Duser.home` pointing at a bot-only `.gitconfig`)._
- [x] T067 [BUG] `tags/.NET.html` is on `master` but returns 404. GitHub Pages runs the site through Jekyll because there's no `.nojekyll`, and Jekyll hides files whose names start with `.` or `_`. Add an empty `.nojekyll` so Pages serves the files as built and skips the Jekyll step (47–80 s per build). No published file has front matter, so Jekyll currently passes every file through unchanged and turning it off can't change any page that's served today. The only effect is that 36 hidden files (`tags/.NET.html` plus 35 SCSS/Less partials, removed in M2 by T019/T020) become reachable (`src/jbake/assets/.nojekyll`)
  - **Test:** `build/jbake/.nojekyll` exists after a bake, and a local publish commit adds it. After publishing: `curl -s -o /dev/null -w '%{http_code}' https://www.mikemcgarr.com/tags/.NET.html` returns `200`, the full baseline sweep returns 260 × 200 and 12 × 404 (drafts), 15 sample live files (pages, CSS, JS, images, sitemap) match byte-for-byte before and after, and the Pages build `duration` drops.
  - _Done 2026-10-05: PR #24 merged, publish run 37254189282 (`master` `0e0b953`) changed only `A .nojekyll` and the `feed.xml` timestamp. `tags/.NET.html` returns **200**, listing its 2 posts. The full sweep returned **260 × 200 and 12 × 404** (drafts). 14 of 15 sample live files were byte-identical before and after. `CNAME` now serves its 19-byte content instead of a 404 page (harmless). The Pages build took **31.7 s**, down from 47–48 s._
- [x] T068 [BUG] `https://mikemcgarr.com` (bare domain, HTTPS) fails with a certificate mismatch. Its DNS A records point to `192.30.252.153`/`.154`, GitHub Pages addresses retired in 2018, which present a `*.github.com` certificate. GitHub's certificate covers only `www.mikemcgarr.com`. **Owner action at the domain registrar:** point `@` A records to `185.199.108.153`, `185.199.109.153`, `185.199.110.153`, `185.199.111.153` (optionally AAAA `2606:50c0:8000::153` … `8003::153`), and keep `www` CNAME → `jmcgarr.github.io`. If GitHub doesn't issue a certificate covering both names, remove and re-add the custom domain under Settings → Pages. Also consider verifying the domain in GitHub to prevent takeover (DNS)
  - **Test:** `dig +short mikemcgarr.com A` lists only `185.199.108-111.153`. `curl -sI https://mikemcgarr.com/` returns `301` to `https://www.mikemcgarr.com/` with no certificate error. `gh api repos/jmcgarr/jmcgarr.github.io/pages --jq '.https_certificate.domains'` includes both names. Then re-submit the sitemap for T007.
  - _2026-10-05: the owner updated the A records (TTL 1 h). Verified: GoDaddy's nameservers and Google/Cloudflare/Quad9/OpenDNS all return only `185.199.108–111.153`, and `http://mikemcgarr.com` redirects to `https://www.mikemcgarr.com/` from the new servers. **Waiting on GitHub** to issue a certificate covering the bare domain (usually within hours, up to about a day). If it hasn't by 2026-10-06, remove and re-add the custom domain in Settings → Pages (this briefly drops `www` while `CNAME` is re-committed). **Remind the owner** about optional GitHub domain verification when this closes._
  - _Done 2026-10-06. The certificate stayed stuck in `dns_changed` for ~20 h after the remove-and-re-add, through a GitHub Actions/Pages outage. **It was issued shortly after Pages switched to Actions (T027):** `approved` for **`www.mikemcgarr.com` and `mikemcgarr.com`** (Let's Encrypt YR2, expires 2027-01-04). **Enforce HTTPS** had been cleared by the re-add, and GitHub refused to re-enable it until the certificate existed. It was re-enabled via the API (`https_enforced: true`). Verified: `http://www`, `http://` bare, `https://` bare, and `https://` bare with a path all **301 → `https://www.mikemcgarr.com/…`**, keeping the path, and `https://www` returns 200. Domain verification in GitHub is still optional; the owner was reminded._

### Improvements

- [x] T015 [IMP] Add a GitHub Actions status badge to the README (`README.md`)
  - **Test:** The badge renders on the GitHub repo page and reflects the latest `source` run.
  - _Done 2026-10-05: the badge uses `badge.svg?branch=source&event=push`, which reports **passing**. Without `event=push` GitHub reported "no status" even though the latest `source` push run (37254189282) succeeded. It reflects the runs that build and publish `source`. Re-check on the repo page after merge. **Follow-up (after PR #25 merged):** `branch=source&event=push` flipped between "no status" and "passing" across repeated requests, and GitHub's runs API returned nothing for `branch=source`, so its branch lookup is unreliable for this repo. `?event=push` alone reported "passing" on every request. The workflow's push trigger only fires for `source`, so it means the same thing. The badge now uses `badge.svg?event=push` (5/5 "passing")._

### Checkpoint: M1

- [x] CHK006 The sitemap is accepted in Search Console and the feed passes the W3C validator _(2026-10-05: the owner checked both. The W3C validator passed, and Search Console shows Success.)_
- [x] CHK007 No drafts are reachable on the live site _(2026-10-05: all 12 return 404 in the full sweep)_
- [x] CHK008 HTTPS is enforced, and the build has no `http://www.mikemcgarr.com` self-links _(2026-10-05. The bare domain's HTTPS is T068.)_
- [x] CHK009 Home, a post, About, and Archive load with **zero** console errors _(2026-10-05, Lighthouse on the live site: 0/0/0/0)_
- [x] CHK010 A test PR ran CI without publishing _(PR #18, 2026-10-04)_
- [x] CHK011 `check-urls.sh` reports only the expected draft removals _(2026-10-05: also confirmed live, with 260 × 200 and 12 × 404)_

---

## M2: Lighten the Load

> **Status: closed 2026-10-06 by owner decision.** CHK014–CHK016 pass. CHK012 (size) and CHK013 (performance on all four pages) are **waived by the owner** ("we are good on performance for now"). Results: site **125 MB → 21.8 MB**. Lighthouse mobile, median of 5, live: **home 96, about 93, archive 96, post 81** (from 72/67/65/56). The post page's remaining gap and its likely fix are in the Backlog (T075).

**Goal**: A first visit is fast on a phone. No page ships more than ~1 MB of images, and the
published site shrinks from ~130 MB to ≤ 15 MB.

**Independent Test**: `du -sh build/jbake` ≤ 15 MB. Lighthouse mobile Performance ≥ 90 on the four
baseline pages. An offline link check finds no missing assets.

### Bugs

- [x] T016 [BUG] Re-encode the oversized masthead backgrounds. Pages currently download 13–16 MB hero images: `hawaii.png`, `london-alley.png`, `london-view.png`, `Los-Gatos_02.jpg`, plus anything else over 500 KB. Target ≤ 1920px wide, JPEG or WebP, ≤ 400 KB. If an extension changes, update the `masthead=` front matter and `archive.ftl` (`src/jbake/assets/img/masthead/`)
  - **Test:** `find src/jbake/assets/img/masthead -size +500k` is empty. A side-by-side visual check in preview looks right. Lighthouse LCP on `/about.html` and `/archive.html` improves against `docs/baseline/metrics.md`.
  - _Done 2026-10-05 (`perf/T016-masthead-images`). **Image URL policy is option B (owner's choice):** published image URLs are never removed. A photo PNG gets a new `<name>.jpg` that pages use, and the old `<name>.png` is replaced with a small fallback (≤ 1024px, 256 colors) for anyone hotlinking it. JPEGs are re-encoded in place under the same name. Every image is converted to **sRGB** before its profile is dropped (the iPhone PNGs were Apple wide-color and `festival.jpg` was Adobe RGB), resized to ≤ 1920px wide, and saved as an optimized progressive JPEG (quality stepping down from 82 until ≤ 400 KB), with camera metadata (EXIF/XMP: make, model, timestamps) stripped. No GPS data was present. Results: `img/masthead/` **76 MB → 5.5 MB**. 0 files over 500 KB (`Los-Gatos_02.jpg` is 445 KB at q60; at full delivered size it looks the same as the original). New `hawaii.jpg`, `london-alley.jpg`, and `london-view.jpg` are used by `about.asciidoc`, the 2019 post, and `archive.ftl`. All 14 live masthead URLs still resolve. Side-by-side checks (full image and 100% crops) show matching color and no visible artifacts. Lighthouse in the local preview: **About 15.1 → 1.9 MB, LCP 80.9 → 11.0 s; Archive 13.8 → 0.9 MB, LCP 74.4 → 6.6 s** (the baseline was live, so compare weight and LCP, not scores; re-measure live after publish). About's heaviest file is now the inline `me_qcon_panel_sq.png` (770 KB), which is T017. **Live after PR #29 (Lighthouse mobile, median of 3):** About **15.1 → 1.6 MB, LCP 80.9 → 9.9 s, Perf 67 → 73**. Archive **13.8 → 0.7 MB, LCP 74.4 → 5.7 s, Perf 65 → 77**. All 17 masthead URLs return 200, each ≤ 455 KB._
- [x] T017 [P] [BUG] _(Use T016's option B method: never remove a published image URL.)_ Re-encode oversized inline post images, including `me_qcon_panel_sq.png` (770 KB, on About): `mike-oscon-1.png` (8 MB), `mike-oscoon-2.jpg` (3 MB), `three-horizons-lean-enterprise.png`, the `qcon-*.png` files, and anything else over 500 KB (`src/jbake/assets/img/`)
  - **Test:** `find src/jbake/assets/img -maxdepth 1 -size +500k` is empty, or lists only exceptions you've documented. The affected posts render correctly in preview.
  - _Done 2026-10-05 (`perf/T017-inline-images`), using T016's option B method (sRGB first, metadata stripped, no image URL removed). **16 files over 500 KB, 30 MB → about 4 MB. `img/` (whole tree) is now 13 MB, down from 110 MB at baseline.** These images display at 300–500px, so inline images are capped at **1000px** (2× the largest display width). `qcon_crowd` is a header and gets 1920px.
    - **JPEG in place:** `mike-gradle-meetup.jpg` (2.0 MB → 67 KB), `mike-oscoon-2.jpg` (3.0 MB → 178 KB), `mcgarr-qcon.jpg` (unused, 1600px).
    - **PNG → new `.jpg` plus small `.png` fallback** (photos, or slides with a photo background): `discovering-culture-artifacts`, `me_qcon_panel_sq`, `mike-oscon-1` (7.7 MB → 112 KB), `qcon-microcultures-talk`, `qcon-ny-talk`, `qcon_crowd`. 6 references updated in `about`, `speaker-bio`, and `talks`.
    - **PNG graphics optimized in place** (text and flat color stay PNG): `netflix-culture-enron.png` (0.6 MB → 162 KB, dithered) and `three-horizons-lean-enterprise.png` (1.8 MB → 309 KB). Side-by-side checks show crisp text and matching colors.
    - **Unused PNG photos shrunk to fallbacks** (part of T018): `me_qcon_heh`, `me_qcon_panel`, `me_qcon_so`, `me_qcon_yo`, `qcon_crowd_closer`.
    - Verified: `find src/jbake/assets/img -maxdepth 1 -size +500k` is empty. All **316 local image references** on all pages resolve, and **all 85 live image URLs still exist** (6 `.jpg` added). Images per page, before → after: Speaker Bio **12.94 → 0.67 MB**, Talks **3.47 → 0.44 MB**, About 1.31 → 0.63 MB, `three-horizons-part1` 2.19 → 0.70 MB._

- [x] T070 [BUG] `asciidoctor.css` begins with `@import url(https://cdnjs.cloudflare.com/ajax/libs/font-awesome/3.2.0/css/font-awesome.css)`, a **second, older icon library** that is render-blocking on every page (~850 ms in Lighthouse mobile, 2026-10-05). Nothing uses it: no rule in `asciidoctor.css` references FontAwesome, and no content uses AsciiDoc admonitions or `:icons:`. Remove the import (`src/jbake/assets/css/asciidoctor.css`)
  - **Test:** `grep -c '@import' src/jbake/assets/css/asciidoctor.css` returns `0`. Lighthouse no longer lists `cdnjs…/font-awesome/3.2.0` under render-blocking resources. Pages look the same.
  - _Done 2026-10-05 (`perf/T070-T072-render-blocking`): import removed. Pixel comparison of About, a post, and mobile nav before/after: **0 differing pixels**._

### Improvements

- [x] T018 [P] [IMP] **Closed 2026-10-05 by owner decision: keep the 41 unused images (option B, no image URL removed). T016/T017 already shrank every unused image over 500 KB, and the rest total 5.3 MB.** _(Under option B, deleting a published image URL needs the owner's explicit approval. The default is to shrink unused images in place, as T016 did with `santa-cruz.png` (12.9 MB → 342 KB fallback), unless the owner approves removal. 2026-10-05: T017 already shrank the 6 unused images over 500 KB, so what's left is small.)_ Delete the ~24 unused images (~25 MB), for example `masthead/santa-cruz.png`, the unused `me_qcon_*.png` files, `bg-*.png`, `glyphicons-*`, and `webicon-*.svg` (`src/jbake/assets/img/`)
  - **Test:** Re-running the unused-image scan (every image basename grepped against content, templates, and CSS) returns nothing. `lychee --offline build/jbake` reports 0 missing local files.
- [x] T019 [P] [IMP] Cut Font Awesome (13 MB) down to `css/all.min.css` and `webfonts/`, or replace it with inline SVGs for the handful of icons in use (footer circles, Twitter, LinkedIn, GitHub, menu bars) (`src/jbake/assets/vendor/fontawesome-free/`)
  - **Test:** The footer and menu icons render in preview. `du -sh src/jbake/assets/vendor/fontawesome-free` is under 1 MB, or the directory is gone. Offline lychee is clean.
- [x] T020 [P] [IMP] Remove unused JS and CSS: unminified and slim jQuery/Bootstrap variants, `*.map` files, `contact_me*.js`, and `jqBootstrapValidation*.js`. Also stop publishing `scss/` by moving the SCSS sources out of `assets/` (`src/jbake/assets/vendor/`, `src/jbake/assets/js/`, `src/jbake/assets/scss/`)
  - **Test:** At mobile width the navbar collapse and toggle work. On desktop the scroll-up navbar reveal works. `test ! -e build/jbake/scss` passes. Offline lychee is clean.
  - _Done together with T019, 2026-10-05 (`perf/T019-T020-unused-assets`). The owner approved removing unused published library and source files, and chose to **keep Font Awesome, trimmed** rather than switch to inline SVGs. **1,431 files no page references were removed** with `git rm`: Font Awesome's 1,344 icon SVGs plus its LESS/SCSS/JS/sprites/extra CSS, unused Bootstrap and jQuery builds and source maps, `clean-blog.js`, the contact-form scripts, and the stale `clean-blog.min.css`. Font Awesome's `LICENSE.txt` stays for attribution. **SCSS moved to `src/scss/`** (kept as source for T043, no longer published). What's kept is exactly what pages load: `bootstrap.min.css`, `bootstrap.bundle.min.js`, `jquery.min.js`, Font Awesome `all.min.css` + `webfonts/`, `clean-blog.min.js`, and the three site CSS files. Removals are recorded as groups in `docs/baseline/expected-removals.txt`. **Published site 33 MB → 19 MB (1,832 → 391 files).** Verified: all **3,027 local asset references** (pages + CSS `url()`) resolve, footer icons render, and the mobile header with the "Menu ☰" button is identical to the live site. Docs guard and URL check OK._
- [x] T021 **Won't do (owner decision, 2026-10-05).** [P] [IMP] Limit the RSS feed to the 20 most recent posts. It's currently 440 KB with all 75 (`src/jbake/templates/feed.ftl`)
  - **Test:** `grep -c '<item>' build/jbake/feed.xml` returns `20`. The file is under 150 KB and still validates.
  - _Measured value was too small to be worth it: the feed is served **gzipped** (123 KB for all 74 posts, 52 KB if capped at 20), and GitHub Pages answers conditional requests, so readers that remember the feed's ETag get `304 Not Modified` (0 bytes) unless it has changed. The cap would save ~71 KB per reader **only when the feed changes** (about once per publish). The full feed also gives new subscribers the whole back catalogue. PR #33 (capped at 20) was closed. Revisit if the feed grows a lot once posting resumes._
- [ ] T022 [IMP] **Deferred 2026-10-05 (owner):** don't change fonts yet. The owner wants to settle on the *right* fonts first, and is open to optimizing font downloads afterwards (for example, Google Fonts loads 10 Open Sans + 4 Lora variants, which are used only for the nav, buttons, and header subheadings). Was: Self-host Lora and Open Sans, or drop them for the system font stack (this continues the May 2024 font changes) (`src/jbake/templates/header.ftl`, `src/jbake/assets/css/clean-blog.css`)
  - **Test:** The DevTools Network tab shows no requests to `fonts.googleapis.com` or `fonts.gstatic.com`. Headings and body text look right on home and on a post.

- [x] T071 [IMP] **The header photo is the largest content (LCP) on every page**, but it's a CSS background, so the browser only discovers it after the CSS loads, and phones get the 1920px version. Serve **width-appropriate variants** (960px for phones, 1440px for tablets and small laptops, the full ≤ 1920px image for larger screens) chosen by media query, and **preload** the right one so it downloads in parallel with the CSS. Generate the variants with a committed script, keeping option B (new URLs only, none removed). WebP was considered: it saves only 60–120 KB per photo beyond resizing, and browsers that support WebP but not CSS `image-set(… type())` would download the image twice, so add it only if still short of 90 (`src/jbake/templates/masthead.ftl`, `src/jbake/assets/css/extra.css`, `scripts/`)
  - **Test:** every masthead in use has its 960/1440 variants. At 412px wide the page requests the 960px variant **once** (no double download) and no 1920px image. Lighthouse lists the header image as preloaded, and LCP on the four baseline pages improves against the M2 checkpoint run.
  - _Done 2026-10-05 (`perf/T071-responsive-mastheads`). `scripts/masthead-variants.py` creates `<stem>-960.jpg` and `<stem>-1440.jpg` for every masthead in use (front matter `masthead=` and template defaults): **15 mastheads, 30 new files**, phone variants mostly 100–130 KB versus ~380 KB full. New URLs only, nothing removed (option B). sRGB, no metadata. It never overwrites, and `--check` fails if any are missing (now a CI step; the negative test fails and names the missing files). `masthead.ftl` puts the three URLs in CSS custom properties on `<header>` (valid HTML, no `<style>` in body), and `extra.css` picks one at `<576px` / `<1280px` / `≥1280px`. Three `<link rel="preload" media=…>` use **the same** queries. Remote (http) mastheads, for T037, skip variants. 960 rather than 768 keeps 3× phones sharp; Lighthouse doesn't flag oversized CSS backgrounds. Verified: 0 template errors. On all **270** pages with a masthead, the preloads equal the CSS URLs, and all 45 image URLs exist. **Lighthouse mobile, local preview, #34 vs + T071:** each page fetches **exactly one** header image (the 960px variant, once). LCP: home 6.3 → 4.7 s, about 8.0 → 6.0 s, archive 6.6 → 4.8 s, post 8.4 → 6.7 s. FCP is unchanged at ~3.2 s, so render-blocking CSS is the remaining limit. (The local preview has no compression or CDN, so compare deltas and re-measure live.) AGENTS.md and README explain adding a masthead. WebP is not done (see above); revisit if still short of 90._
- [x] T072 [IMP] Load only the fonts the site actually uses, without changing how anything looks (owner: "don't compromise fonts"). **Lora is never displayed:** its only rule, `header.masthead .post-heading .meta`, matches no element on any page, yet its CSS request was render-blocking (~900 ms). Open Sans is used in exactly three styles: 300 (header subheadings), 300 italic (post dates), and 800 (nav). Replace both Google Fonts requests with one `css2` request for those three styles, plus `preconnect` to `fonts.googleapis.com` and `fonts.gstatic.com`. `font-display` is unchanged (`src/jbake/templates/header.ftl`)
  - **Test:** the new request serves exactly Open Sans 300, 300 italic, and 800. Screenshots of the nav, header subheading, and post date are **pixel-identical** before and after. A control screenshot with Google Fonts blocked **differs**, which proves the comparison rendered the real fonts.
  - _Done 2026-10-05 (`perf/T070-T072-render-blocking`): the font URL serves exactly those 3 faces. Before vs after: **0 differing pixels** on About, a post (desktop), and the About mobile nav. The control with Google Fonts blocked differs by 5,835 px (About) and 4,112 px (mobile), in the nav and subheading regions. If `.post-heading .meta` is ever used, it falls back to Times New Roman, the next font in its stack, so add Lora back then._

- [x] T073 [IMP] Self-host Open Sans. After T070–T072 and T071 went live (2026-10-06, Lighthouse mobile, median of 3: home 83, about 71, archive 83, post 73), the **Google Fonts stylesheet was the largest render-blocking resource (~870 ms)**, and a cross-origin hop in the critical path. It also drove the large run-to-run swings in FCP (1.7–3.2 s; post scored 70/87/73, home 79/83/94). Serve **the same font files Google serves** from `/fonts/open-sans/` with a same-origin `fonts.css`, and preload the Latin normal file (every page's nav uses it). Include the SIL OFL license, which redistribution requires. The owner approved (fonts must look the same) (`src/jbake/assets/fonts/open-sans/`, `src/jbake/assets/css/fonts.css`, `src/jbake/templates/header.ftl`)
  - **Test:** the self-hosted files are byte-identical to Google's. Screenshots are **pixel-identical** to the Google-hosted version **with Google's font servers blocked**. No page references `fonts.googleapis.com` or `fonts.gstatic.com`.
  - _Done 2026-10-06 (`perf/T073-self-host-fonts`): Google's CSS for the 3 styles has 30 rules over **20 woff2 files** (variable font: one per style per subset, 10 subsets: latin, latin-ext, cyrillic, cyrillic-ext, greek, greek-ext, hebrew, math, symbols, vietnamese) and no `font-display`. All 20 were downloaded, `wOF2` signatures checked, and latin normal/italic re-downloaded and **byte-identical**. `fonts.css` reproduces all 30 rules (same `unicode-range`s and descriptors) with local URLs. `OFL.txt` (SIL OFL 1.1, from google/fonts) is published alongside. Browsers download only the subsets a page needs, as before (latin normal 42 KB, italic 14 KB of the 336 KB total). **Pixel comparison, Google-hosted vs self-hosted with fonts.googleapis.com and fonts.gstatic.com blocked: 0 differing pixels** on About and a post at 1280px and 500px. T072's control showed these regions change when Open Sans is missing, so the self-hosted fonts render identically and need nothing from Google. Build: 0 pages reference Google Fonts, and 270/270 use `/css/fonts.css`._
- [x] T074 [IMP] Right-size in-post images. Live Lighthouse flags images much larger than they're displayed, about 1 s of savings on About and posts (for example `profile_pic-sq.jpg`, 183 KB, displayed at 300px). Re-encode **in place** (same URL, option B) to at most twice the width each page displays it at (from the `width` attribute in the rendered HTML, or the ~730px text column when there is none). PNGs stay PNG, JPEGs stay JPEG (`src/jbake/assets/img/`)
  - **Test:** no in-post image is wider than 2× its largest display width. All image URLs still exist. A side-by-side visual check looks right. Lighthouse's "Properly size images" savings drop on About and the baseline post.
  - _Done 2026-10-06 (`perf/T074-inline-image-sizes`). Display widths come from the rendered pages (`<img width>`, capped at the ~730px column): 44 in-post images, **16 more than 2× wider** than displayed. **13 re-encoded in place** (option B, same URLs): JPEGs converted to sRGB at q82 progressive; PNGs resized **losslessly** (no quantization, so text stays crisp; alpha kept). Examples: `sample-roadmap.png` 3744px/414 KB → 1460px/192 KB, `trello-personal-kanban.png` 3108px/384 KB → 1460px/182 KB, `profile_pic-sq.jpg` 183 → 44 KB, `mike-oscoon-2.jpg` 178 → 67 KB. 2 skipped automatically because a lossless resize would have been bigger (`HTML5_Logo_512.png`, `week-in-review.png`, both palette PNGs). **`jax-devops-speaking.jpg` was restored**: it's also a masthead (CSS background), which the `<img>`-only scan missed, and no other resized file is a masthead. Those 16: **2,156 KB → ~1,140 KB**. A side-by-side at display size (roadmap text, Trello labels, profile photo) shows no visible difference. All image references resolve. Docs, URL, and masthead checks pass._

### Checkpoint: M2

- [ ] CHK012 `du -sh build/jbake` ≤ 15 MB (baseline ~130 MB) **Waived by owner 2026-10-06:** **21.8 MB** in 433 files (from 125 MB / 1,824). Over target because the owner chose to keep unused images (T018, 5.3 MB) and Font Awesome (2.4 MB), and because the performance work added header variants (T071, ~5 MB) and self-hosted font subsets (T073, 336 KB).
- [ ] CHK013 Lighthouse mobile Performance ≥ 90 on `/`, `/about.html`, `/archive.html`, and one post (recorded in `docs/baseline/metrics.md`) **Waived by owner 2026-10-06:** home **96**, about **93**, archive **96**, post **81** (live, median of 5). On the post page, LCP (the header) competes for bandwidth with a 308 KB in-post diagram; see T075 in the Backlog.
- [x] CHK014 `lychee --offline build/jbake` reports 0 missing local assets _(2026-10-06: checked with an equivalent script, since lychee isn't installed. All 856 local image references in the build resolve, every asset referenced by live pages and CSS returns 200, all 121 live image URLs and 20 font files return 200)_
- [x] CHK015 No visual regressions on the four baseline pages at mobile and desktop widths _(2026-10-05: before/after screenshots of the live site at 1280px and 500px look the same. Fonts were pixel-identical after T072 and T073.)_
- [x] CHK016 `check-urls.sh` reports 0 unexpected missing HTML URLs _(2026-10-06: all 260 kept URLs return 200 live, and the 12 drafts are expected removals)_

---

## M3: Modern Toolchain

> **Status: done 2026-10-07.** T023–T032, T092, and T093 are complete, and checkpoints CHK017–CHK022 and CHK037 pass. Gradle 8.14.5 + JDK 21 (output identical to the old toolchain), deploys via GitHub Pages Actions from `main` with no secrets, quality gates on every PR, and a post-deploy smoke test of the live site.

**Goal**: Any machine, including Apple Silicon with a current LTS JDK, builds the site with one command.
Deploys happen only from `source` and need no personal tokens.

**Independent Test**: A fresh clone on Apple Silicon with JDK 21 runs `./gradlew clean bake` successfully.
CI is green. Its output is identical to the pre-upgrade build, and deploying needs no repo secrets.

### Bugs

- [x] T023 [BUG] Make local and CI use the same JDK. Today `.java-version` is `1.8` and CI uses `11`. After T024/T025, standardize on JDK 21 (LTS) (`.java-version`, `.github/workflows/gradle.yml`)
  - **Issue:** Refs #3
  - **Test:** In the repo, `java -version` reports 21. The workflow has `java-version: '21'`. The bake succeeds locally and in CI.
  - _Done 2026-10-06 with T024: `.java-version` is `21.0` (jenv → Temurin 21.0.3), the workflow uses `java-version: '21'`, and README/AGENTS say JDK 21. **Fresh clone on Apple Silicon (arm64)** with that JDK: `./gradlew clean bake` exit 0 on Gradle 8.14.5, 0 deprecations, 0 `sun.misc.VM`. CI on JDK 21 is verified by this PR's run._

### Improvements

- [x] T024 [IMP] Upgrade the Gradle wrapper from 5.6.4 to the latest 8.x, going through 6.9 and 7.6 and fixing deprecations at each step (`gradle/wrapper/gradle-wrapper.properties`, `build.gradle`)
  - **Test:** `./gradlew --version` reports 8.x. `./gradlew clean bake --warning-mode all` has no deprecation warnings. **Output-equivalence check:** before upgrading, copy a clean bake to `/tmp/bake-before`. After upgrading, `diff -r /tmp/bake-before build/jbake` is empty, or every difference is explained.
  - _Done 2026-10-06 (`chore/T024-T023-gradle-8-jdk-21`). Wrapper **5.6.4 → 6.9.4 (JDK 1.8) → 7.6.6 (JDK 11) → 8.14.5 (JDK 21)**. At every step `clean bake --warning-mode all` reported **0 deprecation warnings**, and the output was **identical** to a pre-upgrade baseline (Gradle 5.6.4 / JDK 1.8) apart from the feed timestamp. **Two Gradle 8 issues found and fixed:** (1) the plugin's `bakePreview` fails ("Cannot set readonly property: level"; plugin 5.5.0 is its last release). It's replaced by a `preview` Exec task serving `build/jbake` with the JDK's built-in `jwebserver` on `127.0.0.1:8080`, and `bakePreview` now runs it, so the command is unchanged. Tested: pages, drafts, a tag with a space, CSS, fonts, images, and the feed all 200. Ctrl+C frees the port with no leftover process. About page fonts are pixel-identical to the old preview, apart from the photo T074 re-encoded. (2) JRuby (in AsciidoctorJ) extracted `jffi*.dylib` into the project root. `jffi.extract.dir` now points at `build/tmp/jffi`, plus `.gitignore` entries. Verified with a fresh daemon. Dependabot ignores Gradle majors, so 9.x stays a deliberate future step._
- [x] T025 [IMP] Upgrade `org.jbake.site` from 5.0.0 to the latest 5.x, along with its bundled JBake version (`build.gradle`)
  - **Issue:** Closes #3. JBake 2.6.x's OrientDB calls `sun.misc.VM`, which doesn't exist after JDK 8. Reproduced 2026-10-04: a bake on JDK 11 (Corretto 11.0.23, the version CI uses) succeeds but logs `ClassNotFoundException: sun.misc.VM` stack traces. JDK 1.8 logs none. CI shows it too: 6 occurrences in PR #18's Temurin 11 bake.
  - **Test:** The same output-equivalence `diff -r` as T024. Tags, archive, feed, and sitemap are all present. On JDK 11, `./gradlew clean bake --info 2>&1 | grep -c 'sun.misc.VM'` returns `0` (JDK 21 needs Gradle ≥ 8.5, so that check moves to T024).
  - _Done 2026-10-06. **Dependabot PR #45** bumped `org.jbake.site` 5.0.0 → **5.5.0**, which bundles **JBake 2.6.4 → 2.6.6**, and the owner merged it. CI (JDK 11) was fine, but **local bakes broke on Apple Silicon**: the plugin pulls in JNA 4.5.0, whose native library is Intel-only (`missing compatible architecture (have 'i386,x86_64', need 'arm64…')`). JDK 11 and 21 fail the same way, and this is the likely cause of the May 2025 "Reverting to JDK 1.8 to run on Apple Silicon" commit. **Fix (`fix/jna-apple-silicon`):** `build.gradle` forces JNA **5.17.0** (API-compatible, arm64 included) through `resolutionStrategy`. **Output equivalence:** a bake with plugin 5.0.0 (commit `f5245a0`, in a temporary worktree) vs 5.5.0 + JNA fix differs in **all 272 HTML/XML files, but only in the footer's `JBake v2.6.4` → `v2.6.6` and the feed's build timestamps**. Same file set, and tags, archive, feed, and sitemap are present. **Issue #3:** `sun.misc.VM` errors on JDK 11 went **6 → 0** in CI (last 5.0.0 run 37485353735 vs 5.5.0 runs 37493675373/37494030937), and locally JDK 11 + the fix logs 0 `sun.misc.VM` and 0 `MaxDirectMemorySize` warnings. JDK 1.8 also bakes. `check-urls.sh` and the masthead check pass._
- [x] T026 [P] [IMP] Delete the dead build files: `.travis.yml` (travis-ci.org shut down in 2021), plus `ci.gradle` and `publish.gradle`, which are never applied and use jcenter and gradle-git 0.8 (repo root)
  - **Test:** `./gradlew clean bake` still works. `grep -r 'ci.gradle\|publish.gradle' .` finds no references.
  - _Done 2026-10-06 (`chore/T026-dead-build-files`): removed `.travis.yml`, `ci.gradle`, and `publish.gradle` with `git rm`. Nothing applied or referenced them (only the plan mentioned them). `./gradlew clean bake` still succeeds, and the old `github-pages` plugin tasks are gone._
- [x] T027 [IMP] Move deploys to native GitHub Pages: `actions/upload-pages-artifact` (path `build/jbake`) plus `actions/deploy-pages`, triggered only on push to `source`. Set Pages to "GitHub Actions" and confirm the custom domain `www.mikemcgarr.com` is still set in Settings → Pages (`.github/workflows/gradle.yml`)
  - **Test:** The deploy job succeeds. `gh api repos/jmcgarr/jmcgarr.github.io/pages --jq .build_type` returns `workflow`. The live site serves the new build and `check-urls.sh` reports 0 missing against the live URLs. **Re-run the T004 docs guard**: the uploaded artifact has no `docs/`. **Keep T009's exclusion:** `build/jbake` contains drafts, so upload a copy without `**/*-draft.html`, and point the drafts check at the artifact. The artifact must have no `-draft.html`.
  - _2026-10-06 (`chore/T027-T028-pages-actions`), **implemented, waiting on the owner's changeover**. The workflow now has two jobs. **`build`** (`contents: read`): bake, then **stage `build/site` = `build/jbake` minus `*-draft.html`** (rsync), then run every gate **on the staged folder** (drafts, docs guard, masthead variants, `check-urls.sh`, `xmllint`, lychee), then `actions/upload-pages-artifact` (pinned v5.0.0) on deploy events. **`deploy`** (`pages: write`, `id-token: write`, environment `github-pages`, concurrency group `pages`, never cancels a running deploy): `actions/deploy-pages` (pinned v5.0.1), only for push or `workflow_dispatch` on `source`. The new **Run workflow** button gives one-click redeploys and rollback. The artifact skips hidden files; `.nojekyll` isn't needed with Actions. Locally the staged folder matches live `master` file for file (only `.gitignore`, a local `.DS_Store`, and the macOS case pair differ), and all gates pass on it. **Changeover:** the owner sets Settings → Pages → Source to "GitHub Actions", then merges. Rollback: set Source back to "Deploy from a branch: `master` /", since `master` keeps the last git-publish build. Tick after the live checks._
  - _Done 2026-10-06. The owner switched Pages to **GitHub Actions** and merged PR #44. Run 37479053313: build (all gates on `build/site`) → upload → **deploy succeeded**, and `build_type` is `workflow`. **A regression was found by the live sweep and fixed:** `upload-pages-artifact` drops dot-files by default, so `tags/.NET.html` (T067) returned 404. The gates had checked `build/site`, which still had the file. PR #47 set `include-hidden-files: true`, so the artifact now equals what's checked, and the page returns 200 again. Live after #46/#48: **260/260** pages, 12 drafts 404, `docs/` 404, every referenced asset 200. The README documents the pipeline, settings, DNS, and redeploy/rollback (#44). Follow-up: T092 adds a post-deploy smoke test, so a deployed-vs-checked gap like this is caught automatically._
- [x] T028 [IMP] Remove the `org.ajoberstar.git-publish` plugin and its `gitPublish` block now that Actions deploys the site (`build.gradle`)
  - **Test:** `./gradlew tasks --all | grep -i gitPublish` returns nothing. A push to `source` still deploys, and still without drafts (T009's exclusion lives in `gitPublish.contents` today, so move it to T027's artifact step first).
  - _2026-10-06: removed in the same branch as T027. The `org.ajoberstar.git-publish` plugin, the `gitPublish {}` block, and `gitPublishCommit.dependsOn` are gone, and `./gradlew tasks --all` shows 0 gitPublish tasks. T009's draft exclusion moved to the workflow's staging step. AGENTS.md and the README now describe deploying via Pages. Tick once a push deploys without drafts._
  - _Done 2026-10-06 (PR #44): there's no git-publish plugin, config, or tasks. Pushes since then deploy via Actions with no drafts in the artifact (the staging step excludes them and "Check drafts are not published" verifies it)._
- [x] T029 [IMP] Delete the `GRGIT_USER` and `GRGIT_PASS` repo secrets and revoke the personal access token behind them (GitHub settings)
  - **Test:** `gh secret list` no longer shows them. The next deploy still succeeds.
  - _Note (2026-10-04): the workflow stopped using these secrets in T066, and the token had already expired. This can be done as soon as T066's publish succeeds, ahead of the rest of M3. **2026-10-04: both secrets deleted** with the owner's approval (`gh secret list` is empty). The next deploy succeeded without them (T009 merge, run 37250459609). **2026-10-05: the owner deleted the expired token.** Done._
- [x] T030 [P] [IMP] Add Dependabot for `github-actions` and `gradle` (`.github/dependabot.yml`)
  - **Test:** The Insights → Dependency graph → Dependabot tab shows both ecosystems being checked.
  - _2026-10-06 (`chore/T030-dependabot`): `.github/dependabot.yml` sets **weekly** checks for `github-actions` and `gradle`, each **grouped into one PR** per ecosystem. Dependabot also updates SHA-pinned actions (`setup-gradle`, `lychee-action`) along with their version comments. Gradle **major** updates are ignored, because JBake/Gradle majors are planned work (T024/T025, or replaced by the Roq option). **Verify after merge:** GitHub only validates the config on the default branch, so check Insights → Dependency graph → Dependabot for both ecosystems with no config errors. Tick then._
  - _Done 2026-10-06 (PR #43). GitHub accepted the config: the first Dependabot update runs for `github_actions` and `gradle` succeeded and opened **#46** (checkout v4→v7, setup-java v4→v6, setup-gradle v3.1→v6.4) and **#45** (`org.jbake.site` 5.0.0→5.5.0, which delivered T025). Both merged. #45 needed the Apple Silicon JNA fix (#48)._
- [x] T031 [P] [IMP] Add CI quality gates on every PR: `xmllint` on the feed and sitemap, `lychee --offline build/jbake`, `scripts/check-urls.sh`, and `scripts/check-docs-not-published.sh` (`.github/workflows/gradle.yml`)
  - **Test:** A throwaway PR that deletes an image used by a post fails CI and names the missing file.
  - _2026-10-06 (`chore/T031-ci-quality-gates`): three new CI steps run before publish on every PR and push. **(1)** `scripts/check-urls.sh build/jbake`, strict on Linux. **(2)** `xmllint --noout` on `feed.xml` and `sitemap.xml` (installs `libxml2-utils` if missing). **(3)** `lycheeverse/lychee-action` (pinned to v2.9.0's commit, lychee v0.24.2), run **offline** over every built page with `--root-dir build/jbake`. It fails on any broken internal link or missing image/CSS/JS/font; external links are T050. `--exclude '^file://[^/]'` skips protocol-relative embeds (`//www.youtube.com/…`, `//www.slideshare.net/…`), which offline mode misreads as local files (12 false positives). The gate's first local run found **two real broken internal links, both 404 on the live site**, fixed by changing only the `href` target: `impress_js_github.html` `/img/cd-at-mcjug.html` → `/blog/cd-at-mcjug.html`, and `improving-my-shell-fu-oh-my-zsh.html` `tags/shell-fu.html` (resolved to `/blog/tags/…`) → `/tags/shell-fu.html` (CR line endings preserved). Locally: 8,186 links, 0 errors. **Negative test (local):** a build copy with `img/three-horizons-lean-enterprise.png` removed fails with exit 2 and names `blog/three-horizons-part1.html` and the missing file. **CI-verified:** PR #40's run 37417209831 passed every new step on Linux ("all 272 baseline URLs accounted for"). Throwaway draft PR #41 (since closed, branch deleted) removed that image, and run 37417351745 **failed at "Check local links and assets"**, naming `blog/three-horizons-part1.html` and the missing file, with publish skipped. Lychee isn't installed locally (a verified release binary in `/tmp` was used); CI uses the action._
- [x] T032 [IMP] Clean up the branches. Decide what to do with `post/five-disciplines` (finish and merge, or park it; it has diverged from origin). Tag each stale branch as `archive/<name>` before deleting it: `blog/*`, `guard`, `feature/update-look-and-feel`, `post/three-horizons-part2`. After T027, retire `master` (it's already preserved by `live-2026-10`). Also resolve the two open content PRs, [#5](https://github.com/jmcgarr/jmcgarr.github.io/pull/5) `[WIP] Three Horizons Part 2` (last updated 2021-02-14) and draft [#17](https://github.com/jmcgarr/jmcgarr.github.io/pull/17) `The Five Disciplines of Management`: finish and merge, or close. Merging publishes the post, so that's the owner's call, and it should wait until T008 and T009 are done (git remotes, GitHub PRs)
  - **Test:** `git branch -r` lists only `origin/source` plus any branches you're actively working on. `git ls-remote --tags origin 'archive/*'` lists every removed branch. `gh pr list --state open` shows no stale PRs.
  - _Done 2026-10-06, owner-approved. **Archived as tags first** (each tag verified to equal its branch tip), then deleted: `archive/guard`, `archive/blog/centralized-team`, `archive/blog/change-management`, `archive/blog/change-resistance`, `archive/blog/beyond-the-culture-deck` (old unmerged, may hold unpublished drafts), and `archive/feature/update-look-and-feel` (2014). **32 remote branches deleted:** 26 fully merged into `source`, the 5 archived unmerged ones, and `perf/T019-T020-unused-assets` (its one extra commit was cherry-picked into `source` via #34, confirmed with `git cherry`). **Kept:** `source`; `master`, deliberately **not retired**, because it's the README's emergency rollback ("Deploy from a branch: `master`") and also preserved by `live-2026-10`; and **PRs #5 and #17 left open by the owner**, with their branches. Local cleanup: 28 merged branches deleted with `git branch -d`, plus 2 whose commits exist elsewhere (`git cherry` shows `-`). Local `post/five-disciplines` is kept (it has a commit with no upstream). GitHub's "automatically delete head branches" setting is off, so merged PR branches will pile up again unless it's turned on._

- [x] T092 [IMP] Post-deploy smoke test: after `deploy-pages` succeeds, fetch a handful of known live URLs and fail the run if any isn't 200. Cover the home page, a post, a tag page **including `tags/.NET.html`**, `/feed.xml`, `/sitemap.xml`, a header image, and a font file. Also confirm a draft and `docs/00-REVIVAL.md` return 404. The gates check `build/site` **before** upload, so anything lost between upload and the live site (like the hidden-file drop fixed in #47) is otherwise invisible until a manual sweep. Retry briefly to allow for CDN propagation (`.github/workflows/gradle.yml`, `scripts/`)
  - **Test:** a push run shows the smoke step passing with every URL listed. Negative test (run locally, because only `main` can deploy since T093, so a throwaway branch never reaches the smoke job): `scripts/smoke-test.sh https://www.mikemcgarr.com 'blog/no-such-post.html 200'` exits 1 and names the URL.
  - _Done 2026-10-07 (#53): `scripts/smoke-test.sh` + a `smoke-test` job after `deploy`. Locally against the live site: 15/15 as expected, exit 0. Negative: a bogus 200 and `tags/.NET.html` expected 404 → both named as FAIL, exit 1. **First real run**, the merge of #53 (run 37650256965): build, deploy, and smoke-test all succeeded, with every URL listed as `ok` on the first attempt._

- [x] T093 [IMP] Rename the default branch `source` → `main` (owner's request, now that T027 retired `master` as the publishing branch). The PR updates the workflow (triggers accept `main` and, during the transition only, `source`; **deploy only from `refs/heads/main`**), `AGENTS.md`, and `README.md`. Historical notes in this plan keep saying `source`. **Changeover:** (1) owner: Settings → General → Default branch → rename `source` to `main`, and GitHub retargets open PRs; (2) allow `main` in the `github-pages` environment's deployment branch policy, which currently allows only `source`; (3) merge the PR, whose merge is the first deploying push to `main`; (4) owner updates local clones (`git branch -m source main && git fetch origin && git branch -u origin/main main && git remote set-head origin -a`); (5) follow-up: drop the transitional `source` trigger and the `source` deployment policy (`.github/workflows/gradle.yml`, `AGENTS.md`, `README.md`, GitHub settings)
  - **Test:** `gh api repos/jmcgarr/jmcgarr.github.io --jq .default_branch` returns `main`, and `git ls-remote --heads origin source` returns nothing. The merge push to `main` builds **and deploys**, and the live sweep passes. Open PRs #5, #17, and #49 target `main`. A local `git status -sb` shows `main...origin/main`.
  - _Done 2026-10-07. The owner renamed `source` → `main`: `default_branch` is `main`, `source` no longer exists, and PRs #5/#17/#50 were retargeted to `main` automatically. The `github-pages` deployment policy had allowed only `source`; `main` was added before merging #50, and `source` was deleted afterwards. **The first deploy from `main`** (merge of #50, run 37562901359, JDK 21) built, uploaded, and deployed (GitHub deployment `ref=main`). Live sweep: 260/260 pages (`tags/.NET.html` 200), 12 drafts 404, 129/129 assets, feed and sitemap valid, `docs/` 404, both bare-domain URLs 301 → https://www. The owner's local clone tracks `origin/main` with `origin/HEAD → origin/main`. This follow-up drops the transitional `source` trigger._

### Checkpoint: M3

- [x] CHK017 Fresh clone + JDK 21 on Apple Silicon: `./gradlew clean bake` succeeds with no deprecation warnings _(2026-10-06: fresh clone, arm64, Temurin 21.0.3, Gradle 8.14.5: exit 0, 0 deprecations)_
- [x] CHK018 The rendered output matches the pre-upgrade build (or every difference has been reviewed) _(2026-10-06: Gradle 8.14.5/JDK 21 vs 5.6.4/JDK 1.8: identical except the feed timestamp. Plugin 5.0→5.5 differed only by the footer's JBake version, see T025.)_
- [x] CHK019 Push to `source` deploys through GitHub Pages Actions. PRs run the quality gates and never deploy _(2026-10-06: since #44; every push run deploys, and PR runs skip upload/deploy)_
- [x] CHK020 No PAT-based secrets remain _(2026-10-05: `GRGIT_*` deleted, expired token deleted. Deploys use OIDC `id-token` + `pages: write`)_
- [x] CHK021 The docs guard still passes against the Pages artifact _(2026-10-06: the guard runs on `build/site`, which is exactly the uploaded artifact since #47 added `include-hidden-files`. Live `docs/00-REVIVAL.md` returns 404)_
- [x] CHK022 Only active branches remain, and old ones are preserved as `archive/*` tags _(2026-10-06: remote = `source`, `master` (kept for rollback), and the branches of open PRs #5, #17, #49. 6 `archive/*` tags.)_
- [x] CHK037 Issue #3 is closed by the merged T025 PR, and bakes on JDK 11/21 log no `sun.misc.VM` errors _(2026-10-06: **#3 closed** by PR #48, and JDK 11 logs 0 errors in CI and locally. JDK 21 on Gradle 8.14.5 also logs 0 (T024).)_

---

## M4: Chart the Future

> **Status: done 2026-10-07.** The owner accepted [`01-PLATFORM-DECISION.md`](01-PLATFORM-DECISION.md): **stay on JBake** and turn on modern Markdown, with **Roq, then Hugo** as the successor order if a re-evaluation trigger fires. T033 and CHK023 pass. T034 and CHK024 are not applicable. Follow-ups are T094–T097 (M7) and T098 (Backlog). T099 (M7) was found later, while doing T094.

**Goal**: Decide deliberately whether to stay on JBake or move to a mainstream generator before
spending effort on template polish.

**Independent Test**: An accepted decision record exists. If the decision is to migrate, a spike shows
that representative content can move with its URLs unchanged.

### Bugs

_None. This milestone is a decision gate._

### Improvements

- [x] T033 [IMP] Write a decision record comparing staying on JBake with Hugo, Astro, Eleventy, and **Roq (Quarkus)** (`docs/01-PLATFORM-DECISION.md`). The owner asked to consider Roq (2026-10-06). Its fit assessment, go/no-go criteria, and phased migration plan (T076–T091) are in [`02-ROQ-MIGRATION.md`](02-ROQ-MIGRATION.md). Criteria: support for the existing formats (legacy HTML, AsciiDoc, Markdown, front matter), keeping URLs the same, theme effort, toolchain upkeep, community health, and the writing workflow: new-post scaffolding, watch/rebuild, incremental builds, and live reload (issues #8–#12, M7)
  - **Test:** The record has Context, Options, Decision, and Consequences sections, and its status is **Accepted**.
  - _2026-10-07 (`docs/T033-platform-decision`): draft written, [`01-PLATFORM-DECISION.md`](01-PLATFORM-DECISION.md). Owner's requirements: **Java first, Go second, no frameworks in dynamic languages (Ruby, JS/TS, Python)**, and good **Markdown** support (he's considering moving off AsciiDoc). It compares JBake, Roq, and Hugo in depth; Eleventy and Astro are excluded by the language rule. **Proposed: stay on JBake and turn on modern Markdown (one tested config line); successor order Roq, then Hugo.** New evidence: the current setup works on Gradle 9.8.0; JBake 2.7.0 runs without the unmaintained plugin via `JavaExec` with identical output; JBake's default Markdown settings mangle tables, footnotes, and line wraps; a missing `summary=` breaks the build with a misleading error. **Tick when the owner accepts** (Status → Accepted)._
  - _**Accepted by the owner 2026-10-07.** Decision: stay on JBake and turn on modern Markdown; successor order Roq, then Hugo. Follow-up tasks T094–T098 added (M7 and Backlog)._
- [ ] T034 [IMP] _(Only if migrating. For Roq, this is Phase 1 of [`02-ROQ-MIGRATION.md`](02-ROQ-MIGRATION.md), T076–T081, which also covers tag URLs, feed `<guid>`s, and AsciiDoc fidelity)_ Run a spike: port three representative posts (one HTML, one AsciiDoc, one Markdown) plus the archive page to the chosen platform (separate branch)
  - **Test:** `scripts/check-urls.sh`, run against the spike output, keeps all four URLs. A side-by-side visual check against the live pages looks right.
  - _**Not applicable (2026-10-07):** the accepted decision is to stay on JBake, so there's no migration spike. If a re-evaluation trigger in [`01-PLATFORM-DECISION.md`](01-PLATFORM-DECISION.md) fires, the Roq spike (T076–T081) runs instead._

### Checkpoint: M4

- [x] CHK023 `docs/01-PLATFORM-DECISION.md` exists with status Accepted _(2026-10-07)_
- [ ] CHK024 If migrating: the spike preserves URLs, and M5 has been re-scoped for the new platform (M5 tasks assume JBake/FreeMarker) **Not applicable 2026-10-07:** staying on JBake, so M5 stands as written.

---

## M5: Polished Presentation

**Goal**: Every page is valid HTML, search engines can find it, and links to it share nicely on social media.

**Independent Test**: The HTML validator reports 0 template-originated errors. Lighthouse SEO ≥ 95 and
Accessibility ≥ 95. Social preview validators show a title, description, and image for posts.

### Bugs

- [ ] T035 [BUG] `<p>${content.body}</p>` wraps block-level HTML in a paragraph, which is invalid HTML. _(The same `<article>` restructuring is the likely fix for T069 in the backlog, so do them together.)_ Use `<article>` for posts and `<div>` for pages (`src/jbake/templates/post.ftl`, `src/jbake/templates/page.ftl`)
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
  - _2026-10-07 (M4): with T094, Markdown fenced code renders as `<pre><code class="language-xxx">`, so a client-side highlighter that reads `language-*` classes covers new Markdown posts. Note: JBake has no build-time highlighting for Markdown (Hugo does; see [`01-PLATFORM-DECISION.md`](01-PLATFORM-DECISION.md))._

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
- [x] T045 [P] [IMP] **Decided 2026-10-05: switch to Giscus, and export the existing Disqus comments first** (owner). Was: decide on comments: keep Disqus, switch to Giscus (GitHub Discussions), or remove them (`src/jbake/templates/post.ftl`). _2026-10-05: on the **live** site, the Disqus embed makes a post page load **164 requests from 46 third-party hosts**, most of them ad and data-sync trackers (Taboola, Criteo, DoubleClick, Amazon ads, Facebook, LinkedIn ads, Yahoo, The Trade Desk, LiveRamp, and others). Our own code loads none of these. This works against the cookie-free analytics choice (T013), so consider pulling it forward, and export the existing Disqus threads first if any are worth keeping._
  - _2026-10-05 (`feat/T045-giscus`): the owner installed the Giscus app and created the **Comments** Discussions category (`DIC_kwDOAR4Yus4DHHAz`, repo `MDEwOlJlcG9zaXRvcnkxODc0OTYyNg==`). Its format looks like open-ended rather than Announcement; the owner should check, since Announcement means only the owner and Giscus can start threads. `post.ftl` now embeds Giscus instead of Disqus: `pathname` mapping with strict matching, so a post's thread is tied to its permanent URL; lazy loading; light theme. `giscus.json` at the repo root (not published) limits embedding to `https://www.mikemcgarr.com` and `http://localhost:8080`. Verified: 0 pages mention Disqus, Giscus is on 86 of 86 post pages and no others, and Giscus's API sees the repo ("Discussion not found" for a real post, against "not installed" for another repo). In the local preview the widget loads and queries `term=blog/three-horizons-part1` (404, no thread yet). **Post page: 0 console errors, Best Practices 100, third-party hosts 46 (live, with Disqus) → 8, none of them ad trackers.** **Disqus archive (2026-10-05):** the owner exported the comments. The export has **no emails or IPs**, only name, username, date, and text. Of 47 comments, 3 deleted and 2 spam were dropped. **The other 42 map to 10 current posts**, with none left unmatched, and 15 are replies. `scripts/disqus-archive.py` (stdlib only; the raw export is never committed) writes read-only snippets to `src/jbake/templates/archived-comments/`. They keep display name, date, and text, with replies threaded, and comment HTML is rebuilt from an allow-list (`<p>`, `<br>`, http(s) links with `rel="nofollow ugc noopener"`). `post.ftl` includes a snippet when one exists (`ignore_missing`), inside a collapsed `<details>` above Giscus, wrapped in `<#noparse>`. Verified: bake has 0 template errors, all 42 comments render on exactly the 10 posts with matching counts, and a rendered screenshot shows correct names, dates, and threading. **Live (PR #30 merged, publish run 37357918073, `master` `e7ab20b`):** the publish changed only the 74 post pages, `extra.css`, and `feed.xml`, and `giscus.json` stayed unpublished. All 260 live pages return 200, with **no Disqus embed code anywhere**. Giscus is on 74 of 74 posts and nowhere else. **All 42 archived comments are live on exactly the 10 expected posts, with matching counts.** `giscus.json` is enforced: the widget loads for `www.mikemcgarr.com` (200) and other origins are redirected to Giscus's blocked-origin page (307)._
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

_**M4 decided 2026-10-07: stay on JBake** ([`01-PLATFORM-DECISION.md`](01-PLATFORM-DECISION.md)), so these tasks are built on JBake. Do **T096** (JBake run directly, without the unmaintained plugin) before T062–T064, because watch and live reload hook into how the bake runs. T094 and T095 can go first: they make the next post, in Markdown, work well._

### Bugs

_The owner filed #8–#12 as enhancements. T095 was found while testing for the M4 decision, T099 while testing T094, and T100 while testing T096._

- [x] T095 [BUG] A post without `summary=` **breaks the whole build**. `index.ftl` prints `${post.summary}` for the newest six posts, so a new post that leaves it out fails with "Failed to render masterindex". The unmaintained Gradle plugin then crashes while logging that error, so it surfaces as a confusing Groovy error (`No signature of method … error()`) instead of the template message (found 2026-10-07, see [`01-PLATFORM-DECISION.md`](01-PLATFORM-DECISION.md)). Make `summary` optional (`${post.summary!""}`, or omit the `<p class="post-subtitle">` when it's missing) and keep it in the README's new-post template (`src/jbake/templates/index.ftl`)
  - **Test:** Locally, add a temporary published post with no `summary=` line and run `./gradlew clean bake`: it succeeds, and the home page lists the post without an empty subtitle. Remove the temporary post. With no temporary post, the output is identical to before (`diff -r` against a pre-change bake, ignoring `feed.xml`).
  - _Done 2026-10-07 (`feat/T094-T095-modern-markdown`): `index.ftl` prints the subtitle only `<#if post.summary?has_content>`, on the same line so the output for posts with a summary is unchanged. A temporary published post with no `summary=`, dated today: the bake succeeds, and the home page lists it with no empty subtitle (`post-subtitle"></p>` absent). **Control:** the same post with the old template fails the bake. After removing the post, a clean bake is byte-identical to the pre-change bake except `feed.xml`'s build timestamp. The README now says `summary` is optional but recommended. **Correction:** the "Java heap space" errors seen during the M4 testing weren't this bug, they were the daemon leak in T099._

- [x] T099 [BUG] **The Gradle daemon runs out of memory after 3 bakes.** Every `./gradlew bake` leaves about 60–160 MB in the long-lived Gradle daemon, which has a 512 MiB default heap. The **4th bake in the same daemon fails with "Java heap space"** and writes an ~800 MB `java_pid*.hprof` heap dump into the repo root. Measured 2026-10-07 on `main` and on the T094 branch alike: heap after each bake 236 → 429 → 431 MB, then fail. Cause: the JBake plugin runs each bake with `classLoaderIsolation` inside the daemon, and the old classloaders are never freed (likely JRuby or OrientDB threads). CI is unaffected (one bake per run). Locally, `./gradlew --stop` clears it. **Fix:** T096 runs JBake in a fresh JVM per bake, which removes the leak. If T096 is delayed, a stopgap is `org.gradle.jvmargs` with a larger heap in `gradle.properties`, which only postpones it. Either way, add `*.hprof` to `.gitignore` (`build.gradle`, `.gitignore`)
  - **Test:** after `./gradlew --stop`, 10 consecutive `./gradlew bake --rerun-tasks` runs in the same daemon all succeed, and no `java_pid*.hprof` appears in the repo root (`git status --short` is clean).
  - _Done 2026-10-07 with T096 (`chore/T096-T099-jbake-direct`). The bake now runs in its own JVM, so nothing accumulates in the daemon. After `./gradlew --stop`, 10 consecutive `./gradlew bake --rerun-tasks` runs in one daemon (default 512 MiB heap) all succeed. Daemon heap after each: 95, 119, 144, 166, 54, 75, 96, 117, 137, 157 MB, so it's collected normally instead of climbing to the limit. No `java_pid*.hprof` appeared, and `*.hprof` is now in `.gitignore`._

- [ ] T100 [P] [BUG] **The local preview can't show `tags/.NET.html`.** Since T024, `bakePreview` serves `build/jbake` with the JDK's `jwebserver`, which deliberately refuses any path containing a dot-file, so `/tags/.NET.html` (and `/.nojekyll`) return 404 in the preview. The file is baked, and the live site serves it (200, checked by the smoke test, T092), so **this is preview-only**. Fix options: serve with JBake's own built-in server (`-s`, which needs `jetty-server`, optional in jbake-core). That would also give T062 watch-and-rebuild. Or use a small custom static server. Do it with T062 (`build.gradle`)
  - **Test:** during `./gradlew clean bakePreview`, `curl -s -o /dev/null -w '%{http_code}' 'http://localhost:8080/tags/.NET.html'` prints `200`. Drafts still preview, and Ctrl+C still leaves no process.

### Improvements

- [ ] T060 [IMP] Add a Gradle task that starts a new post. It creates `<slug>.asciidoc` with the front matter filled in (`title`, today's `date`, `type=post`, `tags`, `status=draft`, `summary`, then `~~~~~~`), and creates and switches to a `post/<slug>` branch. It must refuse to overwrite an existing file, because that URL may already be published. Create it with `status=draft` in `src/jbake/content/blog/drafts/`. Per T009, drafts render for local preview but are never published (`build.gradle`)
  - **Issue:** Refs #8 (steps 1–3 of the issue)
  - **Test:** `./gradlew newPost -Ptitle="Hello World"` creates `hello-world.asciidoc` with every header field, and `git branch --show-current` prints `post/hello-world`. Running it again fails with "already exists". After `./gradlew bake`, the draft previews at `/blog/drafts/hello-world-draft.html`. It isn't in the archive or feed, and `./gradlew gitPublishCopy` leaves it out of the publish contents.
  - _2026-10-07 (M4): the owner is considering Markdown for new writing. Support both formats, for example `-Pformat=md|asciidoc`, defaulting to whichever the owner prefers when this is built. Needs T094 for Markdown to render well._
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

- [x] T094 [P] [IMP] **Modern Markdown on JBake.** The site has no `markdown.extensions` setting, so JBake's defaults apply (`HARDWRAPS,AUTOLINKS,FENCED_CODE_BLOCKS,DEFINITIONS`). Line breaks inside a paragraph become `<br>`, and tables, task lists, strikethrough, and footnotes render as literal text (tested 2026-10-07). Set the tested line in `jbake.properties`: `markdown.extensions=AUTOLINKS,FENCED_CODE_BLOCKS,DEFINITIONS,TABLES,STRIKETHROUGH,TASKLISTITEMS,FOOTNOTES,SMARTYPANTS`. Including `SMARTYPANTS` (curly quotes, `…`, dashes) is the owner's call, and it's the only thing that changes an existing page: the typography of `relections-and-projections-2019`. Add a Markdown example to the README's new-post section (`src/jbake/jbake.properties`, `README.md`)
  - **Test:** Locally, add a temporary post with wrapped lines, a table, a task list, `~~strikethrough~~`, a footnote, and a fenced `java` block, then bake. Its HTML has one `<p>` for the wrapped paragraph (no `<br />`), plus `<table>`, `task-list-item`, `<del>`, a `footnotes` block, and `class="language-java"`. Remove the temporary post. `scripts/check-urls.sh` passes, and `diff -r` against a pre-change bake (ignoring `feed.xml`) lists only `blog/relections-and-projections-2019.html` (or nothing without `SMARTYPANTS`), plus `feed.xml`, which carries that post's body.
  - _Done 2026-10-07 (`feat/T094-T095-modern-markdown`), **without `SMARTYPANTS`** (owner's choice): `markdown.extensions=AUTOLINKS,FENCED_CODE_BLOCKS,DEFINITIONS,TABLES,STRIKETHROUGH,TASKLISTITEMS,FOOTNOTES`. A temporary post renders the wrapped paragraph as one `<p>` with no `<br />`, plus `<table>`, 2 `task-list-item` checkboxes, `<del>`, a `footnotes` block, and `class="language-java"`, with straight quotes kept. After removing it, a clean bake (443 files) is byte-identical to the pre-change bake except `feed.xml`'s build timestamp, so `relections-and-projections-2019` doesn't change. `check-urls.sh` (272 OK), the docs guard, masthead check, and `xmllint` pass. Preview: home, the Markdown post, Archive, and About render as before. The README's new-post section now leads with Markdown and has an example. To add smart quotes later, append `,SMARTYPANTS` (it only changes that post's typography)._
- [x] T096 [IMP] **Run JBake directly and upgrade to 2.7.0.** The Gradle plugin `org.jbake.site` is unmaintained: last release 5.5.0 (2021-05-19), last commit 2022-01-02. Its preview already broke on Gradle 8 (T024), it can't run JBake 2.7.0 (`commons-configuration` class missing), it hides template errors (T095), and it leaks memory in the daemon on every bake (T099; a fresh JVM per bake fixes that). Replace it with a `JavaExec` task on `org.jbake:jbake-core:2.7.0` (main class `org.jbake.launcher.Main`, args `src/jbake build/jbake -b`), tested in a scratch clone on 2026-10-07. Keep the commands `./gradlew bake`, `bakePreview`, and `preview`. Also needed: `db.path=cache` (2.7.0 rejects `build/cache`), create the `jffi` extract directory before running, keep the JNA 5.17.0 force, and add an SLF4J binding (`build.gradle`, `src/jbake/jbake.properties`)
  - **Test:** `./gradlew clean bake` succeeds with no deprecation warnings. Its output has the same file list as a pre-change bake and differs only in the footer's `JBake v2.7.0` and `feed.xml` (compare with the version string normalized). All CI gates pass on the PR. `./gradlew clean bakePreview` still serves drafts at `/blog/drafts/<name>-draft.html`. A template error (temporarily break `index.ftl`) is reported with FreeMarker's message, not the plugin's `No signature of method … error()`. Ten consecutive bakes in one Gradle daemon succeed (T099).
  - _Done 2026-10-07 (`chore/T096-T099-jbake-direct`). `build.gradle` has no plugins except `base`: a `jbake` configuration (`jbake-core` 2.7.0 plus FreeMarker 2.3.31, AsciidoctorJ 2.5.7, flexmark 0.62.2 + pegdown profile, picocli 4.6.2, the versions jbake-core 2.7.0 declares) and a `bake` `JavaExec` task with declared inputs/outputs, so an unchanged second run is `UP-TO-DATE`. Also needed: `db.path=cache`, a logback config (`gradle/jbake-logback.xml`, because jbake-core's logback otherwise logs everything at DEBUG; now just JBake's start/finish/summary plus any warnings or errors), and two `--add-opens` flags JRuby asks for. **The JNA 5.17.0 override was removed**, even though the task text says to keep it: JBake 2.7.0's OrientDB (3.1.20) uses `jnr-posix`, so no JNA is on the classpath, and the bake works on Apple Silicon (arm64) without it. **Results:** clean bake exit 0, 0 deprecations. Same 442 files as `main` (JBake 2.6.6 via the plugin): 271 differ only by the footer's `JBake v2.7.0`, `feed.xml` only by its build timestamp, nothing else. A broken `index.ftl` fails the build (exit 1) with FreeMarker's message and the file and line. `bakePreview` serves pages and drafts, and Ctrl+C leaves no process, but `tags/.NET.html` is a 404 in the preview (a `jwebserver` dot-file rule since T024; see T100). CI gates: see the PR run._
- [ ] T097 [IMP] **Gradle 9.** Upgrade the wrapper from 8.14.5 to the current 9.x. The current setup was tested on 9.8.0 on 2026-10-07 (identical output); retest after T096. Dependabot ignores Gradle majors on purpose, so this is a deliberate step (`gradle/wrapper/`, `gradlew`, `gradlew.bat`)
  - **Test:** `./gradlew clean bake --warning-mode all` reports 0 deprecations, and the output is identical to the 8.14.5 build except `feed.xml`. CI on the PR is green, and `./gradlew bakePreview` still works.

### Checkpoint: M7

- [ ] CHK033 The new-post task creates a draft on its own `post/` branch and never overwrites an existing post
- [ ] CHK034 Saving a post updates the open browser within about 2 s, with no manual steps
- [ ] CHK035 `grep -rli livereload build/jbake` returns nothing after a production bake
- [ ] CHK036 Issues #8–#12 are closed by merged PRs (`gh issue list --state open` shows none of them)

---

## Backlog

**Goal**: Keep reports and ideas that aren't scheduled into a milestone yet, so nothing gets lost.
Pick items up when there's new evidence or a milestone touches the same files.

### Bugs

- [ ] T069 [BUG] Safari Reader (reported by a reader on iOS) shows **only the first portion of a post**. Mozilla Readability, a close cousin of Safari's Reader, keeps 96–100% of 10 live posts (2026-10-05), so the problem is specific to Safari's heuristics, which lean on page structure. Likely cause: `post.ftl` wraps the post in `<p>${content.body}</p>`. A paragraph can't contain headings or sections, so the browser closes it at once, leaving the post as **loose sibling blocks** between empty `<p>`s. Those siblings share the column with the share links, an `<hr>`, and the Disqus embed, and nothing marks where the post starts and ends: no `<article>`, no `<main>`, no article metadata. AsciiDoc posts are the clearest case. `three-horizons-part1` parses into a note, an `<hr>`, three `div.paragraph` blocks, and four `div.sect1` section blocks, and a heuristic that groups similar siblings would keep the opening paragraphs and drop the sections. **Fix:** put the post's title, date, and body in one `<article>` inside `<main>` (no `<p>` wrapper), with the share links and comments outside it, and add article metadata (`og:type=article`, published date). This covers the `<article>` part of T035, so do the two together. Ask which post the reader was on, and include it in the test (`src/jbake/templates/post.ftl`, `src/jbake/templates/masthead.ftl`, `src/jbake/templates/header.ftl`)
  - **Test:** On **iOS Safari** (macOS Safari's Reader uses the same engine and is handy for quick checks), open Reader on at least 4 posts: an AsciiDoc post with sections (`blog/three-horizons-part1.html`), a Markdown post (`blog/relections-and-projections-2019.html`), a legacy HTML post (`blog/sonar.html`), and the long `blog/the-modern-tech-resume.html`, plus the reported post if known. Reader shows the title and **the full text through the last section**, without share links or comments. Automated proxy: every built post has exactly one `<article>` containing all of the post's words and none of the share/comment markup, and Mozilla Readability still keeps ≥ 95% on the same posts.
  - _2026-10-05: moved to the backlog at the owner's request. The owner tested on iOS and things **seemed fine**, so the report isn't reproduced. Reopen with the specific post (and iOS version) if it's reported again. The fix above is still worth doing when T035 is picked up._

### Improvements

- [ ] T098 [IMP] **Yearly platform check** (next: 2027-10). Review the re-evaluation triggers in [`01-PLATFORM-DECISION.md`](01-PLATFORM-DECISION.md#decision): JBake/`JavaExec` still builds on the newest Java LTS and Gradle major, no unfixed security issues in OrientDB, AsciidoctorJ, or flexmark, whether M7's live reload is good enough, and Roq's maturity. If a trigger fires, start the Roq spike (T076–T081), then Hugo if Roq fails the criteria.
  - **Test:** a dated note under this task records each trigger as fired or not, with evidence (versions tested, links). If one fired, a new decision doc supersedes `01` (Status: Superseded by NN).
- [ ] T075 [IMP] Lazy-load in-post images, so they don't compete with the header image (LCP) for bandwidth on slow connections. On `three-horizons-part1` (live 2026-10-06), the header's LCP has a 1.8 s load delay plus 1.8 s load time while a 308 KB diagram downloads alongside it. That page scores 81 against 93–96 elsewhere. Fix: in `post.ftl`, render `${content.body?replace("<img ", "<img loading=\"lazy\" decoding=\"async\" ")}`. In-post images are always below the full-width header, so they can never be the LCP. It covers AsciiDoc, Markdown, and legacy HTML posts, and doesn't affect the feed (`feed.ftl` uses `post.body` directly). Proposed while pushing M2 to 90; deferred when the owner closed M2 (`src/jbake/templates/post.ftl`)
  - **Test:** every `<img>` inside post bodies has `loading="lazy"`, and none outside them do. Lighthouse mobile (live, median of 5) on `three-horizons-part1` is ≥ 90, and the header no longer waits on the diagram.

---

## Notes

- **Keep URLs stable.** This site has links pointing at it from about 15 years of the web. Any task that changes a URL must leave a redirect stub, and `check-urls.sh` is what enforces that.
- **Ship each milestone separately.** Publish after each checkpoint passes, then update `docs/baseline/metrics.md` so the next milestone measures against the new numbers.
- **Keep the issue map current.** Check `gh issue list` at each milestone checkpoint. Any new issue gets a task and a row in [GitHub Issues](#github-issues).
- **Option docs:** [`02-ROQ-MIGRATION.md`](02-ROQ-MIGRATION.md) is a possible JBake → Roq migration (T076–T091). It isn't scheduled; it feeds the M4 decision. If Roq is chosen, it replaces M3's T023–T025 and re-scopes M5/M7 (see its "What changes" table). T027 (Pages via Actions) applies either way.
- **Number future planning docs** after this one (`01-PLATFORM-DECISION.md`, `02-...`). They all live in `docs/`, which T004/T005 keep off the live site.
