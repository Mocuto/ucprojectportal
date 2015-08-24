package constants

object Messages {
	val ProjectsAreFreezing = "your projects are freezing!"
	val ProjectsAreCold = "your projects are getting cold!"
	val ProjectFrozenInactivity = "your project has been frozen due to inactivity"
	val ProjectLiked = "people like your project!"

	val UpdateLiked = "people like an update you posted!"

	val OfficeHourDigest = "weekly office hour digest"
	def capitalize(str : String) = str.split(" ").map(_.capitalize).mkString(" ");
}