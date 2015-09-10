package constants

import play.Play

import org.joda.time._

object ServerSettings {
	val DebugUrl = Play.application.configuration.getString("host.debug"); //"65.31.51.202:9000"
	val ProductionUrl = Play.application.configuration.getString("host.production"); //"ucprojectportal.com"
	val ChosenHost = DebugUrl;
	val Protocol = Play.application.configuration.getString("host.protocol")
	val HostUrl = Protocol + "://" + ChosenHost;

	val AdminEmail =  Play.application.configuration.getString("admin.email")//"akomolot@mail.uc.edu"
	val AdminName =  Play.application.configuration.getString("admin.name")//"Tobi Akomolede"

	val IndexingDirectory = Play.application.configuration.getString("indexing.directory")

	val AuthenticationMode = enums.AuthenticationMode.fromString(Play.application.configuration.getString("authentication"))

	val ClusterName = Play.application.configuration.getString("cassandra.cluster")

	object ActivityStatus {
		val Hot = Days.days(Play.application.configuration.getInt("project.activity.hot"))
		val Warm = Days.days(Play.application.configuration.getInt("project.activity.warm"))
		val Cold = Days.days(Play.application.configuration.getInt("project.activity.cold"))
		val Freezing = Days.days(Play.application.configuration.getInt("project.activity.freezing"))
	}

	val ProjectWarningHour = Play.application.configuration.getInt("project.warning.hour")
	val ProjectDigestDay = Play.application.configuration.getInt("project.digest.day")
	val ProjectDigestHour = Play.application.configuration.getInt("project.digest.hour")

	val HotProjectsCount = Play.application.configuration.getInt("project.digest.project.count")
	val HotContributorsCount = Play.application.configuration.getInt("project.digest.contributor.count")

	val OfficeHourDay = Play.application.configuration.getInt("project.office.hour.day")
	val OfficeHourHour = Play.application.configuration.getInt("project.office.hour.hour")
}

