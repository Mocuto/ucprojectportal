package actors.workers

import actors.ActorMessageTypes._
import akka.actor._

import model._

import org.apache.lucene.document.Document

import utils.indexers.ProjectIndexer

case class Work[A](items : Seq[A])
case class Result[A, B](value : Seq[B])

class Indexer extends Actor with ProjectIndexer  {


	//TODO: Store these in some kind of static class or config or something
	val ID =  "project-id"
	val NAME = "project-name"
 	val DESCRIPTION = "project-description"
 	val CATEGORIES = "project-categories"
 	val PRIMARY_CONTACT ="project-primary-contact"
 	val TEAM_MEMBERS = "project-team-members"
 	val STATE = "project-state"
 	val STATE_MESSAGE = "project-state-message"
 
	def receive = {
		case ActorWork(p : Project) => sender ! ActorResult[Project, Option[Document]](p, index(p))
		case ActorTerminate => sender ! ActorTerminated(context self);
	}
}