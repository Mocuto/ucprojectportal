import model.Project;


trait ProjectIndexer(
	ID : String,
	NAME : String,
	DESCRIPTION : String,
	CATEGORIES : String,
	PRIMARY_CONTACT : String,
	TEAM_MEMBERS : String,
	STATE_MESSAGE) {

	implicit def index(project : Project)  : Document = {
		val doc = new Document();

		doc.add(new IntField(ID), project.id, Field.Store.YES);
		doc.add(new TextField(NAME), project.name, Field.Store.NO);
		doc.add(new TextField(DESCRIPTION), project.description, Field.Store.NO);
		doc.add(new TextField(CATEGORIES), project.categories.mkString(" "), Field.Store.NO);
		doc.add(new StringField(PRIMARY_CONTACT), project.primaryContact, Field.Store.NO);
		doc.add(new TextField(TEAM_MEMBERS), project.teamMembers.mkString(" "), Field.Store.NO);
		doc.add(new TextField(STATE_MESSAGE), project.stateMessage, Field.Store.NO);

		return doc;
	}
}