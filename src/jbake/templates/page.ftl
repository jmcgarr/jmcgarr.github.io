<#include "header.ftl">

	<#include "menu.ftl">

	<div class="row-fluid">
	
		<div class="span6 offset3">

			<div class="page-header">
				<h1><#escape x as x?xml>${content.title}</#escape></h1>
			</div>

			<p><em>${content.date?string("MMMM dd yyyy")}</em></p>

			<p>${content.body}</p>

		</div>
	</div>

	<hr>

<#include "footer.ftl">