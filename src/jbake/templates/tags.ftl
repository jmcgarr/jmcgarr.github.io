<#include "header.ftl">

	<#include "menu.ftl">
	<#assign pageTitle = "Tag: ${tag}">

	<#include "masthead.ftl">

	<div class="container">
    <div class="row justify-content-md-center">
			<div class="col-lg-8 col-md-10 mx-auto">

				<ul>
					<#list tag_posts as post>
					<#if (last_month)??>
						<#if post.date?string("MMMM yyyy") != last_month>
							</ul>
							<h4>${post.date?string("MMMM yyyy")}</h4>
							<ul>
						</#if>
					<#else>
						<h4>${post.date?string("MMMM yyyy")}</h4>
						<ul>
					</#if>

					<li>${post.date?string("dd")} - <a href="/${post.uri}">${post.title}</a></li>
					<#assign last_month = post.date?string("MMMM yyyy")>
					</#list>
				</ul>

			</div>
		</div>
	</div>

<#include "footer.ftl">
