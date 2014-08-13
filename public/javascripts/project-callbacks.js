function setupProjectCallbacks() {

	function toggleProjectBox(event) {
		var expanded = $(this).parents(".projectbox").attr("expanded");

		if(expanded == "false") {
			$(this).parents(".projectbox").find(".projectbox-description").slideDown({
				duration : 150,
				done : function(num) {
					var packeryParent = $(event.target).parents(".projectbox-container");
					packeryParent.isotope('layout')
				}
			});
		}
		else {
			$(this).parents(".projectbox").find(".projectbox-description").slideUp({
				duration : 150,
				step : function(num) {
					var packeryParent = $(event.target).parents(".projectbox-container");
					packeryParent.isotope('layout')
			
				}
			});
		}
		$(this).parents(".projectbox").attr("expanded", (expanded == "true") ? "false" : "true");
	}

	$(".projectbox img, .projectbox .projectbox-title").click(toggleProjectBox);

	$(document).on("click", ".close-button",  function() {
		$(this).parents(".popane").popane({
			"show" : "false"
		})
	})

	$("#project-state-select").change(function() {
		if($(this).val() === STATE_IN_PROGRESS_NEEDS_HELP) {
			$("#project-state-message-input").show();
		}
	})

	$("#project-update-log-button").click(function() {
		$(this).css("color", "white");
		$("#project-all-files-button").css("color", "#ff9c9c");
		$("#project-update-log").show().parent().css("width", "100%");
		$("#project-all-files").hide().parent().css("width", "0%");
	})

	$("#project-all-files-button").click(function() {
		$(this).css("color", "white");
		$("#project-update-log-button").css("color", "#ff9c9c");
		$("#project-update-log").hide().parent().css("width", "0%");
		$("#project-all-files").show().parent().css("width", "100%");
	})

	$(".leave-project-button").click(function() {
		var projectId = $(this).attr("for");
		leaveProject(projectId);
	})

	$(".projectbox-container").imagesLoaded( function(){ 
		$(".projectbox-container").isotope({
			layoutMode : 'packery',
			itemSelector : '.isotope-item',
			packery: {
				columnWidth : ".grid-sizer",
				gutter : 32
			}
		})
	});

	$("#update-input").click(function() {
		$(".post-update-container").css("display", "block");
	})

}