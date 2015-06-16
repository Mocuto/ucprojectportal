function onDeleteUpdateClicked() {
	var projectId = $(this).attr("project-id")
	var author = $(this).attr("author")
	var timeSubmitted = $(this).attr("time-submitted")

	dialogYesNo("Are you sure you want to delete this update?", "yes", "nevermind...", function() {
		deleteUpdate(projectId, author, timeSubmitted)
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
}