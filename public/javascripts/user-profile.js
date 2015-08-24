function onUserProfileClicked() {
	var file = $(this).get(0).files[0]

	var username = $(this).attr("username")

	setUserProfile(username, file)
}

function onFollowUserButtonClicked() {
	var active = $(this).attr("active")
	var follower = $(this).attr("follower")
	var toFollow = $(this).attr("to-follow")
	var obj = this;

	if(active !== "true")
	{
		followUser(follower, toFollow, function() {

		})
		$(this).attr("active", true)
		$(".follow-user-button-caption").text("unwatch this user")
		$(".follow-user-button-caption").attr("active", true)

	}
	else
	{
		unfollowUser(follower, toFollow, function() {
		
			
			$(obj).css({"background-image" : "url('/assets/images/icons/eye-unselected.png')"});

			$(obj).bind("mouseleave", function() {
				$(obj).unbind("mouseleave");
				$(obj).css({"background-image" : ""});
			})
		})
		$(this).attr("active", false);
		$(".follow-user-button-caption").text("watch this user")
		$(".follow-user-button-caption").attr("active", false)
	}
}

function onEditUsersFollowingButtonClicked() {
	var active = $(this).attr("active")
	var follower = $(this).attr("follower")

	if(active !== "true")
	{

		$("#user-users-following").hide()
		$(".users-following-edit-field").show()
		$(this).attr("active", true)
	}
	else
	{
		var usersFollowing = $("#users-following-select").val() || [];

		$.map($("#users-following-select option"), function(option) {
			if(usersFollowing.indexOf($(option).val()) == -1) {
				$(option).removeAttr("selected")
			} else {
				$(option).attr("selected", "selected");
			}
		})

		var fullNames = $.map($("#users-following-select option[selected='selected']"), function(option) { return $(option).text()});

		$(".chosen").val('').trigger("chosen:updated")

		if(fullNames.length > 0)
		{
			$("#user-users-following").text(fullNames.join(", ").toLowerCase());
		}
		else
		{
			$("#user-users-following").text("no one");
		}

		$("#user-users-following").show()
		$(".users-following-edit-field").hide()
		$(this).attr("active", false)
		setUsersFollowing(usersFollowing, follower, function() {

		})
	}
}

function setupUserProfileCallbacks() {

	$(".user-profile-upload").click(function() {
		$("#user-profile-file").click()
	})

	$("#user-profile-file").change(onUserProfileClicked)

	$(".user-profile-main").mouseenter(function() {

		$(this).children(".user-profile-upload").animate({
			height: 164,
			bottom: 100
		}, {
			duration : 300,
			easing : "linear"
		})
			
		$(this).children(".user-profile-upload").children(".camera-icon").animate({
		 	opacity: 0
		}, {
		 	duration: 300,
		 	easing: "linear"
		})
			

	})
	$(".user-profile-main").mouseleave(function() {
		$(this).children(".user-profile-upload").animate({
			height: 66,
			bottom: 12
		}, 300)
		$(this).children(".user-profile-upload").children(".camera-icon").animate({
		 	opacity: 1
		 }, 300)
	})

	$(".follow-user-button").on("click", onFollowUserButtonClicked)
	$(".edit-users-following-button").on("click", onEditUsersFollowingButtonClicked)
	$(".users-following-edit-field").hide()
}