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
		.children(".edit-field").css("display", "initial")

	$('.roundbox.update[project-id="' + projectId + '"][author="' + author + '"][time-submitted="' + timeSubmitted + '"]')
		.children(".content").css("display", "none")

	$(".textarea-update").autosize();

	$('.roundbox.update[project-id="' + projectId + '"][author="' + author + '"][time-submitted="' + timeSubmitted + '"]')
		.children(".edit-update-button").unbind("click")

	$('.roundbox.update[project-id="' + projectId + '"][author="' + author + '"][time-submitted="' + timeSubmitted + '"]')
		.children(".edit-update-button").bind("click", function() {
			var content = $('.roundbox.update[project-id="' + projectId + '"][author="' + author + '"][time-submitted="' + timeSubmitted + '"]')
				.children(".textarea-update").val();

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
}