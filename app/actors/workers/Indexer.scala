package actors.workers

import actors.ActorMessageTypes._
import akka.actor._

import model._

import org.apache.lucene.document.Document
import org.apache.lucene.document._

import play.api.Logger

trait ProjectIndexer {

	val ID : String;
	val NAME : String;
	val DESCRIPTION : String;
	val CATEGORIES : String;
	val PRIMARY_CONTACT : String;
	val TEAM_MEMBERS : String;
	val STATE : String;
	val STATE_MESSAGE : String;

	implicit def index(project : Project)  : Option[Document] = {

		project match {
			case Project(
					id, 
					name, 
					description, 
					_, 
					_, 
					categories, 
					_, 
					primaryContact, 
					teamMembers, 
					state, 
					stateMessage, 
					true) => {
				val doc = new Document();

				doc.add(new IntField(ID, id, Field.Store.YES));
				doc.add(new TextField(NAME, name, Field.Store.NO));
				doc.add(new TextField(DESCRIPTION, description, Field.Store.NO));
				doc.add(new TextField(CATEGORIES, categories.mkString(" "), Field.Store.NO));
				doc.add(new StringField(PRIMARY_CONTACT, primaryContact, Field.Store.NO));
				doc.add(new TextField(TEAM_MEMBERS, teamMembers.mkString(" "), Field.Store.NO));
				doc.add(new TextField(STATE, state, Field.Store.NO))
				doc.add(new TextField(STATE_MESSAGE, stateMessage, Field.Store.NO));

				Logger.debug(s"""Generating index document for ${doc.get("project-id")}""");

				return Some(doc);
			}
			case _ => None
		}
	}
}

class Indexer extends Actor with ProjectIndexer with Worker[Project, Option[Document]] {

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
		case ActorWork(p : Project) => sender ! work(p) //sender ! ActorResult[Project, Option[Document]](p, index(p))
		case ActorTerminate => sender ! ActorTerminated(context self);
	}

	def work(p : Project) = ActorResult[Project, Option[Document]](p, this index p)
}