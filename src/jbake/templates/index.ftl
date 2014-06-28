<#include "header.ftl">
	
	<#include "menu.ftl">

	<div class="row-fluid">

		<div class="span2">
		</div>

		<div class="span8">
			<div class="page-header">
				<h1>Recent Posts</h1>
			</div>

			<#list posts as post>
		  		<#if (post.status == "published")>
		  			<a href="${post.uri}"><h1><#escape x as x?xml>${post.title}</#escape></h1></a>
		  			<p>${post.date?string("dd MMMM yyyy")}</p>
		  			<p>${post.summary}</p>

		  		</#if>
		  		<#if post_index = 3><#break></#if>
		  	</#list>
			
			<hr />
			
			<p>Older posts are available in the <a href="/${config.archive_file}">archive</a>.</p>
		</div>
	</div>

<#include "footer.ftl">