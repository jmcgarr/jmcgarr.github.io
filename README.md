[![Build and publish](https://github.com/jmcgarr/jmcgarr.github.io/actions/workflows/gradle.yml/badge.svg?branch=source&event=push)](https://github.com/jmcgarr/jmcgarr.github.io/actions/workflows/gradle.yml?query=branch%3Asource+event%3Apush)

My Blog
==================
Source for https://www.mikemcgarr.com, built with [JBake](https://jbake.org) and Gradle.

Requirements
============
- **JDK 1.8.** `.java-version` selects it if you use [jenv](https://www.jenv.be). JDK 11 also builds, but it logs harmless OrientDB `sun.misc.VM` errors ([#3](https://github.com/jmcgarr/jmcgarr.github.io/issues/3)).
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

When it's ready, set `status=published` and move it to `src/jbake/content/blog/`. Its URL will be
`/blog/<slug>.html`. Published URLs are permanent, so pick the slug carefully (see [AGENTS.md](AGENTS.md)).

To publish
==========
There is no manual publish step. Open a pull request against `source`. When it's merged,
[GitHub Actions](.github/workflows/gradle.yml) bakes the site, runs the checks, and pushes it to the
`master` branch, which GitHub Pages serves. Pull requests run the same build and checks but never publish.

Don't run `./gradlew gitPublishPush` locally.

Checks
======
```
./gradlew clean bake
scripts/check-urls.sh                  # no published URL may disappear
scripts/check-docs-not-published.sh    # docs/ must never reach the site
```

More
====
- [AGENTS.md](AGENTS.md): working rules for this repo (URLs, branches, drafts, issues)
- [docs/00-REVIVAL.md](docs/00-REVIVAL.md): the current improvement plan

Optional mastheads
==================
- https://unsplash.com/photos/tGTVxeOr_Rs
