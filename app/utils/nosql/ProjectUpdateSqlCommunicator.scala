package utils.nosql

import com.datastax.driver.core.{Cluster, ResultSet, ResultSetFuture, Row}
import com.datastax.driver.core.exceptions._

import java.util.Date

import model._

import scala.collection.JavaConversions._

import utils.{Conversions, PasswordHasher}

trait ProjectUpdateSqlCommunicator extends BaseSqlCommunicator {

	private[nosql] val PROJECT_UPDATES : String
	private[nosql] val PROJECT_UPDATES_INSERT_FIELDS : String

	def getLatestUpdateForProject(projectId: Int) : ProjectUpdate = {
	    getUpdatesForProject(projectId) match {
	      case Nil => return ProjectUpdate.undefined
	      case x :: restOfList => return x
    }}

	def getUpdatesForProject(projectId: Int) : Seq[ProjectUpdate] = {
	  	val executeString = s"select * from $PROJECT_UPDATES WHERE project_id=$projectId";

	    execute(executeString) match {
	      case None => return List[ProjectUpdate]();
	      case Some(r : ResultSet) => {
	        val rows = r.all()
	        return rows.map(row => ProjectUpdate.fromRow(row)).sortWith((a,b) => a.timeSubmitted.after(b.timeSubmitted));
	      }
    }}

	def getNumberOfUpdatesForProject(projectId: Int) : Long = {
		val executeString = s"select count(*) from $PROJECT_UPDATES"

		return execute(executeString).get.one().getLong("COUNT");
	}

	def addUpdateForProject(update: ProjectUpdate) : ProjectUpdate = {
	    val timestamp = Conversions.dateToStr(update.timeSubmitted)
	    val content = update.content.replace("'", "''")
	    val files = s"[" + update.files.map(file => s"'$file'").mkString(",") + s"]"
			val values = s"${update.projectId} ,'${update.author}', '$content', '$timestamp', $files"

			val executeString = s"insert into $PROJECT_UPDATES($PROJECT_UPDATES_INSERT_FIELDS) values($values)";
	    execute(executeString)

	    return update
	}

	def removeUpdate(update : ProjectUpdate) {
		val projectId = update.projectId;
		val author = update.author;
		val timeSubmitted = utils.Conversions.dateToStr(update.timeSubmitted)

		val executeString = s"delete from $PROJECT_UPDATES where project_id = $projectId and author = '$author' and time_submitted = '$timeSubmitted'"
		execute(executeString)
	}
}