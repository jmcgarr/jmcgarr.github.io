<#include "header.ftl">
	
	<#include "menu.ftl">

	<div class="row-fluid">

		<div class="span2">
		</div>

		<div class="span8">
			<div class="page-header">
				<h1>Mike McGarr's Blog</h1>
			</div>
		</div>
	</div>
	<div class="row-fluid" >

		<div class="span2">
		</div>

		<div class="span5">

			<#list posts as post>
		  		<#if (post.status == "published")>
		  			<a href="${post.uri}"><h3><#escape x as x?xml>${post.title}</#escape></h3></a>
		  			<p>${post.date?string("MMMM dd, yyyy")}</p>
		  			<p>${post.summary}</p>

		  		</#if>
		  		<#if post_index = 5><#break></#if>
		  	</#list>
			
			<hr />
			
			<p>Older posts are available in the <a href="/${config.archive_file}">archive</a>.</p>
		</div>

		<div class="span3">

			<img src="img/profile_pic-sq.jpg" class="img-rounded" style="width:300px;height:266px"/>
			<p>
			Build Tools Manager @Netflix. A software engineer with a passion for #lean, #tdd, #continuousDelivery, #devops, #automation and the #jvm.
			</p>
			<a href="http://www.mikemcgarr.com/feed.xml" target="_blank">
			   <img src="img/webicon-rss.svg" alt="RSS Feed" style="width:42px;height:42px">
			</a>
			<a href="http://twitter.com/SonOfGarr" target="_blank">
			   <img src="img/webicon-twitter.svg" alt="Twitter account" style="width:42px;height:42px">
			</a>
			<a href="http://github.com/jmcgarr" target="_blank">
			   <img src="img/webicon-github.svg" alt="Github account" style="width:42px;height:42px">
			</a>
			<a href="https://www.linkedin.com/in/jmichaelmcgarr" target="_blank">
			   <img src="img/webicon-linkedin.svg" alt="LinkedIn Profile" style="width:42px;height:42px">
			</a>
			<a href="https://plus.google.com/114285658743725894223/posts/p/pub" target="_blank">
			   <img src="img/webicon-googleplus.svg" alt="Google+ Profile" style="width:42px;height:42px">
			</a>
		</div>
	</div>

<#include "footer.ftl">