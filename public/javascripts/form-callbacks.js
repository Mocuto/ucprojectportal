function setupFormCallbacks() {
	$("#post-update").click(function() {
		submitUpdate();
	})

	$("#login-button, .submit-button").click(function() {
		$("form:first").submit();
	});
}