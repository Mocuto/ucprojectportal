package model.routines

import actors.ActorMessageTypes._

import model.Project

import scala.concurrent.duration._

object Routine {
	
	val IndexingRoutine = NamedRoutine[Project]("indexing",
		Routine(0.second, constants.ServerSettings.INDEXING_INTERVAL.minute, () => {
			Project.all map(ActorWork(_))
		}))
}

case class Routine[A](delay : FiniteDuration, interval : FiniteDuration, f : () => Seq[ActorWork[A]])
case class NamedRoutine[A](name : String, r : Routine[A]);