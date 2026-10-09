<#include "header.ftl">

	<#include "menu.ftl">

  <#assign pageTitle = "Blog Archive">
	<#assign pageSubtitle = "">
	<#assign masthead = "london-view.jpg">
	<#assign mastheadCredit = "Me, View from QCon London 2019 talk">
	<@externalLinksIn><#include "masthead.ftl"></@externalLinksIn><#-- the photo credit opens in a new tab (T058) -->

	<main id="main-content"><#-- skip-link target and main landmark (T046) -->
	<div class="container">
    <div class="row justify-content-md-center">
			<div class="col-lg-8 col-md-10 mx-auto">

				<#-- One <li> per month, holding its heading and its list of posts (T104) -->
				<ul class="archive-months">
					<#list published_posts as post>
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

					<li>${post.date?string("dd")} - <a href="${post.uri}"><#escape x as x?xml>${post.title}</#escape></a></li>
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
