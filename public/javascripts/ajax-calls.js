
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
		if(typeof onError !== "undefined")
		{
			onError.call(this, error, textstatus, message);			
		}
	}
});				
}

function verifyUser(username, firstName, lastName, preferredPronouns, position, officeHourRequirement, cellPhone, callback) {
	var route = jsRoutes.controllers.ModerationController.verify(username)

	var formData = new FormData()
	formData.append("first_name", firstName)
	formData.append("last_name", lastName)
	formData.append("preferred_pronouns", preferredPronouns)
	formData.append("position", position)
	formData.append("office_hour_requirement", officeHourRequirement)
	formData.append("cell_number", cellPhone)

	ajaxSendFormData(formData, route, callback)
}

function setUserProfile(username, file) {
	var route = jsRoutes.controllers.UserController.profilePic(username)

	var formData = new FormData();
	formData.append("file", file);

	ajaxSendFormData(formData, route, function(response) {
		var path = response.path;

		$('.user-profile[username="' + username + '"]').css({
			"background-image" : "url('" + path + "')"
		}).children(".user-profile-text").html("&nbsp; &nbsp;")

		$('.empty.user-profile-main[username="' + username + '"]').removeClass("empty")
	})
}

function setUserEmeritus(username, value, callback) {
	var route = jsRoutes.controllers.ModerationController.emeritus(username)

	var formData = new FormData();
	formData.append("value", value);

	ajaxSendFormData(formData, route, callback)
}

function setUserPrivilege(username, privilegeName, value, callback) {
	var route = jsRoutes.controllers.ModerationController.editUserPrivileges(username)

	var formData = new FormData();
	formData.append("privilege-name", privilegeName)
	formData.append("privilege-value", value)

	ajaxSendFormData(formData, route, callback)
}

function editProject(formData, optionalCallback) {
	if (typeof optionalCallback === "undefined") {
		optionalCallback = function() {

		}
	}

	var route = jsRoutes.controllers.ProjectController.edit(PROJECT_ID);
	ajaxSendFormData(formData, route, optionalCallback, function() {

	})
}

function leaveProject(projectId) {
	var route = jsRoutes.controllers.ProjectController.leave(projectId);
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

function setUsersFollowing(usersFollowing, follower, callback) {
	var formData = new FormData();

	for(var i = 0; i < usersFollowing.length; i++) {
		formData.append("users-following", usersFollowing[i]);
	}

	var route = jsRoutes.controllers.ModerationController.editUserFollowing(follower)
	ajaxSendFormData(formData, route, callback)
}

function followUser(follower, toFollow, callback) {
	var route = jsRoutes.controllers.UserController.follow(follower, toFollow)
	ajaxSendFormData(new FormData(), route, callback)
}

function unfollowUser(follower, toFollow, callback) {
	var route = jsRoutes.controllers.UserController.unfollow(follower, toFollow)
	ajaxSendFormData(new FormData(), route, callback)
}

function likeUpdate(projectId, author, timeSubmittedStr, callback) {
	var route = jsRoutes.controllers.ProjectUpdateController.like(projectId, author, timeSubmittedStr);
	ajaxSendFormData(new FormData(), route, callback)
}

function unlikeUpdate(projectId, author, timeSubmittedStr, callback) {
	var route = jsRoutes.controllers.ProjectUpdateController.unlike(projectId, author, timeSubmittedStr);
	ajaxSendFormData(new FormData(), route, callback)
}

function likeProject(projectId, callback) {
	var route = jsRoutes.controllers.ProjectController.like(projectId);
	ajaxSendFormData(new FormData(), route, callback)
}

function unlikeProject(projectId, callback) {
	var route = jsRoutes.controllers.ProjectController.unlike(projectId);
	ajaxSendFormData(new FormData(), route, callback)
}

function followProject(projectId, callback) {
	var route = jsRoutes.controllers.ProjectController.follow(projectId);
	ajaxSendFormData(new FormData, route, callback);
}

function unfollowProject(projectId, callback) {
	var route = jsRoutes.controllers.ProjectController.unfollow(projectId);
	ajaxSendFormData(new FormData(), route, callback);
}

function editUpdate(projectId, author, timeSubmitted, content) {
	var formData = new FormData();

	formData.append("content", content);

	var route = jsRoutes.controllers.ProjectUpdateController.edit(projectId, author, timeSubmitted)
	ajaxSendFormData(formData, route, function(data) {
		$('.roundbox.update[project-id="' + projectId + '"][author="' + author + '"][time-submitted="' + timeSubmitted + '"]')
			.find(".edit-field").css("display", "none")

		$('.roundbox.update[project-id="' + projectId + '"][author="' + author + '"][time-submitted="' + timeSubmitted + '"]')
			.find(".content").css("display", "block")

		$('.roundbox.update[project-id="' + projectId + '"][author="' + author + '"][time-submitted="' + timeSubmitted + '"]')
			.find(".content").html(content.brTagify());
	})
}

function submitUpdate(projectId) {
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
	var route = jsRoutes.controllers.ProjectUpdateController.submit(projectId);
	ajaxSendFormData(formData, route, function(data) {

		var updateHtml = $($.parseHTML(data["html"])[1]);
		updateHtml.hide();

		$("#update-input").val("");

		$(".update-group").prepend(updateHtml).slideDown();
		updateHtml.slideDown();

		var fileInputs = $(".file-inputs")
		$(".file-inputs").parents("form").trigger("reset");
		$("#update-files-filenames").text("");
		$("#update-files-mask").animate( {
			backgroundColor : "#ddd"
		})

		var fileHtmlGroup = $.parseHTML(data["fileHtml"]) || []; 
		
		for(var i = 1; i < fileHtmlGroup.length; i++) {
			var fileHtml = $(fileHtmlGroup[i]);
			$(".file-group").prepend(fileHtml)
		}

		setupUpdateMenuCallbacks();

	});
}

function deleteUpdate(projectId, author, timeSubmitted) {

	var route = jsRoutes.controllers.ProjectUpdateController.delete(projectId, author, timeSubmitted)
	ajaxSendFormData(new FormData(), route, function(data) {
		$('.roundbox.update[project-id="' + projectId + '"][author="' + author + '"][time-submitted="' + timeSubmitted + '"]').slideUp();
	}, function() {
		//TODO
	})
}

function requestJoin(projectId) {
	var route = jsRoutes.controllers.RequestController.join(projectId);

	ajaxSendFormData(new FormData(), route, function() {

		dialog("<span style='font-weight:500'>thank you!</span> your request to join has been sent to the project's author.");

		//alert("Request sent");
	}, function(xmlhttprequest, textstatus, message) {
		//TODO: Handle this error
	});
}

function acceptRequest(projectId, requester) {
	var route = jsRoutes.controllers.RequestController.accept(projectId, requester);

	ajaxSendFormData(new FormData(), route, function() {

	}, function() {

	});
}

function ignoreRequest(projectId, requester) {
	var route = jsRoutes.controllers.RequestController.ignore(projectId, requester);

	ajaxSendFormData(new FormData(), route, function() {
		//Success
	}, function() {
		//Error
	});
}

function resetUnreadNotifications() {

	var route = jsRoutes.controllers.NotificationController.resetUnread();
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
	var route = jsRoutes.controllers.NotificationController.getUnreadCount();
	ajaxSendFormData(new FormData(), route, onSuccess, onError)
}

function ignoreNotification(timeCreated, href) {
	var route = jsRoutes.controllers.NotificationController.ignore(timeCreated);
	ajaxSendFormData(new FormData(), route, function() {
		if(typeof href !== "undefined") {
			window.location = href
		}
	}, function() {
		//Error
	});
}

function clearAllNotifications() {
	var route = jsRoutes.controllers.NotificationController.clearAll();
	ajaxSendFormData(new FormData, route, function() {
		$(".notification-item").slideUp();
	}, function() {
		//Error
	})
}