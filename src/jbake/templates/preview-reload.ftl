<#-- Live reload for the local preview only (T064). header.ftl includes this file only when the `preview` Gradle
     task sets the system property preview.livereload=true, so a normal bake never contains it. CI fails the build
     if the word "livereload" (the marker below) reaches the deploy contents.

     How it works: JBake's preview server can't push, so the page asks. Every 0.5 s it sends a HEAD request for
     itself and compares Last-Modified and Content-Length with the previous answer. A rebuild rewrites every page,
     so any content or template change shows up there. A CSS or JS change only copies that file, so the page's
     same-origin stylesheets and scripts are checked too. On a change it re-downloads any changed CSS/JS (so the
     browser cache can't serve the old copy), then reloads. It pauses while the tab is hidden, and if the server
     stops it backs off (1, 2, 4, 8, then 10 s) and gives up after 10 failed tries; showing the tab again retries.
     It only reloads when something changes, so a broken template can't cause a reload loop. It sits early in
     <head>, so a page cut short by a template error still has it and reloads again once the error is fixed. -->
<!-- livereload: local preview only (T064), never published -->
<script data-livereload>
(function () {
  "use strict";
  var INTERVAL = 500, MAX_DELAY = 10000, MAX_FAILURES = 10;
  var page = location.pathname + location.search;
  var assets = null, seen = {}, failures = 0, timer = null, busy = false, stopped = false;

  function sameOriginAssets() {
    var urls = [], els = document.querySelectorAll('link[rel~="stylesheet"][href], script[src]');
    for (var i = 0; i < els.length; i++) {
      var url = new URL(els[i].href || els[i].src, location.href);
      if (url.origin === location.origin && urls.indexOf(url.pathname + url.search) < 0) {
        urls.push(url.pathname + url.search);
      }
    }
    return urls;
  }

  function stamp(url) {
    return fetch(url, { method: "HEAD", cache: "no-store" }).then(function (response) {
      if (!response.ok) { throw new Error("HTTP " + response.status); }
      return response.headers.get("Last-Modified") + "|" + response.headers.get("Content-Length");
    });
  }

  function schedule(delay) {
    if (!stopped && timer === null) { timer = setTimeout(check, delay); }
  }

  function check() {
    timer = null;
    if (stopped || busy || document.hidden) { return; }
    busy = true;
    if (assets === null) { assets = sameOriginAssets(); }
    // The page first, so a stopped server costs one failed request per try, not one per file.
    stamp(page).then(function (pageStamp) {
      return Promise.all(assets.map(stamp)).then(function (assetStamps) {
        return [pageStamp].concat(assetStamps);
      });
    }).then(function (stamps) {
      busy = false;
      failures = 0;
      var urls = [page].concat(assets), changed = [];
      for (var i = 0; i < urls.length; i++) {
        if (urls[i] in seen && seen[urls[i]] !== stamps[i]) { changed.push(urls[i]); }
        seen[urls[i]] = stamps[i];
      }
      if (changed.length === 0) { schedule(INTERVAL); return; }
      stopped = true;
      var refetch = changed.filter(function (url) { return url !== page; }).map(function (url) {
        return fetch(url, { cache: "reload" }).catch(function () {});
      });
      Promise.all(refetch).then(function () { location.reload(); });
    }, function () {
      busy = false;
      failures++;
      if (failures >= MAX_FAILURES) {
        stopped = true;
        console.info("Preview: the server isn't responding, so the page stopped checking for changes. " +
                     "Reload the page (or switch back to this tab) to start again.");
        return;
      }
      schedule(Math.min(INTERVAL * Math.pow(2, failures), MAX_DELAY));
    });
  }

  document.addEventListener("visibilitychange", function () {
    if (document.hidden) { return; }
    if (stopped && failures >= MAX_FAILURES) { stopped = false; failures = 0; }
    schedule(0);
  });
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", check);
  } else {
    check();
  }
})();
</script>
