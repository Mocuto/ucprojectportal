
function onProjectSubmitSuccess(data) {
	alert("Success");
}

function onProjectSubmitError(xmlhttprequest, textstatus, message) {
	console.error(xmlhttprequest.responseText);
	console.error(textstatus);
	console.error(message);
}

function ajaxSendFormData(formData, route, onSuccess, onError) {
$.ajax({
	type: route.type,
	url: route.url,
	data:formData,
	processData: false,
	contentType: false,
	dataType: "json",
	success: function (data)
	{
		onSuccess.call(this, data);
	},
	error: function(error, textstatus, message){
		//alert("ERROR" + error.responseText);
		onError.call(this, error, textstatus, message);
	}
});				
}

function editProject(formData, optionalCallback) {
	if (typeof optionalCallback === "undefined") {
		optionalCallback = function() {

		}
	}

	var route = jsRoutes.controllers.ProjectController.editProject(PROJECT_ID);
	ajaxSendFormData(formData, route, optionalCallback, function() {

	})
}

function leaveProject(projectId) {
	var route = jsRoutes.controllers.ProjectController.leaveProject(projectId);
	ajaxSendFormData(new FormData(), route, function() {
		for(var i = 0; i < $(".projectbox-" + projectId).length; i++) {
			var projectbox = $(".projectbox-" + projectId)[i]
			var container = $(projectbox).parents(".projectbox-container").data('isotope');
			container.remove($(projectbox).parents(".isotope-item"))
			container.layout()
		}
	}, function() {

	})
}

function submitUpdate() {
	var content = $("#update-input").val();
	var files = $("#update-files").get(0).files;
	var formData = new FormData();
	formData.append("content", content);
	formData.append("project_id", PROJECT_ID)
	for(var i = 0; i < files.length; i++) {
		formData.append("file" + i, files[i]);
	}
	var ajax = {
		sucess: onProjectSubmitSuccess,
		error: onProjectSubmitError
	}
	var route = jsRoutes.controllers.ProjectController.submitUpdate();
	ajaxSendFormData(formData, route, function(data) {

		var updateHtml = $($.parseHTML(data["html"])[1]);
		updateHtml.hide();

		$("#update-input").val("");

		$(".update-group").prepend(updateHtml).slideDown();
		updateHtml.slideDown();

		var fileInputs = $(".file-inputs")
		$(".file-inputs").parents("form").trigger("reset");
	});
}

function requestJoin(projectId) {
	var route = jsRoutes.controllers.RequestController.requestJoin(projectId);

	ajaxSendFormData(new FormData(), route, function() {

		dialog("<span style='font-weight:500'>thank you!</span> your request to join has been sent to the project's author.");

		//alert("Request sent");
	}, function(xmlhttprequest, textstatus, message) {
		//TODO: Handle this error
	});
}

function acceptRequest(projectId, requester) {
	var route = jsRoutes.controllers.RequestController.acceptRequest(projectId, requester);

	ajaxSendFormData(new FormData(), route, function() {

	}, function() {

	});
}

function ignoreRequest(projectId, requester) {
	var route = jsRoutes.controllers.RequestController.ignoreRequest(projectId, requester);

	ajaxSendFormData(new FormData(), route, function() {
		//Success
	}, function() {
		//Error
	});
}

function resetUnreadNotifications() {

	var route = jsRoutes.controllers.NotificationController.resetUnreadNotifications();
	ajaxSendFormData(new FormData(), route, function() {
		$("#notification-text").text(user.defaultMessage)
		user.unreadNotifications = 0;
	},
	function(xmlhttprequest, textstatus, message) {
		if (textstatus === "timeout") {
			//TODO: Have the website change to reflect that the user is offline
		}
		else {
			//TODO: Have case by case error handling
			console.error(xmlhttprequest.responseText);
			console.error(textstatus);
			console.error(message);
		}
	})
}

function getUnreadNotificationCount(onSuccess, onError) {
	var route = jsRoutes.controllers.NotificationController.getUnreadNotificationCount();
	ajaxSendFormData(new FormData(), route, onSuccess, onError)
}

function ignoreNotification(timeCreated, href) {
	var route = jsRoutes.controllers.NotificationController.ignoreNotification(timeCreated);
	ajaxSendFormData(new FormData(), route, function() {
		if(typeof href !== "undefined") {
			window.location = href
		}
	}, function() {
		//Error
	});
}