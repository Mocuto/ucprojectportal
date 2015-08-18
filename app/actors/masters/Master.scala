package actors.masters

import actors.ActorMessageTypes._
import actors.workers.Worker
import akka.actor._
import akka.pattern.ask
import akka.routing._
import akka.util.Timeout

import play.api.libs.concurrent.Akka
import play.api.Play.current

import scala.reflect.ClassTag

trait WorkRouter {

	self : Actor =>

	protected val capActorSize = 5;

	def workerProps : Props;

	val routees = Vector.fill(capActorSize) {
		val r = context.actorOf(workerProps)
		context watch r
		ActorRefRoutee(r)
	}

	private val router = {
		Router(RoundRobinRoutingLogic(), routees)
	}

	protected def routeWork(w : ActorWork[_]) : Unit = {
		router.route(w, context self);
		onWorkRouted(w);
	}

	protected def handleResult(f : => Unit) : Unit = {
		f
		onResultHandled()
	}

	protected def onWorkRouted(w : ActorWork[_]) : Unit = {
		//Override
	}

	protected def onResultHandled() : Unit = {
		//override
	}

	protected def onActorTerminated(a : ActorRef) : Unit = {
		println("Terminating router");
		router.removeRoutee(a);
		val r = context.actorOf(workerProps)
		context watch r
		router.addRoutee(r);
	}
}

trait Master {

	def actorName : String;

	def masterProps : Props;

	protected val actor : ActorRef = Akka.system.actorOf(masterProps, name = actorName)

	def start() : Unit;

	def terminate() : Unit = {
		//Override
	}
}