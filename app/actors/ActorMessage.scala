package actors

import akka.actor._
import play.api.libs.concurrent.Akka

sealed trait ActorMessage

object ActorMessageTypes {
	case class ActorWork[T](input : T)
	case class ActorResult[T, S](input : T, output : S)
	
	val ActorTerminate = "actor-terminate"

	case class ActorTerminated(actor : ActorRef)

	case class PrintMesssage(message : String)
		extends ActorMessage
}