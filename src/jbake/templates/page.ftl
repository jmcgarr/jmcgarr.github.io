<#include "header.ftl">

	<#include "menu.ftl">
	<#-- Off-site links open in a new tab (T058, external-links.ftl): the photo credit and the body -->
	<@externalLinksIn><#include "masthead.ftl"></@externalLinksIn>

	<!-- Main Content -->
	<main id="main-content"><#-- skip-link target and main landmark (T046) -->
	<div class="container">
		<div class="row">
			<div class="col-lg-8 col-md-10 mx-auto">
				<#-- A lone link to a video or deck becomes the player, first (T115, embeds.ftl), as in post.ftl: the Talks page has 18 -->
				<div>${externalLinks(embeds(content.body))}</div>
			</div>
		</div>
	</div>
	</main>

	<hr>

<#include "footer.ftl">
