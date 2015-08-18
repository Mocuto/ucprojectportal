function onProfilePicClicked(event) {
	event.stopPropagation();

	if ($(this).attr("active") != "true")
	{
			$(this).children(".profile-menu-container").css("display", "block");
			$(this).attr("active", true)

			var obj = this;
			$("html").click(function() {
				$(obj).children(".profile-menu-container").css("display", "none");
				$(obj).attr("active", false)
			})
	}
	else
	{
			$(this).children(".profile-menu-container").css("display", "none");
			$(this).attr("active", false)
	}
}

function showNotificationsWindow(event) {
	resetUnreadNotifications();

	$("#popane-overlay").css("backgroundColor", "black");
	$("#notifications-window").popane({
		show : "true"
	})

	event.stopPropagation();
}

function setupNotificationCallbacks() {

	$(".user-profile-header").click(onProfilePicClicked)

	$("#notification-text").click(showNotificationsWindow)
	$(".unread-notification-marker").click(showNotificationsWindow)

	$("#request-join, .request-join").click(function() {
		var projectId = Number($(this).attr("project-id"));
		requestJoin(projectId);
	})

	$(document).on("click", ".accept-join", function() {
		$(this).parents(".notification-item").slideUp();

		var projectId = Number($(this).attr("project-id"));
		var requester = $(this).attr("requester");
		acceptRequest(projectId, requester);
	});

	$(document).on("click", ".ignore-join", function() {
		$(this).parents(".notification-item").slideUp();

		var projectId = Number($(this).attr("project-id"));
		var requester = $(this).attr("requester");
		ignoreRequest(projectId, requester);
	});

	$(document).on("click", ".ignore-notification", function() {
		$(this).parents(".notification-item").slideUp();
		var href = $(this).attr("href");
		var timeCreated = $(this).attr("time-created");

		ignoreNotification(timeCreated, href);

	})

	$(".notification-clear-all").click(function() {
		clearAllNotifications()
	})
}