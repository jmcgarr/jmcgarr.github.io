<#include "header.ftl">

	<#include "menu.ftl">

  <#assign pageTitle = "Blog Archive">
	<#assign pageSubtitle = "">
	<#assign masthead = "london-view.png">
	<#assign mastheadCredit = "Me, View from QCon London 2019 talk">
	<#include "masthead.ftl">

	<div class="container">
    <div class="row justify-content-md-center">
			<div class="col-lg-8 col-md-10 mx-auto">

				<ul>
					<#list published_posts as post>
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

					<li>${post.date?string("dd")} - <a href="${post.uri}"><#escape x as x?xml>${post.title}</#escape></a></li>
					<#assign last_month = post.date?string("MMMM yyyy")>
					</#list>
				</ul>

			</div>
		</div>
	</div>

<#include "footer.ftl">
