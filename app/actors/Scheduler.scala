package actors

import actors.ActorMessageTypes._
import actors.masters.IndexerMaster

import akka.actor._

import model.Project
import model.routines._

import play.api.libs.concurrent.Akka
import play.api.Play.current

import scala.collection.mutable.MutableList
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


object Scheduler {
	val tasks = MutableList[Cancellable]()

	val routines = scala.collection.mutable.HashMap.empty[String, Cancellable];

	val DEFAULT_WAIT = 100 microsecond;

	val indexMaster = Akka.system.actorOf(Props[IndexerMaster], name = "index-master")

	def schedule[A](startDelay : FiniteDuration)(interval : FiniteDuration)(work : ActorWork[A]) : MutableList[Cancellable] = {
		tasks += Akka.system.scheduler.schedule(startDelay, interval, indexMaster, work);
	}

	def schedule[A](r : NamedRoutine[A]) : Cancellable = {

		r match {
			case NamedRoutine(name, Routine(startDelay, interval, func)) => {
				(routines.get(name)) match {
					case Some(cancellable: Cancellable) => {
						cancellable.cancel()
					}
					case None => //NOOP
				}
				val delayedFunc : () => Unit = ( () => indexMaster ! func() )

				val c = if(interval.toNanos <= 0) {
					Akka.system.scheduler.scheduleOnce(startDelay) { 
						delayedFunc()
					}
				}
				else {
					Akka.system.scheduler.schedule(startDelay, interval) {
						delayedFunc()
					}
				}

				routines += name -> c

				return c
			}
		}
	}

	def indexNow(project: Project) : Unit = indexMaster ! ActorWork(project);

}