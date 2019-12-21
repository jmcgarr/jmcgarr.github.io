<#include "header.ftl">

	<#include "menu.ftl">

<!-- Not title -->
	<#assign pageTitle = "">
	<#assign pageSubtitle = "">

	<#include "masthead.ftl">

	<div class="container">
    <div class="row justify-content-md-center">
			<div class="col-lg-8 col-md-10 mx-auto">

		  		<#list posts as post>
			  		<#if (post.status == "published")>
						<div class="post-preview">
			  			<a href="${post.uri}"><h3 class="post-title"><#escape x as x?xml>${post.title}</#escape></h3></a>
			  			<p class="post-meta">${post.date?string("MMMM dd, yyyy")}</p>
			  			<p class="post-subtitle">${post.summary}</p>
						</div>
						<hr>
			  		</#if>
			  		<#if post_index = 5><#break></#if>
			  	</#list>

					<p>Older posts are available in the <a href="/${config.archive_file}">archive</a>.</p>
			</div>
    </div>
	</div>

<#include "footer.ftl">
