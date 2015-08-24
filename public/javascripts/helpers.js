var PULSE_INTERVAL = 5000;


String.prototype.replaceAll = function(what, to) {
	var find = what;
	var re = new RegExp(find, 'g');

	return this.replace(re, to);
}

String.prototype.brTagify = function() {
	return this.replaceAll("\r\n", "<br>").replaceAll("\n", "<br>")
}

function tickDates() {
	$(".time-ago").each(function() {
		var time = +$(this).attr("time-submitted")
		$(this).text(moment(time).fromNow())
	})
}

function pulse() {
	var unreadNotifications = getUnreadNotificationCount(function(data) {

		var count = Number(data["count"]);

		var notificationText = DEFAULT_MESSAGE;
		if(count > 0) {
			notificationText = "notifications (" + count + ")";
			$(".unread-notification-marker").css("display", "initial").text(count)
		}
		else {
			$(".unread-notification-marker").css("display", "none").text("")

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

	tickDates();

	window.setTimeout(pulse, PULSE_INTERVAL);

}

$(document).ready(function() {

	setupLayout();

	setupNotificationCallbacks();

	setupProjectCallbacks();

	setupFilterCallbacks();

	setupEditCallbacks();

	setupFormCallbacks();

	setupUpdateMenuCallbacks();

	setupSearch();

	setupModerator();

	setupUserProfileCallbacks();

	setupOfficeHoursCallbacks();

	setupHeroCardCallbacks();

	pulse();
})