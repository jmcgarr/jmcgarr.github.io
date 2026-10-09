[![Build and publish](https://github.com/jmcgarr/jmcgarr.github.io/actions/workflows/gradle.yml/badge.svg?event=push)](https://github.com/jmcgarr/jmcgarr.github.io/actions/workflows/gradle.yml?query=event%3Apush)

My Blog
==================
Source for https://www.mikemcgarr.com, built with [JBake](https://jbake.org) and Gradle.

Requirements
============
- **JDK 21** (CI uses 21 too). `.java-version` selects it if you use [jenv](https://www.jenv.be). Works on Apple Silicon.
- Nothing else: `./gradlew` downloads the right Gradle version, and Gradle downloads the Sass compiler (see
  [To change the site's styles](#to-change-the-sites-styles)). No Node or Ruby.

To preview
==========
```
./gradlew clean bakePreview
```
Then open http://localhost:8080. Stop it with `Ctrl+C`. (`./gradlew preview` does the same.)

**It rebuilds on save, and the open page reloads itself.** While the preview runs, saving a post, a template
(`src/jbake/templates/`), an asset such as CSS (`src/jbake/assets/`), or the theme's SCSS (`src/scss/`) rebuilds the
site with no restart, and the
page open in your browser reloads about 0.5–1.6 s after you save. No browser extension is needed: preview pages carry
a small script (`src/jbake/templates/preview-reload.ftl`) that asks the server every half second whether the page or
its CSS changed. A normal bake leaves the script out, and CI fails if it ever reaches the deploy contents. The
terminal logs each change it picks up, and any template or post error; a page broken by a template error reloads
again once you fix it. If you stop the preview, open pages keep trying for about a minute (and reload once it's back),
then stop; reload them by hand after that.

This is JBake's own server and watcher (`jbake -b -s`), so in the preview the feed, sitemap, and share links point
at `http://localhost:8080`; a normal `./gradlew bake` uses the real address again. Restart the preview after changing
`jbake.properties`, or after deleting or renaming a file (its old page stays in `build/jbake` until `clean`).

`./gradlew bake` runs JBake 2.7 directly in its own JVM (`build.gradle`). If a template or post has an error, the
build fails and prints JBake's message with the file and line.

Drafts (posts with `status=draft`) are rendered for preview only, at
`http://localhost:8080/blog/drafts/<name>-draft.html`. They are never published.

To write a post
===============
Start a draft with one command, from a clean working tree:
```
./gradlew newPost -Ptitle="My Post Title" -Ptags="management, leadership" -Psummary="A short teaser."
```
It creates `src/jbake/content/blog/drafts/my-post-title.md` with the header below filled in (`status=draft`,
today's date) and switches to a new branch `post/my-post-title` made from `main`. Options: `-Pformat=asciidoc`
for AsciiDoc, `-Pslug=...` to choose the URL slug yourself, and `-Pbranch=false` to stay on the current branch.
It refuses a slug that's already a draft, a post, or a published URL, and warns when a tag differs only by
capitalization from an existing one (that would make a second tag page).

Add `-Ppr` to also commit the draft, push `post/my-post-title` to GitHub, and open a **draft** pull request
titled `[WIP] My Post Title`:
```
./gradlew newPost -Ptitle="My Post Title" -Ppr
```
It needs the [GitHub CLI](https://cli.github.com) logged in (`gh auth login`), and checks that first, before it
creates anything. Pull requests never deploy, so the PR is safe to push to as you write. If the commit, push, or
PR step fails, nothing is undone and the error lists the commands to finish by hand. `-Ppr` can't be combined
with `-Pbranch=false`.

To create one by hand instead, make `src/jbake/content/blog/drafts/<slug>.md` (Markdown) or `<slug>.asciidoc`
(AsciiDoc), starting with a header like this. It's JBake's `key=value` format ending in `~~~~~~`, not YAML front
matter:

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

**Tags:** each tag gets a page at `/tags/<tag>.html`, with spaces turned into hyphens (`continuous delivery` →
`/tags/continuous-delivery.html`; `tag.sanitize=true`, T039). Capitals are kept, so write a tag the same way every
time, in lowercase unless it's a name (`devops`, not `DevOps`): `DevOps` and `devops` would make two pages that
each list only some of the posts (T055). Pages show a tag as you wrote it, spaces included ("Tag: continuous
delivery"); only the URL has hyphens. JBake keeps only the hyphenated form, so a tag with a hyphen of its own
(`apt-get`) must be added to `src/jbake/templates/tag-names.ftl`, or it's shown as "apt get"; CI names any such tag.
Every tag is listed with its post count on the **Topics** page, `/tags/` (in the menu; T057).

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

Write links normally. Links to other sites open in a new tab automatically (the templates add `target="_blank"`,
`rel="noopener noreferrer"` and a screen reader hint at build time), so there's nothing to add by hand.

If you use a new header image (`masthead=`), put it in `src/jbake/assets/img/masthead/` (≤ 1920px wide) and run
`scripts/masthead-variants.py` (needs Pillow: `pip install pillow`) to create its phone and tablet sizes.
`masthead=` also takes a full `https://` URL (a remote image, used as-is at every width, so pick one around
1920px wide), or a site path such as `/img/qcon_crowd.jpg` for an image elsewhere under `src/jbake/assets/`.

When it's ready, set `status=published` and move it to `src/jbake/content/blog/`. Its URL will be
`/blog/<slug>.html`. Published URLs are permanent, so pick the slug carefully (see [AGENTS.md](AGENTS.md)).

To change the site's styles
===========================
**Edit `src/scss/`, never `clean-blog.css`.** The theme's stylesheet, `/css/clean-blog.css`, is generated from
`src/scss/clean-blog.scss` and its partials (`_variables.scss` for colors and fonts, `_global.scss`, `_navbar.scss`,
`_masthead.scss`, ...) on every build (T043). It isn't in the repository: `./gradlew compileSass` writes it to
`build/generated/sass/css/clean-blog.css`, and `bake` and `preview` copy it into the site at the same URL as always.
A Sass error fails the build (and CI) with the file and line. The build also refuses to run if a
`src/jbake/assets/css/clean-blog.css` reappears, since JBake would publish it and it would go stale.

While `./gradlew preview` runs, saving a file in `src/scss/` recompiles the CSS within about a second and the open
page reloads. A Sass error there is printed in the terminal, and the page keeps the last good CSS.

Small site-specific additions that aren't part of the theme go in `src/jbake/assets/css/extra.css`, which is
plain CSS and published as-is.

**The compiler** is [Dart Sass](https://sass-lang.com/dart-sass/), the reference Sass implementation, run from
Gradle by the [freefair Sass plugin](https://plugins.gradle.org/plugin/io.freefair.sass-base) through its
Java host, [`sass-embedded-host`](https://github.com/larsgrefer/dart-sass-java). No Node, Ruby, or separate install:
the host's jar (about 49 MB, from Maven Central, cached by Gradle) carries Dart Sass for macOS, Linux, and Windows.
On first use it unpacks the one for your machine into the system temp folder and runs it as a helper process (a
small Dart runtime plus the compiler), talking to it over stdin/stdout. Dependabot keeps the plugin current.

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

1. **Bake:** `./gradlew bake` compiles `src/scss/` into `css/clean-blog.css` with Dart Sass and renders
   `src/jbake/` into `build/jbake/` with JBake (JDK 21). Drafts are rendered too, as `<name>-draft.html`, but only
   so you can preview them locally. A Sass, template, or post error fails the build here.
2. **Stage:** `build/jbake/` is copied to **`build/site/` without drafts**. `build/site/` is exactly what goes live.
3. **Check:** every check below runs on `build/site/`, so what's checked is what ships.
4. **Upload:** `build/site/` is packaged as the GitHub Pages artifact (pushes and manual runs on `main` only).
5. **Deploy:** a separate `deploy` job publishes the artifact to GitHub Pages (pushes and manual runs on
   `main` only, never two at once).
6. **Smoke test:** after a deploy, `scripts/smoke-test.sh` fetches a few known live URLs (home, a post, tag pages
   including `tags/.NET.html`, the Topics page, feed, sitemap, an image, a font) and expects 200, plus 404 for a draft and `docs/`.
   It retries for a few minutes to allow for the CDN. If it fails, the run goes red **after** the site is live:
   check the listed URLs, then fix forward or roll back (below). Run it yourself any time: `scripts/smoke-test.sh`.

**Pull requests stop after step 3:** they build and check but never deploy. Merging to `main` *is* publishing.

| Check | Protects against |
|---|---|
| Drafts are not published | Unfinished `*-draft.html` posts going live |
| Live reload is not published | The preview's live-reload script (marker `livereload`) going live |
| Docs are not published | Anything from `docs/` (planning notes) going live |
| Masthead variants | A header image missing its 960/1440px sizes (phones would get a blank header) |
| Published URLs still exist (`scripts/check-urls.sh`) | Removing or renaming any page that's live (strict on CI's Linux runner) |
| Old tag URLs redirect (`scripts/tag-redirects.py --check`) | An old tag URL that's missing, or whose redirect stub points at a page that isn't there; a tag page that shows its tag differently from the posts |
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
python3 scripts/tag-redirects.py --check build/site   # old tag URLs are tag pages or working redirects
python3 scripts/masthead-variants.py --check     # header images have their phone/tablet sizes
xmllint --noout build/site/feed.xml build/site/sitemap.xml
```
The link checker (lychee) runs in CI. To run it locally, install [lychee](https://lychee.cli.rs) and see the
"Check local links and assets" step in the workflow for its options.

**Old tag URLs (T039, T055):** tag pages used to be published with spaces (`/tags/acceptance test.html`), and two
tags had a capitalized twin (`/tags/DevOps.html`, `/tags/Groovy.html`). Those 50 URLs are now small redirect pages
(meta refresh, `rel=canonical`, `noindex`, and a visible link) to the new pages, made by `scripts/tag-redirects.py`
from `docs/baseline/urls.txt` and committed: 48 in `src/jbake/assets/tags/`, and the 2 capitalized ones in
`src/case-redirects/tags/`. Run the script again if a tag in that list stops being used, and keep its stubs.
**macOS caveat:** macOS filesystems ignore case, so `tags/DevOps.html` and `tags/devops.html` are the same file
there. `./gradlew bake` therefore adds `src/case-redirects/` only when `build/jbake` is case-sensitive (Linux, as
in CI and on GitHub Pages) and says so when it skips them. A local bake on a Mac has the real `devops` and `groovy`
pages but not those 2 stubs, and `check-urls.sh` and `tag-redirects.py --check` report them as "can't check here".
They're not JBake assets on purpose: JBake copies assets after rendering, so on a Mac the stub would overwrite the
real page and redirect to itself.

More
====
- [AGENTS.md](AGENTS.md): working rules for this repo (URLs, branches, drafts, issues)
- [docs/00-REVIVAL.md](docs/00-REVIVAL.md): the current improvement plan

Optional mastheads
==================
- https://unsplash.com/photos/tGTVxeOr_Rs
