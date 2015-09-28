function showOfficeHoursWindow(event) {

	$("#popane-overlay").css("backgroundColor", "black");
	$("#office-hours-window").popane({
		show : "true"
	})

	$("#office-hour-project-select").chosen({
		no_results_text: "oops, nothing found!",
		placeholder_text_multiple : "select options",
		width: "224px"
	})

	var todayEl = document.querySelector('#office-hour-today');
	$(todayEl).attr("value", +todayEl.innerText)

	od = new Odometer({
	  el: todayEl,
	  value: +todayEl.innerHTML,

	  // Any option (other than auto and selector) can be passed in here
	  format: '(,ddd).dd'
	});

	var thisWeekEl = document.querySelector('#office-hour-this-week');
	$(thisWeekEl).attr("value", +thisWeekEl.innerText)

	od = new Odometer({
	  el: thisWeekEl,
	  value: +thisWeekEl.innerHTML,

	  // Any option (other than auto and selector) can be passed in here
	  format: '(,ddd).dd'
	});

	var allTimeEl = document.querySelector('#office-hour-all-time');
	$(allTimeEl).attr("value", +allTimeEl.innerText)

	od = new Odometer({
	  el: allTimeEl,
	  value: +allTimeEl.innerHTML,

	  // Any option (other than auto and selector) can be passed in here
	  format: '(,ddd).dd'
	});


	event.stopPropagation();
}

function onSubmitHoursClicked() {
	var reqBody = {
	      date: $("#mDate").val(),
	      projectId: $("#office-hour-project-select").val(),
	      hours: $("#mHours").val(),
	      comment: $("#mComments").val(),
	      markAsUpdate: $("#mPostUpdate").is(":checked")
	};

	$.post("/officehour/logHour", reqBody, function(data) {
	    $("#logTimeError").html("");
	    $("#mDate").val("");
	    $("#office-hour-project-select").val(-1);
	    $('#office-hour-project-select').trigger("chosen:updated");
	    $("#mHours").val("");
	    $("#mComments").val("");

	    var hourDate = moment(reqBody.date, "MM/DD/YYYY")
	    var today = moment(moment().format("MM/DD/YYYY"), "MM/DD/YYYY")

	    if(today.diff(hourDate) == 0) {
		    var todayVal = +$("#office-hour-today").attr("value")
		    $("#office-hour-today").html((+todayVal) + (+reqBody.hours))
		    $("#office-hour-today").attr("value", (+todayVal) + (+reqBody.hours))
	    }

	    if(moment().week() == hourDate.week()) {
		    var thisWeekVal = +$("#office-hour-this-week").attr("value")
		    var targetVal = +($("#office-hour-this-week").attr("target-value"))

		    $("#office-hour-this-week").html((+thisWeekVal) + (+reqBody.hours))
		    $("#office-hour-this-week").attr("value", (+thisWeekVal) + (+reqBody.hours))

		    if((+thisWeekVal) + (+reqBody.hours) >= targetVal)
		    {
		    	$('.office-hour-mark-value[warning="true"]').animate({
		    		color : "#00DC75"
		    	})
		    }	    	
	    }

	    var allTimeVal = +$("#office-hour-all-time").attr("value")
	    $("#office-hour-all-time").html((+allTimeVal) + (+reqBody.hours))
	    $("#office-hour-all-time").attr("value", (+allTimeVal) + (+reqBody.hours))

	}).error(function(data) {
	    $("#logTimeSuccess").html("");
	    $("#logTimeError").html(data.responseText);
	});
}

function setupOfficeHoursCallbacks() {
	$(".office-hour-button").on("click", showOfficeHoursWindow)
	$("#submitHours").on("click", onSubmitHoursClicked);

	$("#mDate").datepicker();
}