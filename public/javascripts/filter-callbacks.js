

function setupFilterCallbacks() {
	if(typeof activeFilters !== "undefined") {
		if(activeFilters.length > 0) {
			var filterStr = activeFilters.map(function(str) {
					return "." + str
				}).join("");

			$(".projectbox-container").isotope({
				filter : filterStr
			})
		}		
	}

	$(".filter-button").click(function() {
		var activated = $(this).attr("activated");
		var filterVal = $(this).attr("for");
		if(activated == "true") {
			//Remove filter

			$(this).attr("activated", "false");
			var index = activeFilters.indexOf(filterVal);

			$(this).animate({
				backgroundColor : "white"
			})

			if(index != -1) {
				activeFilters.splice(index, 1);
			}
		} 
		else {
			//Add filter

			$(this).attr("activated", "true");
			$(this).animate({
				backgroundColor : "#aedefc"
			})

			activeFilters.push(filterVal);
		}
		if(activeFilters.length == 0) {
			$(".projectbox-container").isotope({
				filter : '*'
			})
			var urlPath = "/filter"
			window.history.pushState({},"", urlPath);
		}
		else {
			var filterStr = activeFilters.map(function(str) {
				return "." + str
			}).join("");

			$(".projectbox-container").isotope({
				filter : filterStr
			})

			var urlPath = "/filter/" + activeFilters.join(",");
			window.history.pushState({},"", urlPath);
		}
	});
}