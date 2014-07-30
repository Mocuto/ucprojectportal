

var PULSE_INTERVAL = 1000;

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

	var route = jsRoutes.controllers.Application.editProject(PROJECT_ID);
	ajaxSendFormData(formData, route, optionalCallback, function() {

	})
}

function leaveProject(projectId) {
	var route = jsRoutes.controllers.Application.leaveProject(projectId);
	ajaxSendFormData(new FormData(), route, function() {
		$("#projectbox-" + projectId).parents("td").animate({width : "0px"}, 400, "swing", function() {
			$(this).hide();
		})
	}, function() {

	})
}

function submitUpdate() {
	var content = $("#update-input").val();
	var files = $("#update-files").get(0).files;
	var formData = new FormData();
	formData.append("content", content);
	for(var i = 0; i < files.length; i++) {
		formData.append("file" + i, files[i]);
	}
	var ajax = {
		sucess: onProjectSubmitSuccess,
		error: onProjectSubmitError
	}
	var route = jsRoutes.controllers.Application.submitUpdate();
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
	var route = jsRoutes.controllers.Application.requestJoin(projectId);

	ajaxSendFormData(new FormData(), route, function() {

		$("#popane-overlay").css("backgroundColor", "white");

		var notificationBox = $(document.createElement("div"))
			.addClass("roundbox")
			.addClass("popane")
			.addClass("notification-popup")
			.appendTo("body");

		$(document.createElement("span"))
			.addClass("close-button")
			.text("close")
			.appendTo(notificationBox);
		$(document.createElement("div"))
			.html("<span style='font-weight:500'>thank you!</span> your request to join has been sent to the project's author.")
			.appendTo(notificationBox)

		notificationBox.popane({show : "true"});

		//alert("Request sent");
	}, function(xmlhttprequest, textstatus, message) {
		//TODO: Handle this error
	});
}

function acceptRequest(projectId, requester) {
	var route = jsRoutes.controllers.Application.acceptRequest(projectId, requester);

	ajaxSendFormData(new FormData(), route, function() {

	}, function() {

	});
}

function ignoreRequest(projectId, requester) {
	var route = jsRoutes.controllers.Application.ignoreRequest(projectId, requester);

	ajaxSendFormData(new FormData(), route, function() {
		//Success
	}, function() {
		//Error
	});
}

function resetUnreadNotifications() {

	var route = jsRoutes.controllers.Application.resetUnreadNotifications();
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
	var route = jsRoutes.controllers.Application.getUnreadNotificationCount();
	ajaxSendFormData(new FormData(), route, onSuccess, onError)
}

function ignoreNotification(timeCreated, href) {
	var route = jsRoutes.controllers.Application.ignoreNotification(timeCreated);
	ajaxSendFormData(new FormData(), route, function() {
		if(typeof href !== "undefined") {
			window.location = href
		}
	}, function() {
		//Error
	});
}

function dialogYesNo(message, yesText, noText, yesCallback, noCallback) {
	var notificationBox = $(document.createElement("div"))
	.addClass("roundbox")
	.addClass("popane")
	.addClass("notification-popup")
	.css("backgroundColor", "white")
	.appendTo("body");

	var table = $(document.createElement("table")).appendTo(notificationBox);
	$(document.createElement("td"))
		.text(message)
		.attr("colspan", "2")
		.appendTo(
			$(document.createElement("tr"))
				.appendTo(table)
	)
	var optionTd = $(document.createElement("td"))
		.addClass("dialog-option-td")
		.appendTo(
		$(document.createElement("tr")).appendTo(table)
	);
	$(document.createElement("span"))
		.text(yesText)
		.addClass("button")
		.css("marginRight", "64px")
		.click(function() {
			yesCallback.call(this);
			notificationBox.popane({show : "false"});
		})
		.appendTo(optionTd)

	$(document.createElement("span"))
		.text(noText)
		.addClass("button")
		.click(function() {
			noCallback.call(this);
			notificationBox.popane({show : "false"});
		})
		.appendTo(optionTd)

	notificationBox.popane({show : "true"});
}

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
	$(".textarea-description").autosize()

	$(".chosen").chosen({
			no_results_text: "oops, nothing found!",
			placeholder_text_multiple : "select options"
	})

	$(".edit-field").css("display", "none");

	$("#post-update").click(function() {
		submitUpdate();
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

	$("#update-input").click(function() {
		$(".post-update-container").css("display", "block");
	})
	$("#login-button, .submit-button").click(function() {
		$("form:first").submit();
	});

	$("#notification-text").click(function() {
		resetUnreadNotifications();

		$("#popane-overlay").css("backgroundColor", "black");
		$("#notifications-window").popane({
			show : "true"
		})
	})


	$(".projectbox img, .projectbox .projectbox-title").click(function(event) {
		var expanded = $(this).parents(".projectbox").attr("expanded");

		if(expanded == "false") {
			$(this).parents(".projectbox").find(".projectbox-description").slideDown( {
				done : function(num) {
					var packeryParent = $(event.target).parents(".projectbox-container");
					packeryParent.isotope('layout')
				}
			});
		}
		else {
			$(this).parents(".projectbox").find(".projectbox-description").slideUp({
				step : function(num) {
					var packeryParent = $(event.target).parents(".projectbox-container");
					packeryParent.isotope('layout')
			
				}
			});
		}
		$(this).parents(".projectbox").attr("expanded", (expanded == "true") ? "false" : "true");

		
	});

	$(document).on("click", ".close-button",  function() {
		$(this).parents(".popane").popane({
			"show" : "false"
		})
	})

	$("#project-state-select").change(function() {
		if($(this).val() === STATE_IN_PROGRESS_NEEDS_HELP) {
			$("#project-state-message-input").show();
		}
	})

	$("#project-update-log-button").click(function() {
		$(this).css("color", "white");
		$("#project-all-files-button").css("color", "#ff9c9c");
		$("#project-update-log").show().parent().css("width", "100%");
		$("#project-all-files").hide().parent().css("width", "0%");
	})

	$("#project-all-files-button").click(function() {
		$(this).css("color", "white");
		$("#project-update-log-button").css("color", "#ff9c9c");
		$("#project-update-log").hide().parent().css("width", "0%");
		$("#project-all-files").show().parent().css("width", "100%");
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
				gutter : 32
			}
		})
	});

	/*$(".filter-button-container").isotope({
		masonry: {
  			columnWidth: 64,
  			gutter: 3,
  			isFitWidth: true
  		}
	})*/
	if(typeof activeFilters !== "undefined") {
		if(activeFilters.length > 0) {
			var filterStr = activeFilters.map(function(str) {
					return "." + str
				}).join("");

				$(".projectbox-container").isotope({
					filter : filterStr
			})
		}		
	}


	$(".filter-button").click(function() {
		var activated = $(this).attr("activated");
		var filterVal = $(this).attr("for");
		if(activated == "true") {
			//Remove filter

			$(this).attr("activated", "false");
			var index = activeFilters.indexOf(filterVal);

			$(this).animate({
				backgroundColor : "white"
			})

			if(index != -1) {
				activeFilters.splice(index, 1);
			}
		} 
		else {
			//Add filter

			$(this).attr("activated", "true");
			$(this).animate({
				backgroundColor : "#aedefc"
			})

			activeFilters.push(filterVal);
		}
		if(activeFilters.length == 0) {
			$(".projectbox-container").isotope({
				filter : '*'
			})
			var urlPath = "/filter"
			window.history.pushState({},"", urlPath);
		}
		else {
			var filterStr = activeFilters.map(function(str) {
				return "." + str
			}).join("");

			$(".projectbox-container").isotope({
				filter : filterStr
			})

			var urlPath = "/filter/" + activeFilters.join(",");
			window.history.pushState({},"", urlPath);
		}
	});


	$(".edit-button").click(function() {
		var activated = $(this).attr("activated");
		var field = $(this).attr("for");
		var editField = ".edit-field[for='" + field +"']";

		if(activated !== "true") {
			$(this).text("ok");
			$(this).attr("activated", "true");

			$("#" + field).hide();

			$(editField).show();

			if(field === "project-state") {
				var state = $("#project-state-select").val();
				if(state === STATE_IN_PROGRESS_NEEDS_HELP) {
					$("#project-state-message-input").show();
				}
			}
		}
		else {
			//Send edit to server
			var formData = new FormData();
			var column = field.replace("project-", "");

			//Change elements back
			$(this).text("edit");
			$(this).attr("activated", "false");

			$("#" + field).show()
			
			$(editField).hide();


			if(column === "state") {
				var stateMessage = $("#project-state-message-input").val();
				var state = $("#project-state-select").val();

				formData.append("state-message", stateMessage);
				formData.append("state", state);

				$("#" + field).text(state)
				$("#project-state-message").text(stateMessage);
				if(state === STATE_IN_PROGRESS_NEEDS_HELP) {
					$("#project-state-message").show();
				}
			}

			else if (column === "description") {
				var description = $("#project-description-input").val();

				formData.append("description", description);

				$("#" + field).text(description);
			}

			else if (column === "categories") {
				var categories = $("#project-categories-select").val();

				for(var i = 0; i < categories.length; i++) {
					formData.append("categories", categories[i]);
				}


				$("#" + field).text(categories.join(", "));
			}

			else if (column === "team-members") {

				var newTeamMembers = $("#project-team-members-select").val();
				$.map($("#project-team-members-select option"), function(option) {
					if(newTeamMembers.indexOf($(option).val()) == -1) {
						$(option).removeAttr("selected")
					} else {
						$(option).attr("selected", "selected");
					}
				})
				var fullNames = $.map($("#project-team-members-select option[selected='selected']"), function(option) { return $(option).text()});

				if(newTeamMembers.indexOf(PRIMARY_CONTACT) == -1) {
					newTeamMembers.push(PRIMARY_CONTACT);
				}

				for(var i = 0; i < newTeamMembers.length; i++) {
					formData.append("team-members", newTeamMembers[i]);
					if(teamMembers.indexOf(newTeamMembers[i]) == -1) {
						$(document.createElement("option"))
							.attr("value", newTeamMembers[i])
							.text(fullNames[i])
							.appendTo($("#project-primary-contact-select"))
					}
				}

				for (var i = 0; i < teamMembers.length; i++) {
					if(newTeamMembers.indexOf(teamMembers[i]) == -1) {
						var option = $("#project-primary-contact-select option[value='" + teamMembers[i] + "']")
						option.remove();
					}
				}

				$(".chosen").val('').trigger("chosen:updated")

				teamMembers = newTeamMembers;

				$("#" + field).text(fullNames.join(", ").toLowerCase());

			}

			else if (column === "primary-contact") {
				dialogYesNo("Are you sure you want to change the primary contact?", "yes", "no", function() {
					var primaryContact = $("#project-primary-contact-select").val();
					formData.append("primary-contact", primaryContact);

					editProject(formData, function() {
						location.reload(true)
					})
				}, function() {
					
				})
				return;
			}
			editProject(formData);
		}

	})


	$(".popane").popane();
	pulse();
})