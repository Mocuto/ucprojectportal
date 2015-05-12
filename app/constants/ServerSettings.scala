package constants

import play.Play

object ServerSettings {
	val DEBUG_URL = Play.application.configuration.getString("host.debug"); //"65.31.51.202:9000"
	val PRODUCTION_URL = Play.application.configuration.getString("host.production"); //"ucprojectportal.com"

	val CHOSEN_HOST = DEBUG_URL;

	val HOST_URL = "http://" + CHOSEN_HOST;

	val ADMIN_EMAIL =  Play.application.configuration.getString("admin.email")//"akomolot@mail.uc.edu"
	val ADMIN_NAME =  Play.application.configuration.getString("admin.name")//"Tobi Akomolede"

	val INDEXING_INTERVAL : Int = Play.application.configuration.getInt("routine.indexing.interval");
}

