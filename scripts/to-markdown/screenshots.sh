#!/bin/sh
# Full-page-ish headless Chrome screenshots of posts served at http://localhost:8202, at 1280px and 375px.
# Usage: [PORT=8203] screenshots.sh <out_dir> <slug>...
#   Serve the build first, for example: jwebserver -p 8202 -d /abs/path/to/build/site
#   (Never use port 8080: it's the owner's preview.) PORT in the environment picks another port, for a
#   second run alongside the first.
# Each run is wrapped in gtimeout 45. A throwaway Chrome profile lives in <out_dir>/.chrome.
OUT="$1"; shift
PORT="${PORT:-8202}"
CHROME="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
mkdir -p "$OUT/.chrome"
for slug in "$@"; do
  for spec in 1280x9000 375x20000; do
    w=${spec%x*}; h=${spec#*x}
    gtimeout 45 "$CHROME" --headless=new --disable-gpu --hide-scrollbars --mute-audio \
      --no-first-run --no-default-browser-check --user-data-dir="$OUT/.chrome" \
      --force-device-scale-factor=1 --window-size="$w,$h" --virtual-time-budget=8000 \
      --screenshot="$OUT/$slug-$w.png" "http://localhost:$PORT/blog/$slug.html" >/dev/null 2>&1
    echo "$slug $w: $?"
  done
done
