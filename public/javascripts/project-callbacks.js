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
		else {
			$("#project-state-message-input").hide();
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

	$("#manage-projects-button").click(function() {
		var activated = $(this).attr("activated");
		if(activated == "true") {
			$(this).attr("activated", "false");
			$(this).text("manage projects");
			$(".leave-project-button").fadeOut();
		}
		else {
			$(this).attr("activated", "true");
			$(this).text("done managing")
			$(".leave-project-button").fadeIn();
		}
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
				rowHeight: 96,
				gutter : 32
			}
		})
	});

	$("#update-input").click(function() {
		$(".post-update-container").css("display", "block");
	});

	$("#update-files-mask").click(function() {
		$("#update-files").click();
	});

	$("#update-files").change(function() {
		$("#update-files-mask").animate({
			backgroundColor : "#aedefc"
		}, 3000);

		var files = $("#update-files").get(0).files
		var filenames = (function(files) {
			var list = [];
			for(var i = 0; i < files.length; i++) {
				list.push(files[i].name);
			}
			return list;
		})(files).join(", ");

		$("#update-files-filenames").text(filenames)
	});
}
