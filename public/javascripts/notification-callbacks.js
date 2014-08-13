function setupNotificationCallbacks() {

	$("#notification-text").click(function() {
		resetUnreadNotifications();

		$("#popane-overlay").css("backgroundColor", "black");
		$("#notifications-window").popane({
			show : "true"
		})
	})

	$("#request-join").click(function() {
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
}