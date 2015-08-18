package enums

import scala.Enumeration

object ProjectActivityStatus extends scala.Enumeration {
	type ProjectActivityStatus = Value

	val Hot, Warm, Cold, Freezing, Frozen = Value

	def toString(p : ProjectActivityStatus) : String = p match {
		case Hot => "Hot"
		case Warm => "Warm"
		case Cold => "Cold"
		case Freezing => "Freezing"
		case Frozen => "Frozen"
		case _ => "Unknown"
	}
}