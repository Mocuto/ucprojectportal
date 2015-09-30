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
	val PRIMARY_CONTACT_FULL : String;
	val TEAM_MEMBERS : String;
	val TEAM_MEMBERS_FULL : String;
	val STATE : String;
	val STATE_MESSAGE : String;

	implicit def index(p : Project)  : Option[Document] = {
		if(p.isDefined) {

			val doc = new Document();

			doc.add(new IntField(ID, p.id, Field.Store.YES));
			doc.add(new TextField(NAME, p.name, Field.Store.NO));
			doc.add(new TextField(DESCRIPTION, p.description, Field.Store.NO));
			doc.add(new TextField(CATEGORIES, p.categories.mkString(" "), Field.Store.NO));
			doc.add(new StringField(PRIMARY_CONTACT, p.primaryContact, Field.Store.NO));
			doc.add(new TextField(PRIMARY_CONTACT_FULL, User.get(p.primaryContact).fullName, Field.Store.NO))
			doc.add(new TextField(TEAM_MEMBERS_FULL, p.teamMembers.map(User.get(_).fullName).mkString(" "), Field.Store.NO));
			doc.add(new TextField(TEAM_MEMBERS, p.teamMembers.mkString(" "), Field.Store.NO));
			doc.add(new TextField(STATE, p.state, Field.Store.NO))
			doc.add(new TextField(STATE_MESSAGE, p.stateMessage, Field.Store.NO));

			Logger.debug(s"""Generating index document for ${doc.get("project-id")}""");

			return Some(doc);
		}

		else {
			None
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
 	val PRIMARY_CONTACT_FULL ="project-primary-contact-full"
 	val TEAM_MEMBERS = "project-team-members"
 	val TEAM_MEMBERS_FULL = "project-team-members-full"
 	val STATE = "project-state"
 	val STATE_MESSAGE = "project-state-message"
 
	def receive = {
		case ActorWork(p : Project) => sender ! work(p) //sender ! ActorResult[Project, Option[Document]](p, index(p))
		case ActorTerminate => sender ! ActorTerminated(context self);
	}

	def work(p : Project) = ActorResult[Project, Option[Document]](p, this index p)
}