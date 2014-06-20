<#include "header.ftl">

	<#include "menu.ftl">

	<div class="row-fluid">
	
		<div class="span8 offset2">

			<div class="page-header">
				<h1><#escape x as x?xml>${content.title}</#escape></h1>
			</div>

			<p><em>${content.date?string("dd MMMM yyyy")}</em></p>

			<p>${content.body}</p>

		</div>
	</div>

	<hr>

<#include "footer.ftl">