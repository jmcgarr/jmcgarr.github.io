#!/bin/sh
# Full-page-ish headless Chrome screenshots of posts served at http://localhost:8202, at 1280px (desktop).
# Usage: [PORT=8203] screenshots.sh <out_dir> <slug>...
#   A slug is a post's base name ("java-live" shoots /blog/java-live.html), or a path ending in .html
#   relative to the site root ("about.html", "blog/drafts/<name>-draft.html") for a page or a draft; the
#   file is named after it with "/" as "-" and without ".html" ("blog-drafts-<name>-draft-1280.png").
#   Serve the build first, for example: jwebserver -p 8202 -d /abs/path/to/build/site
#   (Never use port 8080: it's the owner's preview.) PORT in the environment picks another port, for a
#   second run alongside the first.
# For the phone width use phone-shots.mjs: on macOS, headless Chrome won't open a window narrower than
# about 500px, so a --window-size=375 shot is a 375px crop of a wider layout, not what a phone shows.
# (The T102 batches before 2026-10-17 used such crops for their 375px pairs; the before/after comparisons
# were still valid, since both sides had the same crop, but they didn't show the real phone layout.)
# Each run is wrapped in gtimeout 45. A throwaway Chrome profile lives in <out_dir>/.chrome.
# Compare two runs with PixelDiff.java: java scripts/visual/PixelDiff.java <before_dir> <after_dir> [<diff_out_dir>].
OUT="$1"; shift
PORT="${PORT:-8202}"
CHROME="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
mkdir -p "$OUT/.chrome"
for slug in "$@"; do
  case "$slug" in
    *.html) path="$slug"; name="$(printf '%s' "${slug%.html}" | tr / -)" ;;
    *) path="blog/$slug.html"; name="$slug" ;;
  esac
  for spec in 1280x9000; do
    w=${spec%x*}; h=${spec#*x}
    gtimeout 45 "$CHROME" --headless=new --disable-gpu --hide-scrollbars --mute-audio \
      --no-first-run --no-default-browser-check --user-data-dir="$OUT/.chrome" \
      --force-device-scale-factor=1 --window-size="$w,$h" --virtual-time-budget=8000 \
      --screenshot="$OUT/$name-$w.png" "http://localhost:$PORT/$path" >/dev/null 2>&1
    echo "$slug $w: $?"
  done
done
