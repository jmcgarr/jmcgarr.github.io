<#include "header.ftl">

	<#include "menu.ftl">

	<#--
	  Topics (T057, issue #13): every tag with its number of posts, published at /tags/ (tags/index.html).
	  JBake renders this template because jbake.properties has render.tagsindex=true. Its `tags` lists every tag of a
	  published post or page: `name` as JBake gives it (hyphens for spaces, T039), `uri` (tags/<name>.html, the real
	  tag page, never a redirect stub), and `tagged_posts`, the same published posts the tag page lists. So each count
	  matches its tag page. A tag named "index" would share this page's file name; don't use one.
	-->
	<#assign pageTitle = "Topics">
	<#assign pageSubtitle = "">
	<@externalLinksIn><#include "masthead.ftl"></@externalLinksIn><#-- the photo credit opens in a new tab (T058) -->

	<#-- Sorted A–Z ignoring case and leading punctuation (".NET" goes under N), word by word: FreeMarker's sort
	     ignores spaces ("agile richmond" would follow "agiledox"), so in the key a space becomes "0", which sorts
	     before every letter ("agile", "agile richmond", "agiledc"). -->
	<#assign topics = []>
	<#list tags as t>
		<#assign topicName = tagName(t.name)>
		<#assign topics = topics + [{"name": topicName, "uri": t.uri, "count": t.tagged_posts?size,
			"key": topicName?lower_case?replace(r"^[^a-z0-9]+", "", "r")?replace(" ", "0")}]>
	</#list>
	<#assign topics = topics?sort_by("key")>
	<#-- Most posts first, ties A–Z: the 12 biggest topics, plus any tied with the 12th, never one with a single post -->
	<#assign byCount = topics?reverse?sort_by("count")?reverse>
	<#assign mostUsedMin = [byCount[[11, byCount?size - 1]?min].count, 2]?max>

	<#macro topicItem topic>
						<li><a href="/${topic.uri?url_path('UTF-8')}">${topic.name?html}</a> <span class="topics-count">(${topic.count}<span class="sr-only"> ${(topic.count == 1)?then("post", "posts")}</span>)</span></li>
	</#macro>

	<main id="main-content"><#-- skip-link target and main landmark (T046) -->
	<div class="container">
		<div class="row justify-content-md-center">
			<div class="col-lg-8 col-md-10 mx-auto">

				<#if byCount?has_content && byCount[0].count gte mostUsedMin>
				<h2 class="topics-heading">Most used</h2>
				<ul class="topics-list">
					<#list byCount as topic>
					<#if topic.count lt mostUsedMin><#break></#if>
					<@topicItem topic/>
					</#list>
				</ul>
				</#if>

				<h2 class="topics-heading">All ${topics?size} topics, A–Z</h2>
				<ul class="topics-list">
					<#list topics as topic>
					<@topicItem topic/>
					</#list>
				</ul>

			</div>
		</div>
	</div>
	</main>

<#include "footer.ftl">
