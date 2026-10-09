<#include "header.ftl">

	<#include "menu.ftl">
	<#assign pageTitle = "Tag: ${tagName(tag)}"><#-- the tag as written, with its spaces (tag-names.ftl, T057) -->

	<@externalLinksIn><#include "masthead.ftl"></@externalLinksIn><#-- the photo credit opens in a new tab (T058) -->

	<main id="main-content"><#-- skip-link target and main landmark (T046) -->
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
							<h2>${post.date?string("MMMM yyyy")}</h2>
							<ul>
						</#if>
					<#else>
						<li>
							<h2>${post.date?string("MMMM yyyy")}</h2>
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
	</main>

<#include "footer.ftl">
