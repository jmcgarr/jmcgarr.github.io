<#include "header.ftl">

	<#include "menu.ftl">
	<#assign pageTitle = "Tag: ${tag}">

	<#include "masthead.ftl">

	<div class="container">
    <div class="row justify-content-md-center">
			<div class="col-lg-8 col-md-10 mx-auto">

				<#-- One <li> per month, holding its heading and its list of posts (T104) -->
				<ul class="archive-months">
					<#list tag_posts as post>
					<#if (last_month)??>
						<#if post.date?string("MMMM yyyy") != last_month>
							</ul>
						</li>
						<li>
							<h4>${post.date?string("MMMM yyyy")}</h4>
							<ul>
						</#if>
					<#else>
						<li>
							<h4>${post.date?string("MMMM yyyy")}</h4>
							<ul>
					</#if>

					<li>${post.date?string("dd")} - <a href="/${post.uri}">${post.title}</a></li>
					<#assign last_month = post.date?string("MMMM yyyy")>
					</#list>
					<#if (last_month)??>
							</ul>
						</li>
					</#if>
				</ul>

			</div>
		</div>
	</div>

<#include "footer.ftl">
