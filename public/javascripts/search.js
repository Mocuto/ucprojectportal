$("#search-go").click(function() {
	var query = $("#searchbox").val();


	window.location.href = "/search/" + encodeURIComponent(query);
})