<#include "header.ftl">

	<#include "menu.ftl">

<!-- Not title -->
	<#assign pageTitle = "">
	<#assign pageSubtitle = "">

	<@externalLinksIn><#include "masthead.ftl"></@externalLinksIn><#-- the photo credit opens in a new tab (T058) -->

	<div class="container">
    <div class="row justify-content-md-center">
			<div class="col-lg-8 col-md-10 mx-auto">

		  		<#-- The 6 newest published posts. [0..*6] stops early if there are fewer. -->
		  		<#list published_posts[0..*6] as post>
						<div class="post-preview">
			  			<a href="${post.uri}"><h3 class="post-title"><#escape x as x?xml>${post.title}</#escape></h3></a>
			  			<p class="post-meta">${post.date?string("MMMM dd, yyyy")}</p>
			  			<#if post.summary?has_content><p class="post-subtitle">${post.summary}</p></#if>
						</div>
						<hr>
			  	</#list>

					<p>Older posts are available in the <a href="/${config.archive_file}">archive</a>.</p>
			</div>
    </div>
	</div>

<#include "footer.ftl">
