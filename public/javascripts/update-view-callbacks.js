function onLikeUpdateButtonClicked() {
	var active = $(this).attr("active")
	var projectId = $(this).attr("project-id")
	var author = $(this).attr("author")
	var timeSubmitted = $(this).attr("time-submitted")

	var likeCountSelector = $(this).parent().find(".like-count");
	var likeCount = (+likeCountSelector.text() || 0);
	var seeMoreMenuContainer = $(this).parent().children(".see-more-menu-container");
	var obj = this;

	if(active !== "true")
	{
		likeUpdate(projectId, author, timeSubmitted, function() {
			likeCountSelector.text(likeCount + 1)
			seeMoreMenuContainer.css({"display" : ""})
			var html = '<div class="see-more-menu-item" for="' + user.username + '"><a href="/' + user.username + '">' + user.firstName + '</a></div>'
			seeMoreMenuContainer.children(".see-more-menu").prepend(html);
		})
		$(this).attr("active", true)

	}
	else
	{
		unlikeUpdate(projectId, author, timeSubmitted, function() {
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

function onDeleteUpdateClicked() {
	var projectId = $(this).attr("project-id")
	var author = $(this).attr("author")
	var timeSubmitted = $(this).attr("time-submitted")

	dialogYesNo("Are you sure you want to delete this update?", "yes", "nevermind...", function() {
		deleteUpdate(projectId, author, timeSubmitted)
	})
}

function onEditUpdateClicked() {
	var projectId = $(this).attr("project-id")
	var author = $(this).attr("author")
	var timeSubmitted = $(this).attr("time-submitted")

	$('.roundbox.update[project-id="' + projectId + '"][author="' + author + '"][time-submitted="' + timeSubmitted + '"]')
		.find(".edit-field").css("display", "initial")

	$('.roundbox.update[project-id="' + projectId + '"][author="' + author + '"][time-submitted="' + timeSubmitted + '"]')
		.find(".content").css("display", "none")

	$(".textarea-update").autosize();

	$('.roundbox.update[project-id="' + projectId + '"][author="' + author + '"][time-submitted="' + timeSubmitted + '"]')
		.find(".edit-update-button").unbind("click")

	$('.roundbox.update[project-id="' + projectId + '"][author="' + author + '"][time-submitted="' + timeSubmitted + '"]')
		.find(".edit-update-button").bind("click", function() {
			var content = $('.roundbox.update[project-id="' + projectId + '"][author="' + author + '"][time-submitted="' + timeSubmitted + '"]')
				.find(".textarea-update").val();

			editUpdate(projectId, author, timeSubmitted, content);
	})
}

function onUpdateArrowClicked(event) {

	event.stopPropagation();

	if ($(this).attr("active") != "true")
	{
			$(this).siblings(".update-menu").css("display", "block");
			$(this).attr("active", true)

			var obj = this;
			$("html").click(function() {
				$(obj).siblings(".update-menu").css("display", "none");
				$(obj).attr("active", false)
			})
	}
	else
	{
			$(this).siblings(".update-menu").css("display", "none");
			$(this).attr("active", false)
	}
}


function setupUpdateMenuCallbacks() {
	$(".update-arrow").on("click", onUpdateArrowClicked)
	$(".delete-update").on("click", onDeleteUpdateClicked)
	$(".edit-update").on("click", onEditUpdateClicked)
	$(".like-update-button").on("click", onLikeUpdateButtonClicked)
}