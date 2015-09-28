function setupSearch() {

	var dataset = new Bloodhound({
		datumTokenizer: Bloodhound.tokenizers.whitespace,
		queryTokenizer: Bloodhound.tokenizers.whitespace,
		prefetch: {
			url: jsRoutes.controllers.ProjectController.jsonForAll().url,
			cache : true
		},
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

	var positionDataSet = new Bloodhound({
		datumTokenizer: Bloodhound.tokenizers.whitespace,
		queryTokenizer: Bloodhound.tokenizers.whitespace,
		prefetch: {
			url: jsRoutes.controllers.UserController.positionJson().url,
			cache : false
		},
		remote: {
			url: jsRoutes.controllers.UserController.positionJson().url
		}
	})

	$('.position-typeahead').typeahead({
	  minLength: 3,
	  highlight: true
	},
	{
	  name: 'positions',
	  source: positionDataSet
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

