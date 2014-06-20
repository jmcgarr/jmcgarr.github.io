<#include "header.ftl">
	
	<#include "menu.ftl">

    <div class="row-fluid">
    
        <div class="span8 offset2">
	
        	<div class="page-header">
        		<h1><#escape x as x?xml>${content.title}</#escape></h1>
        	</div>

        	<p><em>${content.date?string("dd MMMM yyyy")}</em></p>

        	<p>${content.body}</p>
        </div>
    </div>

	<hr>

	<div id="disqus_thread"></div>
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
	
<#include "footer.ftl">