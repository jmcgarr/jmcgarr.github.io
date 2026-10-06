[![Build and publish](https://github.com/jmcgarr/jmcgarr.github.io/actions/workflows/gradle.yml/badge.svg?event=push)](https://github.com/jmcgarr/jmcgarr.github.io/actions/workflows/gradle.yml?query=event%3Apush)

My Blog
==================
Source for https://www.mikemcgarr.com, built with [JBake](https://jbake.org) and Gradle.

Requirements
============
- **JDK 1.8 or 11** (CI uses 11). `.java-version` selects 1.8 if you use [jenv](https://www.jenv.be). Works on Apple Silicon.
  JDK 17+ needs the newer Gradle planned in T024.
- Nothing else: `./gradlew` downloads the right Gradle version.

To preview
==========
```
./gradlew clean bakePreview
```
Then open http://localhost:8080. Stop it with `Ctrl+C`.

Drafts (posts with `status=draft`) are rendered for preview only, at
`http://localhost:8080/blog/drafts/<name>-draft.html`. They are never published.

To write a post
===============
Create `src/jbake/content/blog/drafts/<slug>.asciidoc` (or `.md`), starting with a header like this:

```
title=My Post Title
date=2026-10-05
type=post
tags=management, leadership
status=draft
summary=A short teaser shown on the home page.
masthead=three-hills.jpg
mastheadCredit=https://flic.kr/p/rFRzzj
~~~~~~
```

If you use a new header image (`masthead=`), put it in `src/jbake/assets/img/masthead/` (≤ 1920px wide) and run
`scripts/masthead-variants.py` (needs Pillow: `pip install pillow`) to create its phone and tablet sizes.

When it's ready, set `status=published` and move it to `src/jbake/content/blog/`. Its URL will be
`/blog/<slug>.html`. Published URLs are permanent, so pick the slug carefully (see [AGENTS.md](AGENTS.md)).

To publish
==========
There is no manual publish step. Open a pull request against `source`. When it's merged,
[GitHub Actions](.github/workflows/gradle.yml) bakes the site, runs the checks, and **deploys it to GitHub Pages**.
Pull requests run the same build and checks but never deploy. To redeploy (for example, to roll back by
re-running an older run), use **Actions → Java CI with Gradle → Run workflow** on `source`.

How the site is built and deployed
==================================
Everything happens in one workflow, [`.github/workflows/gradle.yml`](.github/workflows/gradle.yml)
(shown as **"Java CI with Gradle"** under Actions). It runs on every pull request and every push to `source`.

1. **Bake:** `./gradlew bake` renders `src/jbake/` into `build/jbake/` with JBake (JDK 11 in CI). Drafts are
   rendered too, as `<name>-draft.html`, but only so you can preview them locally.
2. **Stage:** `build/jbake/` is copied to **`build/site/` without drafts**. `build/site/` is exactly what goes live.
3. **Check:** every check below runs on `build/site/`, so what's checked is what ships.
4. **Upload:** `build/site/` is packaged as the GitHub Pages artifact (pushes and manual runs on `source` only).
5. **Deploy:** a separate `deploy` job publishes the artifact to GitHub Pages (pushes and manual runs on
   `source` only, never two at once).

**Pull requests stop after step 3:** they build and check but never deploy. Merging to `source` *is* publishing.

| Check | Protects against |
|---|---|
| Drafts are not published | Unfinished `*-draft.html` posts going live |
| Docs are not published | Anything from `docs/` (planning notes) going live |
| Masthead variants | A header image missing its 960/1440px sizes (phones would get a blank header) |
| Published URLs still exist (`scripts/check-urls.sh`) | Removing or renaming any page that's live (strict on CI's Linux runner) |
| Feed and sitemap XML | A broken `feed.xml` or `sitemap.xml` |
| Local links and assets (lychee) | Broken internal links, or missing images, CSS, JS, or fonts |

**GitHub settings and DNS this depends on:**
- **Settings → Pages → Build and deployment → Source: "GitHub Actions".**
- **Custom domain:** `www.mikemcgarr.com`, with **Enforce HTTPS** on (Settings → Pages). The HTTPS certificate is
  issued by Let's Encrypt and managed by GitHub automatically.
- **DNS (GoDaddy):** `www` CNAME → `jmcgarr.github.io`. `@` (bare domain) A records → `185.199.108.153`,
  `185.199.109.153`, `185.199.110.153`, `185.199.111.153`.
- **No secrets or tokens.** The build job has read-only access, and the deploy job can only deploy to Pages.

**Redeploy or roll back:**
- **Redeploy:** Actions → Java CI with Gradle → **Run workflow** (branch `source`).
- **Roll back a bad change:** revert the PR on GitHub and merge the revert. That deploys the previous content.
  For a faster stopgap, open an older successful run on `source` and **Re-run all jobs**, which rebuilds and
  redeploys that commit.
- **Emergency:** Settings → Pages → Source → **"Deploy from a branch: `master`"** serves the last build made
  before the switch to Actions (Oct 2026). `master` is otherwise retired, so don't push to it.

**Dependency updates:** Dependabot (`.github/dependabot.yml`) opens weekly PRs for the workflow's actions and
Gradle. Each runs the same build and checks, and never deploys.

**Run the same checks locally:**
```
./gradlew clean bake
rm -rf build/site && rsync -a --prune-empty-dirs --exclude '*-draft.html' build/jbake/ build/site/
scripts/check-docs-not-published.sh build/site   # docs/ must never reach the site
scripts/check-urls.sh build/site                 # no published URL may disappear
python3 scripts/masthead-variants.py --check     # header images have their phone/tablet sizes
xmllint --noout build/site/feed.xml build/site/sitemap.xml
```
The link checker (lychee) runs in CI. To run it locally, install [lychee](https://lychee.cli.rs) and see the
"Check local links and assets" step in the workflow for its options.

More
====
- [AGENTS.md](AGENTS.md): working rules for this repo (URLs, branches, drafts, issues)
- [docs/00-REVIVAL.md](docs/00-REVIVAL.md): the current improvement plan

Optional mastheads
==================
- https://unsplash.com/photos/tGTVxeOr_Rs
