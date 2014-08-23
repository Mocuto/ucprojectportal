package utils.nosql

import com.datastax.driver.core.{Cluster, ResultSet, ResultSetFuture, Row}
import com.datastax.driver.core.exceptions._

import java.util.Date

import model._

import scala.collection.JavaConversions._

import utils.{Conversions, PasswordHasher}

trait ProjectFileSqlCommunicator extends BaseSqlCommunicator {

	private[nosql] val FILES : String
	private[nosql] val FILES_INSERT_FIELDS : String

	def getFilesForProject(project : Project) : Seq[ProjectFile] = {
		val executeString = s"select * from $FILES where project_id = ${project.id}";
		executeAsync(executeString) match {
		  case None => return List[ProjectFile]();
		  case Some(r : ResultSetFuture) => {
		    val rows = r.getUninterruptibly().all();
		    return rows.map(row => ProjectFile.fromRow(row)).sortWith((a, b) => a.timeSubmitted.after(b.timeSubmitted))
		  }
		}
	}

	def getFile(projectId : Int, timeSubmitted : Date, filename : String) : ProjectFile = {
		val timestamp = utils.Conversions.dateToStr(timeSubmitted);

		val executeString = s"select * from files where project_id = $projectId and time_submitted = '$timestamp' and filename = '$filename'";
		execute(executeString) match {
		  case None => return ProjectFile.undefined
		  case Some(r : ResultSet) => {
		    val row = r.one();
		    return ProjectFile.fromRow(row);
		  }
		}
	}

	def addFile(projectFile : ProjectFile) : ProjectFile = {

		val timestamp = Conversions.dateToStr(projectFile.timeSubmitted);
		val values = s"${projectFile.projectId}, '$timestamp', '${projectFile.filename}', '${projectFile.originalName}', '${projectFile.author}'"
			val executeString = s"insert into $FILES($FILES_INSERT_FIELDS) VALUES($values)";
			execute(executeString);

		return projectFile
	}

	def removeFile(file : ProjectFile) {
		val projectId = file.projectId;
		val timestamp = utils.Conversions.dateToStr(file.timeSubmitted);
		val filename = file.filename;

		val executeString = s"delete from $FILES where project_id = $projectId and time_submitted = '$timestamp' and filename = '$filename'";
		execute(executeString)
	}

}