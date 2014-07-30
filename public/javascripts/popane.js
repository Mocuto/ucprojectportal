(function( $ ) {
	var overlay = null;
	function buildOverlay(options) {

		var doesShow = (options["show"] === "true");

		var doesShowOverlay = (options["showOverlay"] === "true");

		if(overlay == null) {
			overlay = $(document.createElement("div"))
				.attr("id", "popane-overlay")
				.appendTo("body");
		}

		overlay.width(window.innerWidth)
			.height(window.innerHeight)

		overlay.attr(options.overlayAttrs);
		overlay.css(options.overlayStyles);

		if(doesShow == false || doesShowOverlay == false) {
			overlay.css("display", "none");
		}
		else {
			overlay.css("display", "block");
		}

		$( window ).resize(function(){
			overlay.width(window.innerWidth)
				.height(window.innerHeight)
		})
	}
	$.fn.popane = function (options) {
		options = (typeof options === "undefined") ? {} : options;

		options = $.extend( true, {}, $.fn.popane.defaults, options );
		
		$(this).css("position", options["position"]);
		$(this).css("z-index", options["zIndex"]);
		$(this).attr("popane", "true");
		
		var width = $(this).width();
		var height = $(this).height();
		var xPos = (window.innerWidth / 2) - (width / 2);
		
		if(options["x"] != null) {
			xPos = options["x"];
		}
		
		var yPos = (window.innerHeight / 2) - (height / 2);
		if(options["y"] != null) {
			yPos = options["y"];
		}

		$(this).css("left", String(xPos) + "px");
		$(this).css("top", String(yPos) + "px");

		var doesShow = (options["show"] === "true");

		var doesShowOverlay = options["showOverlay"];
		
		if(doesShow == false) {
			$(this).css("display", "none");
		}
		else {
			$(this).css("display", "block");
		}

		var this_ = this;
		$( window ).resize(function(){
			if(options["x"] != null) {
				xPos = options["x"];
			}
			
			var yPos = (window.innerHeight / 2) - (height / 2);
			if(options["y"] != null) {
				yPos = options["y"];
			}

			$(this_).css("left", String(xPos) + "px");
			$(this_).css("top", String(yPos) + "px");
		})

		buildOverlay(options);
		

		return this;
	}

	$.fn.popane.defaults = {
		x : null,
		y : null,
		show : "false",
		showOverlay : "true",
		position : "fixed",
		zIndex : "999",
		overlayAttrs : {

		},
		overlayStyles : {
			position : "fixed",
			left : "0px",
			top : "0px",
			zIndex : "998"
		}
	}
})(jQuery)