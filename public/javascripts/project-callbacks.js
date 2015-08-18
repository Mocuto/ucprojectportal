function onLikeButtonClicked() {
	var active = $(this).attr("active")
	var projectId = $(this).attr("project-id")
	var likeCountSelector = $(this).parent().children(".like-count");
	var likeCount = (+likeCountSelector.text() || 0);
	var seeMoreMenuContainer = $(this).parent().children(".see-more-menu-container");
	var obj = this;

	if(active !== "true")
	{
		likeProject(projectId, function() {
			likeCountSelector.text(likeCount + 1)
			seeMoreMenuContainer.css({"display" : ""})
			var html = '<div class="see-more-menu-item" for="' + user.username + '"><a href="/' + user.username + '">' + user.firstName + '</a></div>'
			seeMoreMenuContainer.children(".see-more-menu").prepend(html);
		})
		$(this).attr("active", true)

	}
	else
	{
		unlikeProject(projectId, function() {
			if(likeCount == 1)
			{
				likeCountSelector.text("")
				seeMoreMenuContainer.css({"display" : "none"})
			}
			else
			{
				likeCountSelector.text(likeCount - 1);
			}

			var sel = ".see-more-menu-item[for='" + user.username + "']";
			var find = seeMoreMenuContainer.children(".see-more-menu").find(sel);
			find.remove();
			
			$(obj).css({"background-image" : "url('/assets/images/icons/heart-unselected.png')"});

			$(obj).bind("mouseleave", function() {
				$(obj).unbind("mouseleave");
				$(obj).css({"background-image" : ""});
			})
		})
		$(this).attr("active", false);
	}
}

function onFollowButtonClicked() {
	var active = $(this).attr("active")
	var projectId = $(this).attr("project-id")
	var obj = this;

	if(active !== "true")
	{
		followProject(projectId, function() {

		})
		$(this).attr("active", true)

	}
	else
	{
		unfollowProject(projectId, function() {
		
			
			$(obj).css({"background-image" : "url('/assets/images/icons/eye-unselected.png')"});

			$(obj).bind("mouseleave", function() {
				$(obj).unbind("mouseleave");
				$(obj).css({"background-image" : ""});
			})
		})
		$(this).attr("active", false);
	}
}

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
		$(this).removeClass("unselected");
		$("#project-all-files-button").addClass("unselected");
		$("#project-update-log").show().parent().css("width", "100%");
		$("#project-all-files").hide().parent().css("width", "0%");
	})

	$("#project-all-files-button").click(function() {
		$(this).removeClass("unselected")
		$("#project-update-log-button").addClass("unselected")
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
	})

	$("#update-files-mask").click(function() {
		$("#update-files").click();
	})

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
	})

	$(".like-button").on("click", onLikeButtonClicked)
	$(".follow-button").on("click", onFollowButtonClicked)
}