function setupFormCallbacks() {
	$("#post-update").click(function() {
		var projectId = $(this).attr("project-id")
		submitUpdate(projectId);
	})

	$("#login-button, .submit-button").click(function() {
		$("form:first").submit();
	});
}