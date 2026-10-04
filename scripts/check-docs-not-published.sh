#!/usr/bin/env bash
#
# Fails if anything from docs/ (planning and internal docs) has leaked into the
# baked site. See docs/00-REVIVAL.md, task T004.
#
# Usage: scripts/check-docs-not-published.sh [site-dir]   (default: build/jbake)

set -euo pipefail

site_dir="${1:-build/jbake}"

if [[ ! -d "$site_dir" ]]; then
  echo "ERROR: site directory '$site_dir' not found. Run ./gradlew bake first." >&2
  exit 2
fi

failed=0

if [[ -e "$site_dir/docs" ]]; then
  echo "FAIL: '$site_dir/docs' exists."
  failed=1
fi

# vendor/ is skipped: third-party packages ship their own README.md files
# (e.g. vendor/fontawesome-free/README.md), which are not ours.
md_files="$(find "$site_dir" -path "$site_dir/vendor" -prune -o -type f -name '*.md' -print)"
if [[ -n "$md_files" ]]; then
  echo "FAIL: Markdown files found in the site:"
  echo "$md_files" | sed 's/^/  /'
  failed=1
fi

# Marker string from docs/00-REVIVAL.md. -I skips binary files (images, fonts).
marker_files="$(grep -rIl 'REVIVAL' "$site_dir" || true)"
if [[ -n "$marker_files" ]]; then
  echo "FAIL: planning-doc marker 'REVIVAL' found in:"
  echo "$marker_files" | sed 's/^/  /'
  failed=1
fi

if [[ "$failed" -ne 0 ]]; then
  echo "docs/ content must never be published. See AGENTS.md (Rule 4)." >&2
  exit 1
fi

echo "OK: no docs/ content in $site_dir"
