# Reviving mikemcgarr.com: What Changed

**Branch**: `docs/revival-changelog` | **Created**: 2026-10-12 | **Status**: Done

This blog has been online since 2009. By October 2026 it hadn't been touched in two years, the build
tools were years out of date, and publishing had quietly broken. Between **October 4 and October 12, 2026**,
the site was brought back to good health in about 90 small, reviewed pull requests. This is the
plain-language list of what changed. The detailed plan, with every task and how it was tested, is in
[`00-REVIVAL.md`](00-REVIVAL.md).

One rule held throughout: **no link anyone made to this site over the last 15 years was broken.**
Every old page address still works. Where an address had to change, the old one now redirects to the new one.

## By the numbers

| | Before | After |
|---|---|---|
| Size of the published site | 125 MB in 1,824 files | about 22 MB in 433 files |
| Home page download | 1.3 MB | 0.39 MB |
| About page download | 15.1 MB | 0.46 MB |
| Lighthouse performance score on a phone, home / About / Archive | 72 / 67 / 65 | 96 / 93 / 96 |
| Lighthouse performance score on a phone, a post with big diagrams | 56 | 97 |
| Lighthouse accessibility score (home, About, Archive, a post) | 89 / 95 / 89 / 92 | 100 on all four |
| Requests made when loading a post | 178 | about 26 |
| Outside servers a post page contacts | 46, mostly ad trackers | 8, none of them trackers |
| HTML errors across the site | 1,574 | 0 |
| Pages that scroll sideways on a phone | 253 of 258 | 0 |
| Broken links in posts | 275 | all fixed, or kept on purpose (what still fails an automated check is mostly sites that block checkers) |
| Links still using insecure `http://` | 559 | 302 (the rest don't support `https://`) |
| Old page addresses that still work | — | all 272 |
| Time from saving a post to seeing it in the browser | refresh by hand | about 1 second |

## Things that were broken

- **Publishing had stopped working.** The first attempt to publish in two years failed, because the old
  setup relied on a personal access token that no longer worked. Publishing now uses GitHub's built-in
  Pages deployment, with no personal tokens at all, and the old token was revoked.
- **Unfinished drafts were public.** All 12 draft posts had been published at hidden-but-reachable
  addresses. They're now only visible on the author's own machine.
- **The sitemap and RSS feed had broken links:** a missing `/` turned every address into
  `mikemcgarr.comblog/...`. Search engines and feed readers now get working links.
- **`https://mikemcgarr.com`, without the `www`, showed a security warning.** The domain still pointed
  at GitHub server addresses retired in 2018. The DNS was updated, and the whole site is now served over HTTPS.
- **The `.NET` tag page returned "not found",** because GitHub's default processing hides files whose
  names start with a dot. Fixed.
- **The Twitter share button was in French** ("Tweeter"). The Google+ and Facebook buttons were dead, and the
  Google Analytics code had stopped collecting data in 2023.
- **Posts scrolled sideways on phones,** because of one overly wide photo-credit link. Every page now fits
  every screen width from 320 to 1280 pixels.
- **A post without a summary broke the entire build**, and the build ran out of memory after three runs
  in a row. Both fixed.
- **Leftover debris from the 2013 WordPress export** showed up as stray code inside five posts' code
  samples. It was removed, and characters the export had eaten were restored.

## Faster and lighter

- **Header photos shrank from as much as 16 MB each to under 400 KB,** with no visible loss. Phones
  and tablets now get smaller versions sized for their screens.
- **Unused code was removed:** about 1,400 files of icon fonts, JavaScript libraries and stylesheets
  that no page used.
- **Fonts:** only the fonts that are actually displayed are downloaded, and they're served from the site
  itself instead of Google.
- **Images inside posts now load only as you scroll to them,** so the header photo isn't held up by a large diagram.
- **Every published image address was kept.** Other sites still link to some of these images, so
  oversized images were shrunk in place under the same names rather than removed.

## Privacy

- **Disqus comments, and the ad trackers that came with them, are gone.** Loading a post used to contact 46
  outside servers, most of them belonging to ad and data brokers. Comments now use Giscus, which is built on GitHub
  Discussions, and the old Disqus comments were saved and still appear, read-only, under each post.
- **Share buttons are plain links** to X, LinkedIn, Bluesky and email. Nothing loads from those sites
  unless a reader clicks.
- **Google Analytics was replaced by GoatCounter,** a small, privacy-friendly counter.

## Easier to read and find

- **Every page is valid HTML.** The template errors went from 1,556 to 0, and the errors inside old posts from 18 to 0.
- **Accessibility scores are 100** on the pages checked:
  - better color contrast and heading order
  - labels for icons
  - titles on every embedded video and slide deck
  - screen readers told when a link opens a new tab
- **A new "Topics" page** lists every tag with its number of posts.
- **Tags were cleaned up.**
  - Tag addresses no longer contain spaces.
  - Duplicates were merged, for example "presentation", "presentations" and "talks".
  - Typos were fixed, for example "lamdbas" and "google calender".
  - Every old tag address redirects to its new page.
- **Links to other sites open in a new tab,** across all posts, without editing the posts themselves.
- **Better previews when shared:** each page has its own title, description and image for search results
  and social media.
- **Code samples have syntax highlighting.**
- **The footer is current:** the copyright year is up to date, and the links are X, LinkedIn and GitHub.

## Content care

- **275 broken links were fixed:**
  - pointed at a page's new address where one exists
  - otherwise at an archived copy on the Wayback Machine
  - when neither exists, the link was removed and its text kept
  - links to domains that had been taken over by spam sites were caught and fixed
- **240 links were upgraded** from `http://` to `https://` where the other site supports it.
- **14 broken or fragile images were fixed:**
  - some were restored from the Wayback Machine or the author's own files
  - some were switched to copies already on the site
  - two that couldn't be found anywhere were removed
- **The posts' wording was not changed.** Everything here touched links, images and markup only.

## Behind the scenes

- **Modern tools:**
  - Java 1.8 → 25
  - Gradle 5.6 → 9.8
  - JBake 2.6 → 2.7, now run directly, without an abandoned plugin
  - Markdown support with tables, footnotes and fenced code
- **The theme's styles are generated** from their source files on every build, instead of being hand-edited.
- **Every change is checked before it goes live:**
  - an automatic build
  - checks that no old page address disappeared and that no internal notes get published
  - valid feed and sitemap
  - no broken internal links
  - after each deploy, a quick test of the live site
- **Dependabot** proposes dependency updates every week.
- **Safer build tools:** the libraries the build uses with known security problems went from 23 to 4,
  and those 4 have no fix available. (They only run while building the site, never on the live site.)
- **One format for everything (October 16–18, 2026):** every post, page and draft is Markdown now. The 62
  posts imported from WordPress in 2013, the 11 posts written in AsciiDoc between 2014 and 2020, the About,
  Talks and Speaker Bio pages and the 12 drafts were converted mechanically, a batch at a time, with each
  published page checked for identical text, and screenshots compared, before and after. The AsciiDoc
  tools (a Ruby runtime inside Java, 19 libraries) then left the build, and the AsciiDoc stylesheet every
  page used to download is no longer loaded; the handful of its rules the site relied on moved into the
  theme's own styles, so the pages look the same.
- **A platform review** compared staying on JBake with Hugo, Astro, Eleventy and Roq, and decided to stay
  ([`01-PLATFORM-DECISION.md`](01-PLATFORM-DECISION.md)). It will be checked again every year.

## Writing is pleasant again

- **`./gradlew newPost -Ptitle="..."`** starts a new draft with everything filled in, on its own branch.
  It can also open a draft pull request.
- **A live preview:** saving a post rebuilds the site and refreshes the open browser tab in about one second.
- **Clear errors:** for example, if the preview's port is already in use, it says so.

## Still to come

These are for the author to write or decide, and they're tracked in their own list,
[`04-AUTHORS-DESK.md`](04-AUTHORS-DESK.md):
- an up-to-date About page and speaker bio
- what to do with the 12 drafts
- header images for older posts
- alt text in the author's own words
