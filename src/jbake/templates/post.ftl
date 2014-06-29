<#include "header.ftl">
	
	<#include "menu.ftl">

    <div class="row-fluid">
    
        <div class="span6 offset3">
	
        	<div class="page-header">
        		<h1><#escape x as x?xml>${content.title}</#escape></h1>
        	</div>

        	<p><em>${content.date?string("dd MMMM yyyy")}</em></p>
            <p><em>Tags: <#list content.tags as tag></em>
                <a href="/tags/${tag?trim?replace(' ','-')}.html">${tag}</a> 
            </#list></p>
            <a href="https://twitter.com/share" class="twitter-share-button" data-url="http://www.mikemcgarr.com/blog/${content.uri}" data-via="jMichaelMcGarr" data-lang="fr">Tweeter</a>
            <script>!function(d,s,id){var js,fjs=d.getElementsByTagName(s)[0];if(!d.getElementById(id)){js=d.createElement(s);js.id=id;js.src="//platform.twitter.com/widgets.js";fjs.parentNode.insertBefore(js,fjs);}}(document,"script","twitter-wjs");</script>
            <div class="g-plusone" data-size="medium" data-href="http://www.mikemcgarr.com/blog/${content.uri}"></div>
            <script id='fbvpgng'>(function(i){var f,s=document.getElementById(i);f=document.createElement('iframe');f.src='//api.flattr.com/button/view/?uid=jMichaelMcGarr&button=compact&url='+encodeURIComponent('http://www.mikemcgarr.com/blog/${content.uri}');f.title='Flattr';f.height=20;f.width=110;f.style.borderWidth=0;s.parentNode.insertBefore(f,s);})('fbvpgng');</script>
            <script type="IN/Share" data-url="http://www.mikemcgarr.com/blog/${content.uri}" data-counter="right"></script>
            <div class="fb-like" data-href="http://www.mikemcgarr.com/blog/${content.uri}" data-layout="button_count" data-action="like" data-show-faces="false" data-share="true"></div>

        	<p>${content.body}</p>

            <hr>

            <div id="disqus_thread">
            <script type="text/javascript">
                /* * * CONFIGURATION VARIABLES: EDIT BEFORE PASTING INTO YOUR WEBPAGE * * */
                var disqus_shortname = 'mikemcgarr'; // required: replace example with your forum shortname

                /* * * DON'T EDIT BELOW THIS LINE * * */
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