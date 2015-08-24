function setupHeroCardCallbacks() {
	$(".hero-card-container").imagesLoaded( function(){
		$(".hero-card-container").isotope({
			layoutMode : 'packery',
			itemSelector : '.hero-card-isotope-item',
			packery: {
				rowHeight: 320,
				gutter : 32
			}
		})
	});


}