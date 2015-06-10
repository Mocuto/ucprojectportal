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

function dialog(content) {
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
		.html(content)
		//.html("<span style='font-weight:500'>thank you!</span> your request to join has been sent to the project's author.")
		.appendTo(notificationBox)

	notificationBox.popane({show : "true"});
}

function setupLayout() {
	$(".textarea-description").autosize()

	$(".chosen").chosen({
			no_results_text: "oops, nothing found!",
			placeholder_text_multiple : "select options"
	})

	$(".edit-field").css("display", "none");

	$(".popane").popane();

	$(".popane-menu").popane({
		position : "absolute"
	})

	$(".dashboard-item").hover(function() {
		$(this).find(".dashboard-item-text").animate({
			"color" : "white"
		}, 300);
	}, function() { //Mouse leave
		$(this).find(".dashboard-item-text").animate({
			"color" : "black"
		}, 300)
	});

	$("#login-button").hover(function() {
		$(this).animate({
			"backgroundColor" : "#aedefc"
		}, 300)
	}, function() { //Mouse leave
		$(this).animate({
			"backgroundColor" : "#fa544f"
		}, 300)
	})

	$("#activate-sg-button").click(function() {
		$(".activate-button").fadeOut(500, function() {
			$("#activate-inputs").css({
				"display": "block",
				"opacity": 0,
				"position": "relative",
				"bottom" : "-100px"});

			$("#activate-inputs").animate({
				"opacity": 1.0,
				"bottom": "0px"
			}, {
				duration: 450,
				complete: function() {
					$("#activate-inputs").css("position", "relative")
				}
			})
		})
	})

	var initialWidth = $(".title-nav").width() //Grab the left position left first
	var initialPaddingLeft = parseInt($(".title-nav").css("padding-left"));

	function onWindowResize() {
		var width = $(this).width();
		initialWidth = width;

		var notificationTextWidth = $("#notification-text").width();
		if(width < 1024 + notificationTextWidth) {
			$("#uc-sg").hide()

		}
		else {
			$("#uc-sg").show()
			$('.title-nav').css("width", "auto");
		}

	    $('.title-nav').css({
				'width': $(this).scrollLeft() + initialWidth, //Use it later
				'paddingLeft' : $(this).scrollLeft() + initialPaddingLeft
		});
	}

	onWindowResize.call($(window));

	$(window).resize(onWindowResize);

	$(window).scroll(function(){ 
		var width = $(this).width();
		var notificationTextWidth = $("#notification-text").width();
		//if(width < 1024 + notificationTextWidth)
	 {
		    $('.title-nav').css({
				'width': $(this).scrollLeft() + initialWidth, //Use it later
				'paddingLeft' : $(this).scrollLeft() + initialPaddingLeft
			});
		}
		//else {
		//	$('.title-nav').css("width", "auto");
		//}

	});
}