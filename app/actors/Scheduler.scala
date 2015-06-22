package actors

import actors.ActorMessageTypes._
import actors.masters.IndexerMaster

import akka.actor._

import model.Project

import play.api.libs.concurrent.Akka
import play.api.Play.current

import scala.collection.mutable.MutableList
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import utils.Conversions._


trait Scheduler {

	private val CheckOnDefaultName = "default"

	protected var cancellables = List[(String, Cancellable)]()

	protected def checkOn(interval : FiniteDuration, name : String)(f : => Unit) : List[(String, Cancellable)] = {
		val c = Akka.system.scheduler.schedule(interval, interval, f)

		cancellables = cancellables :+ (name, c);
		cancellables
	}

	protected def checkOn(interval : FiniteDuration)(f : => Unit) : List[(String, Cancellable)] = checkOn(interval, CheckOnDefaultName)(f);

	protected def cancelAll(name : String) = cancellables = (cancellables groupBy { case (n : String, c : Cancellable) =>
		if(n == name) {
			c.cancel()
		}
		n == name
	})(false);

}