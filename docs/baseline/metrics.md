# Baseline Metrics

**Branch**: `chore/M0-baseline-guardrails` | **Created**: 2026-10-04 | **Status**: Accepted

Measurements of the live site before the revival work, taken 2026-10-04. The live site is the
`live-2026-10` tag (`origin/master` at `dd49867`, published 2024-05-27). Each milestone
checkpoint compares against these numbers. Update this file (with a new dated section) only at
a checkpoint, per [`AGENTS.md`](../../AGENTS.md).

## Site size

| Metric | Value | How measured |
|---|---|---|
| Published size | **125.0 MB** in 1,824 files | `git ls-tree -r -l live-2026-10 \| awk '{s+=$4} END {print s/1048576}'` |
| Images (`img/`, incl. mastheads) | 109.4 MB | same, limited to `img` |
| Vendor libraries (`vendor/`) | 12.7 MB | same, limited to `vendor` |
| HTML pages | 272 | `wc -l docs/baseline/urls.txt` |
| `feed.xml` | 436,810 bytes (all posts, full bodies) | `git cat-file -s live-2026-10:feed.xml` |
| Local bake (macOS) | 130 MB in `build/jbake` | `du -sh build/jbake` after `./gradlew clean bake` |

## Lighthouse (mobile)

Lighthouse 12.8.2, default mobile emulation (simulated slow 4G), headless Chrome.
**Median of 3 runs** per page, against https://www.mikemcgarr.com.

| Page | Perf | A11y | Best Pr. | SEO | LCP | TBT | CLS | Page weight | Requests |
|---|---|---|---|---|---|---|---|---|---|
| `/` | 72 | 89 | 100 | 100 | 9.6 s | 10 ms | 0.000 | 1.3 MB | 23 |
| `/about.html` | 67 | 95 | 100 | 100 | 80.9 s | 0 ms | 0.000 | 15.1 MB | 25 |
| `/archive.html` | 65 | 89 | 100 | 100 | 74.4 s | 0 ms | 0.000 | 13.8 MB | 23 |
| `/blog/three-horizons-part1.html` | 56 | 92 | 57 | 100 | 31.4 s | 16 ms | 0.000 | 4.5 MB | 178 |

To reproduce one run:

```sh
npx -y lighthouse@12 <url> --chrome-flags="--headless=new" \
  --only-categories=performance,accessibility,best-practices,seo --output=json --output-path=<file>.json
```

## Content

| Metric | Value | How measured |
|---|---|---|
| `http://` links in content | **559** | `grep -roE 'http://[^"'"'"' )<>]+' src/jbake/content \| wc -l` |
| Content files with CR-only line endings | 63 | `grep -rlU $'\r' src/jbake/content \| wc -l` |
| Tags differing only by case | 2 (`devops`/`DevOps`, `groovy`/`Groovy`) | see T055 in [`00-REVIVAL.md`](../00-REVIVAL.md) |

## Observations

- **The hero images dominate.** On About and Archive, the 14–16 MB masthead PNGs push LCP to 75–80 s under mobile throttling. These are M2's T016 targets.
- **Third-party widgets dominate the post page.** Its 178 requests come mostly from the share buttons and Disqus, and they cause its Best Practices score of 57 (M1 T011–T013, M5 T045).
- **Lighthouse SEO is already 100.** It only checks basics (title, meta description presence, crawlability), so M5's "SEO ≥ 95" checkpoint is met before any work. Judge the M5 meta and social tasks by their own `Test:` lines (canonical, per-page descriptions, Open Graph), not by this score.
- **macOS bakes don't match CI bakes.** Case-only tag duplicates collapse on the case-insensitive filesystem (see T055).

## After M2 (2026-10-06)

Measured after PRs #29–#37 (live site). The M0 numbers above are kept for comparison. Lighthouse 12, mobile,
**median of 5 runs** (M0 used 3; scores swung ±10 between single runs before the fonts were self-hosted).

| Page | Perf (M0 → now) | FCP | LCP | Page weight (M0 → now) | A11y | Best Pr. | SEO |
|---|---|---|---|---|---|---|---|
| `/` | 72 → **96** | 1.2 s | 2.7 s | 1.3 → **0.39 MB** | 89 | 100 | 100 |
| `/about.html` | 67 → **93** | 1.1 s | 3.2 s | 15.1 → **0.46 MB** | 95 | 100 | 100 |
| `/archive.html` | 65 → **96** | 1.1 s | 2.7 s | 13.8 → **0.40 MB** | 89 | 100 | 100 |
| `/blog/three-horizons-part1.html` | 56 → **81** | 1.1 s | 5.1 s | 4.5 → **0.72 MB** | 96 | 100 | 100 |

| Metric | M0 | After M2 |
|---|---|---|
| Published size | 125.0 MB, 1,824 files | **21.8 MB, 433 files** |
| Post page requests | 178 (Disqus trackers) | ~26 |
| Third-party hosts on a post | 46 (live, Disqus) | 8, none of them trackers (Giscus, GoatCounter, CDN) |

The post page's remaining LCP gap is a 308 KB in-post diagram downloading alongside the header (Backlog T075).

## After M7 (2026-10-08)

Writing-loop timings on `main` at `be5cdc9` (after PRs #63–#65), Apple Silicon, JDK 21, Gradle 9.8.1, JBake 2.7.0.
Measured on a clean clone: `./gradlew newPost`, then `./gradlew clean bakePreview`, then 10 edits of the new draft.

| Metric | M0 (2026-10-04) | After M7 |
|---|---|---|
| Clean bake | about 6 s (JDK 1.8, Gradle 5.6.4, warm daemon) | about 3–4 s (2.9 s of it is the cold JBake bake) |
| Preview start, to the new draft being served | no watch mode | 4.4 s |
| Save → change served by the preview | no watch mode (re-run the bake by hand) | **0.24–1.24 s, median 0.78 s** (10 edits, none missed) |
| Save → page reloaded in the browser | manual refresh | 0.4–1.6 s (Chrome, 80 saves, T064); Firefox and Safari confirmed by the owner |
| Bakes in one Gradle daemon before "Java heap space" | 3 (T099) | no limit seen (10 in a row, then 50+ rebuilds in one preview) |

