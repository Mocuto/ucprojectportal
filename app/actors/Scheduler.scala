package actors

import actors.ActorMessageTypes._
import actors.masters.IndexerMaster
import akka.actor._

import com.github.nscala_time.time.Imports._

import model.Project

import org.joda.time._

import play.api.libs.concurrent.Akka
import play.api.Logger
import play.api.Play.current

import scala.collection.mutable.MutableList
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import utils.Conversions._


trait Scheduler {

	private val CheckOnDefaultName = "default"
	private val ScheduleAtDefaultName = "scheduleAt"

	protected var cancellables = List[(String, Cancellable)]()

	protected def checkOn(interval : FiniteDuration, name : String)(f : => Unit) : List[(String, Cancellable)] = {
		import play.api.libs.concurrent.Execution.Implicits._
		val c = Akka.system.scheduler.schedule(interval, interval, f)

		cancellables = cancellables :+ (name, c);
		cancellables
	}

	protected def checkOn(interval : FiniteDuration)(f : => Unit) : List[(String, Cancellable)] = checkOn(interval, CheckOnDefaultName)(f);

	def scheduleAtNext(day : Int, hour : Int, name : String)(f : => Unit) : List[(String, Cancellable)] = {
		val now = new DateTime();

		val dayNow = now.getDayOfWeek()
		val hourNow = now.getHourOfDay()

		val date = if(dayNow > day || dayNow == day && hourNow > hour) {
			now.plusWeeks(1).withDayOfWeek(day).withHourOfDay(hour)
		}
		else {
			now.withDayOfWeek(day).withHourOfDay(hour)
		}

		scheduleAt(date, name)(f)
	}

	def scheduleAtNext(hour : Int, name : String)(f : => Unit) : List[(String, Cancellable)] = {
		val now = new DateTime();

		val hourNow = now.getHourOfDay()
		val minutesNow = now.getMinuteOfHour()

		val date = if(hourNow >= hour) {
			now.plusDays(1).minusHours(hourNow - hour).minusMinutes(minutesNow)
		}
		else {
			now.plusHours(hour - hourNow).minusMinutes(minutesNow)
		}

		scheduleAt(date, name)(f)
	}

	def scheduleAtNext(hour : Int)(f : => Unit) : List[(String, Cancellable)] = scheduleAtNext(hour, ScheduleAtDefaultName)(f)

	def scheduleAt(date : DateTime, name : String)(f : => Unit) : List[(String, Cancellable)] = {
		val now = new DateTime();
		val interval = new FiniteDuration((now to date).millis, java.util.concurrent.TimeUnit.MILLISECONDS)

		Logger.debug(s"$this scheduled $name at $date $interval")

		import play.api.libs.concurrent.Execution.Implicits._

		implicit def funToRunnable(fun: => Unit) = new Runnable() { def run() : Unit = {
			Logger.debug(s"Running scheduled $name at time ${new org.joda.time.DateTime()}");
			//throw new Exception("This should not have run")
			fun
		} }

		val c = Akka.system.scheduler.scheduleOnce(interval, f)

		cancellables = cancellables :+ (name, c)

		cancellables
	}

	def isScheduled(name : String) : Boolean = cancellables map(_._1) contains(name);

	protected def cancelAll(name : String) = cancellables = (cancellables groupBy { case (n : String, c : Cancellable) =>
		if(n == name) {
			c.cancel()
		}
		n == name
	})(false);

}