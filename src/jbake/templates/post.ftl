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

          <div id="disqus_thread">
          <script type="text/javascript">
                var disqus_shortname = 'mikemcgarr';
                (function() {
                    var dsq = document.createElement('script'); dsq.type = 'text/javascript'; dsq.async = true;
                    dsq.src = '//' + disqus_shortname + '.disqus.com/embed.js';
                    (document.getElementsByTagName('head')[0] || document.getElementsByTagName('body')[0]).appendChild(dsq);
                })();
          </script>
          <noscript>Please enable JavaScript to view the <a href="http://disqus.com/?ref_noscript">comments powered by Disqus.</a></noscript>
          <a href="http://disqus.com" class="dsq-brlink">comments powered by <span class="logo-disqus">Disqus</span></a>
          </div>
        </div>
    </div>

<#include "footer.ftl">
