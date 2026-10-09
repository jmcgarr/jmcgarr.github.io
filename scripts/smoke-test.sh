#!/usr/bin/env bash
#
# Post-deploy smoke test (T092). Fetches a handful of known URLs from the LIVE site and fails if any
# doesn't return the expected status. The CI gates check build/site before upload, so anything lost
# between upload and the live site (like the hidden-file drop fixed in #47) only shows up here.
#
# Each request carries a cache-busting query string, so the CDN can't answer with a stale copy.
# A URL that doesn't match yet is retried for a while, to allow for CDN propagation after a deploy.
#
# Usage: scripts/smoke-test.sh [base-url] ['<path> <status>' ...]
#   base-url defaults to https://www.mikemcgarr.com/
#   extra '<path> <status>' pairs are checked too (e.g. 'blog/no-such-post.html 200' as a negative test)
#   SMOKE_ATTEMPTS (default 10) and SMOKE_DELAY seconds (default 15) control the retries.
# Live site only: a local preview serves drafts, so the draft check would fail there.

set -uo pipefail

base="${1:-https://www.mikemcgarr.com/}"
base="${base%/}/"
shift $(( $# > 0 ? 1 : 0 ))
attempts="${SMOKE_ATTEMPTS:-10}"
delay="${SMOKE_DELAY:-15}"
bust="smoke=$(date +%s)"

checks=(
  "index.html 200"                                     # home page
  "about.html 200"
  "archive.html 200"
  "blog/roadmaps.html 200"                             # a post (AsciiDoc)
  "blog/a-roadmap-unit-testing.html 200"               # a legacy WordPress post
  "tags/.NET.html 200"                                 # hidden-file name: the one #47 lost
  "tags/acceptance%20test.html 200"                    # an old tag URL with a space: now a redirect stub (T039)
  "tags/acceptance-test.html 200"                      # ...and the tag page it points to
  "tags/DevOps.html 200"                               # old capitalized tag URL: a stub added only on Linux (T055)
  "tags/ 200"                                          # the Topics page (T057): its URL is the folder...
  "tags/index.html 200"                                # ...served from this file
  "feed.xml 200"
  "sitemap.xml 200"
  "css/clean-blog.css 200"
  "img/masthead/hawaii.jpg 200"                        # a header image, full size
  "img/masthead/hawaii-960.jpg 200"                    # ...and its phone variant (T071)
  "fonts/open-sans/open-sans-normal-latin.woff2 200"   # self-hosted font (T073)
  "blog/drafts/acceptance-test-driven-development-draft.html 404"   # drafts never go live
  "docs/00-REVIVAL.md 404"                             # docs/ never goes live
  "$@"
)

status_of() { curl -s -o /dev/null -w '%{http_code}' --max-time 30 "$base$1?$bust"; }

pending=("${checks[@]}")
for (( try = 1; try <= attempts; try++ )); do
  still=()
  for check in "${pending[@]}"; do
    path="${check% *}" want="${check##* }"
    got="$(status_of "$path")"
    if [[ "$got" == "$want" ]]; then
      echo "ok    $got  /$path"
    else
      still+=("$check")
      [[ $try -eq $attempts ]] && echo "FAIL  $got  /$path  (expected $want)"
    fi
  done
  pending=("${still[@]+"${still[@]}"}")
  (( ${#pending[@]} == 0 )) && break
  if (( try < attempts )); then
    echo "... ${#pending[@]} not as expected yet (attempt $try/$attempts), retrying in ${delay}s"
    sleep "$delay"
  fi
done

if (( ${#pending[@]} > 0 )); then
  echo "FAIL: ${#pending[@]} of ${#checks[@]} live URLs not as expected on $base" >&2
  exit 1
fi
echo "OK: all ${#checks[@]} live URLs as expected on $base"
