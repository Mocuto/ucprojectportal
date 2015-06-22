package actors.workers

import actors.ActorMessageTypes._

trait Worker[T,S] {
	def work(input : T) : ActorResult[T, S]
}