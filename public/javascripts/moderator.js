function onEmeritusClicked() {
	var value = ($(this).attr("active") === "true") ? true : false
	var username = $(this).parent(".privilege-button-container").attr("username")

	var obj = this;

	setUserEmeritus(username, !value, function() {
		if(value) {
			$(obj).text("mark this user as emeritus")
			$(obj).animate({backgroundColor : "#ddd", color : "black"}, 500)
		}
		else {
			$(obj).text("unmark this user as emeritus")
			$(obj).animate({backgroundColor : "black", color : "white"}, 500)
		}
		$(obj).attr("active", !value)
	})
}

function onPrivilegeClicked() {
	var name = $(this).attr("privilege-name")
	var value = ($(this).attr("active") === "true") ? true : false
	var username = $(this).parent(".privilege-button-container").attr("username")

	var obj = this;

	setUserPrivilege(username, name, !value, function() {
		var text = $(obj).text();
		var prefix = text.substr(0, text.indexOf(":") + 1)

		if(value) {
			var fullText = prefix + " off"
			$(obj).text(fullText)
			$(obj).animate({backgroundColor : "#ddd", color : "black"}, 500)
		}
		else {
			var fullText = prefix + " on"
			$(obj).text(fullText)
			$(obj).animate({backgroundColor : "black", color : "white"}, 500)
		}
		$(obj).attr("active", !value)
	})
}

function onFinishVerifyClicked() {
	var username = $("#activate-inputs").attr("username")
	var firstName = $(".verify-first-name").val();
	var lastName = $(".verify-last-name").val();
	var preferredPronouns = $(".verify-preferred-pronouns").val();
	var position = $(".verify-position").val();

	verifyUser(username, firstName, lastName, preferredPronouns, position, function() {
		$("#activate-inputs").popane({
			show : "false"
		})
		$(".verify-button").hide();
	})
}

function onVerifyClicked() {
	$("#activate-inputs").popane({
		show : "true"
	})
	$("#popane-overlay").css("backgroundColor", "black");
}

function setupModerator() {
	$(".emeritus-button").on("click", onEmeritusClicked);
	$(".privilege-button").on("click", onPrivilegeClicked);
	$(".verify-button").on("click", onVerifyClicked);
	$(".finish-verify-button").on("click", onFinishVerifyClicked)
	$(".non-sg-button").on("click", function() {
		$(".verify-position").val("NON-SG")
	})
}