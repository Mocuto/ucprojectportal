function onUserProfileClicked() {
	var file = $(this).get(0).files[0]

	var username = $(this).attr("username")

	setUserProfile(username, file)
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
}