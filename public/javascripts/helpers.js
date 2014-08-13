var PULSE_INTERVAL = 3000;


function pulse() {
	var unreadNotifications = getUnreadNotificationCount(function(data) {

		var count = Number(data["count"]);

		var notificationText = DEFAULT_MESSAGE;
		if(count > 0) {
			notificationText = user.firstName + ", you have " + count + " new notification" + (count == 1 ? "" : "s") +"!";
		}
		var notificationsHtml = data["html"];
		
		$("#notification-text").text(notificationText);

		if(count != user.unreadNotifications) {
			$("#notifications-window").find(".notifications-list").remove();
			$("#notifications-window").append(notificationsHtml)
		}
		

	}, function(xmlhttprequest, textstatus, message) {
		if(textstatus === "timeout") {
			//TODO: Change the site in some way to indicate that the user is offline
		}
		else {
			//TODO: Handle case by case
		}
	});

	window.setTimeout(pulse, PULSE_INTERVAL);

}

$(document).ready(function() {

	setupLayout();

	setupNotificationCallbacks();

	setupProjectCallbacks();

	setupFilterCallbacks();

	setupEditCallbacks();

	pulse();
})