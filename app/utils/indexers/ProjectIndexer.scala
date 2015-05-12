package utils.indexers

import model.Project;
import org.apache.lucene.document._

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

				println(s"""I'm running! ${doc.get("project-id")}""");

				return Some(doc);
			}
			case _ => None
		}


	}
}