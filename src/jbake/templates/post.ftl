<#include "header.ftl">

	<#include "menu.ftl">
	<#include "masthead.ftl">

		<div class="container">

        <div class="col-lg-8 col-md-10 mx-auto">

        	<p>${content.body}</p>

          <#-- Plain share links: no third-party scripts, nothing loads until a reader clicks -->
          <#assign shareUrl = "${config.site_host}/${content.uri}">
          <#assign shareTitle = content.title!"">
          <p class="share-links"><em>Share this:</em>
            <a href="https://www.linkedin.com/sharing/share-offsite/?url=${shareUrl?url('UTF-8')}" target="_blank" rel="noopener noreferrer">LinkedIn</a> &middot;
            <a href="https://x.com/intent/post?text=${shareTitle?url('UTF-8')}&amp;url=${shareUrl?url('UTF-8')}&amp;via=SonOfGarr" target="_blank" rel="noopener noreferrer">X</a> &middot;
            <a href="https://bsky.app/intent/compose?text=${(shareTitle + " " + shareUrl)?url('UTF-8')}" target="_blank" rel="noopener noreferrer">Bluesky</a> &middot;
            <a href="mailto:?subject=${shareTitle?url('UTF-8')}&amp;body=${shareUrl?url('UTF-8')}">Email</a>
          </p>

          <hr>

          <#-- Comments: Giscus, stored as GitHub Discussions in the "Comments" category. No ads or trackers.
               Threads are keyed by the page path, so a post's URL must never change (AGENTS.md, Rule 1). -->
          <div class="giscus"></div>
          <script src="https://giscus.app/client.js"
                  data-repo="jmcgarr/jmcgarr.github.io"
                  data-repo-id="MDEwOlJlcG9zaXRvcnkxODc0OTYyNg=="
                  data-category="Comments"
                  data-category-id="DIC_kwDOAR4Yus4DHHAz"
                  data-mapping="pathname"
                  data-strict="1"
                  data-reactions-enabled="1"
                  data-emit-metadata="0"
                  data-input-position="bottom"
                  data-theme="light"
                  data-lang="en"
                  data-loading="lazy"
                  crossorigin="anonymous"
                  async>
          </script>
          <noscript>Comments are powered by <a href="https://giscus.app">giscus</a> and need JavaScript.</noscript>
        </div>
    </div>

<#include "footer.ftl">
