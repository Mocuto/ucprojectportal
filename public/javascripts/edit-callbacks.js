function onEditButtonClicked() {
	var activated = $(this).attr("activated");
	var field = $(this).attr("for");
	var editField = ".edit-field[for='" + field +"']";

	if(activated !== "true") {
		$(this).css("background-image", "url(/assets/images/buttons/project-edit-active.png)");
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
		$(this).css("background-image", "");
		$(this).attr("activated", "false");

		$("#" + field).show()
		
		$(editField).hide();

		if(column === "name") {
			var name = $("#project-name-input").val()

			formData.append("name", name);

			$("#" + field).text(name.toLowerCase());
		}

		else if(column === "state") {
			var stateMessage = $("#project-state-message-input").val();
			var state = $("#project-state-select").val();

			formData.append("state-message", stateMessage);
			formData.append("state", state);

			$("#" + field).text(state)
			$("#project-state-message").text(stateMessage);
			if(state === STATE_IN_PROGRESS_NEEDS_HELP)
			{
				$("#project-state-message").show();
			}

			editProject(formData, function() {
					location.reload(true)
			})
			
			return;
		}

		else if (column === "description") {
			var description = $("#project-description-input").val();

			formData.append("description", description);

			$("#" + field).html(description.brTagify());
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
}

function setupEditCallbacks() {
	$(".edit-button").click(onEditButtonClicked)
}