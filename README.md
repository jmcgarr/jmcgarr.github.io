[![Build and publish](https://github.com/jmcgarr/jmcgarr.github.io/actions/workflows/gradle.yml/badge.svg?event=push)](https://github.com/jmcgarr/jmcgarr.github.io/actions/workflows/gradle.yml?query=event%3Apush)

My Blog
==================
Source for https://www.mikemcgarr.com, built with [JBake](https://jbake.org) and Gradle.

Requirements
============
- **JDK 21** (CI uses 21 too). `.java-version` selects it if you use [jenv](https://www.jenv.be). Works on Apple Silicon.
- Nothing else: `./gradlew` downloads the right Gradle version.

To preview
==========
```
./gradlew clean bakePreview
```
Then open http://localhost:8080. Stop it with `Ctrl+C`. (`./gradlew preview` does the same. It serves the baked
site with the JDK's built-in `jwebserver`.) One known gap: `jwebserver` refuses paths with a dot-file, so
`/tags/.NET.html` is a 404 in the preview only. The live site serves it (T100).

`./gradlew bake` runs JBake 2.7 directly in its own JVM (`build.gradle`). If a template or post has an error, the
build fails and prints JBake's message with the file and line.

Drafts (posts with `status=draft`) are rendered for preview only, at
`http://localhost:8080/blog/drafts/<name>-draft.html`. They are never published.

To write a post
===============
Create `src/jbake/content/blog/drafts/<slug>.md` (Markdown) or `<slug>.asciidoc` (AsciiDoc), starting with a
header like this. It's JBake's `key=value` format ending in `~~~~~~`, not YAML front matter:

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

`summary` is optional but recommended: it's the teaser under the title on the home page.

Markdown is GitHub-style: wrap lines however you like (single line breaks don't break the paragraph), and use
tables, task lists (`- [ ]`), `~~strikethrough~~`, footnotes (`[^1]`), and fenced code blocks with a language:

````
Some text that wraps
across two lines is one paragraph.

| Tool  | Language |
|-------|----------|
| JBake | Java     |

```java
System.out.println("hello");
```

A claim that needs a source.[^1]

[^1]: The source.
````

If you use a new header image (`masthead=`), put it in `src/jbake/assets/img/masthead/` (≤ 1920px wide) and run
`scripts/masthead-variants.py` (needs Pillow: `pip install pillow`) to create its phone and tablet sizes.

When it's ready, set `status=published` and move it to `src/jbake/content/blog/`. Its URL will be
`/blog/<slug>.html`. Published URLs are permanent, so pick the slug carefully (see [AGENTS.md](AGENTS.md)).

To publish
==========
There is no manual publish step. Open a pull request against `main`. When it's merged,
[GitHub Actions](.github/workflows/gradle.yml) bakes the site, runs the checks, and **deploys it to GitHub Pages**.
Pull requests run the same build and checks but never deploy. To redeploy (for example, to roll back by
re-running an older run), use **Actions → Java CI with Gradle → Run workflow** on `main`.

How the site is built and deployed
==================================
Everything happens in one workflow, [`.github/workflows/gradle.yml`](.github/workflows/gradle.yml)
(shown as **"Java CI with Gradle"** under Actions). It runs on every pull request and every push to `main`.

1. **Bake:** `./gradlew bake` renders `src/jbake/` into `build/jbake/` with JBake (JDK 21). Drafts are
   rendered too, as `<name>-draft.html`, but only so you can preview them locally.
2. **Stage:** `build/jbake/` is copied to **`build/site/` without drafts**. `build/site/` is exactly what goes live.
3. **Check:** every check below runs on `build/site/`, so what's checked is what ships.
4. **Upload:** `build/site/` is packaged as the GitHub Pages artifact (pushes and manual runs on `main` only).
5. **Deploy:** a separate `deploy` job publishes the artifact to GitHub Pages (pushes and manual runs on
   `main` only, never two at once).
6. **Smoke test:** after a deploy, `scripts/smoke-test.sh` fetches a few known live URLs (home, a post, tag pages
   including `tags/.NET.html`, feed, sitemap, an image, a font) and expects 200, plus 404 for a draft and `docs/`.
   It retries for a few minutes to allow for the CDN. If it fails, the run goes red **after** the site is live:
   check the listed URLs, then fix forward or roll back (below). Run it yourself any time: `scripts/smoke-test.sh`.

**Pull requests stop after step 3:** they build and check but never deploy. Merging to `main` *is* publishing.

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
- **Redeploy:** Actions → Java CI with Gradle → **Run workflow** (branch `main`).
- **Roll back a bad change:** revert the PR on GitHub and merge the revert. That deploys the previous content.
  For a faster stopgap, open an older successful run on `main` and **Re-run all jobs**, which rebuilds and
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
