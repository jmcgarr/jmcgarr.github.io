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
