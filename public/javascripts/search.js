function setupSearch() {

	var dataset = new Bloodhound({
		datumTokenizer: Bloodhound.tokenizers.whitespace,
		queryTokenizer: Bloodhound.tokenizers.whitespace,
		prefetch: jsRoutes.controllers.ProjectController.jsonForUser().url,
		remote: {
			url: jsRoutes.controllers.ProjectController.jsonForAll().url
		}
	})

	$('.typeahead').typeahead({
	  minLength: 3,
	  highlight: true
	},
	{
	  name: 'projects',
	  source: dataset
	});

	$("#search-box").bind("enter", function() {
		redirectToSearch();
	})

	$('#search-box').keyup(function(e){
		if(e.keyCode == 13)
		{
		    $(this).trigger("enter");
		}
	});
}


function redirectToSearch() {
	var query = $("#search-box").val();

	redirectToSearchWithQuery(query)
}

function redirectToSearchWithQuery(query) {
	window.location.href = jsRoutes.controllers.Application.search(query).url
}

$("#search-go").click(redirectToSearch);

