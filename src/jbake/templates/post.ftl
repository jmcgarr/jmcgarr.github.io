<#include "header.ftl">

	<#include "menu.ftl">

    <div class="row-fluid">

				<div class="span6 offset3">

					<div class="page-header">
						<h1><#escape x as x?xml>${content.title}</#escape></h1>
					</div>

					<p><em>${content.date?string("MMMM dd, yyyy")}</em></p>

				</div>
		</div>


		<div class="row-fluid">

				<div class="span2 offset1">
					<p/>
					<p class="text-right"><em><#list content.tags as tag></em>
								<a href="/tags/${tag?trim?replace(' ','-')}.html">${tag}</a><br/>
								</#list>
					</p>
				</div>

        <div class="span6">

        	<p>${content.body}</p>

					<p><em>Share this:</em></p>
          <p style="float:left">
                <a href="https://twitter.com/share" class="twitter-share-button" data-url="http://www.mikemcgarr.com/${content.uri}" data-via="SonOfGarr" data-lang="fr">Tweeter</a>
                <script>!function(d,s,id){var js,fjs=d.getElementsByTagName(s)[0];if(!d.getElementById(id)){js=d.createElement(s);js.id=id;js.src="//platform.twitter.com/widgets.js";fjs.parentNode.insertBefore(js,fjs);}}(document,"script","twitter-wjs");</script>
                <div class="g-plusone" data-size="medium" data-href="http://www.mikemcgarr.com/${content.uri}"></div>
                <script type="IN/Share" data-url="http://www.mikemcgarr.com/${content.uri}" data-counter="right"></script>
                <div class="fb-like" data-href="http://www.mikemcgarr.com/${content.uri}" data-layout="button_count" data-action="like" data-show-faces="false" data-share="true"></div>
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
