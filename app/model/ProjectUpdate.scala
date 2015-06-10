package model

import com.datastax.driver.core.{Cluster, ResultSet, ResultSetFuture, Row}
import com.github.nscala_time.time.Imports._
import com.websudos.phantom.Implicits._

import java.util.Date

import play.api.libs.Files._
import play.api.libs.json._
import play.api.mvc._

import scala.collection.JavaConversions._
import scala.concurrent._
import scala.concurrent.duration._

import utils._
import utils.nosql.CassieCommunicator

protected sealed class ProjectUpdateTable extends CassandraTable[ProjectUpdateTable, ProjectUpdate] {
	object project_id extends IntColumn(this) with PartitionKey[Int]
	object author extends StringColumn(this) with PrimaryKey[String]
	object time_submitted extends DateColumn(this) with PrimaryKey[Date]
	object categories extends SetColumn[ProjectUpdateTable, ProjectUpdate, String](this)
	object content extends StringColumn(this)
	object files extends ListColumn[ProjectUpdateTable, ProjectUpdate, String](this)

	override def fromRow(r : Row) = ProjectUpdate(
		projectId = project_id(r),
		author = author(r),
		timeSubmitted = time_submitted(r),
		content = content(r),
		files = files(r)
	)
}

object ProjectUpdateTable extends ProjectUpdateTable {
	override val tableName = "project_updates";
	implicit val session = CassieCommunicator.session

	def get(projectId : Int, author : String, timeSubmitted : Date) : Future[Option[ProjectUpdate]] = select
		.where(_.project_id eqs projectId)
		.and(_.author eqs author)
		.and(_.time_submitted eqs timeSubmitted)
		.one();
}

object ProjectUpdate {

	def applyIncomplete(content : String, projectId :  Int, timeSubmitted : Date) : ProjectUpdate = return ProjectUpdate(
		content, 
		projectId =  projectId, 
		timeSubmitted = timeSubmitted
	)

	def unapplyIncomplete(update : ProjectUpdate) : Option[(String, Int, Date)] = return Some(update.content, update.projectId, update.timeSubmitted)

	def undefined : ProjectUpdate = {
		return ProjectUpdate(
			isDefined = false,
			content = "",
			author = ""
		);
	}

	def get(user : User) : Seq[ProjectUpdate] = return CassieCommunicator.getUpdatesForUser(user.username);

	def get(projectId : Int, author : String, timeSubmitted : Date) = scala.concurrent.Await.result(ProjectUpdateTable.get(projectId, author, timeSubmitted), constants.Cassandra.defaultTimeout)

	def create (content: String, author: String, projectId : Int, files : Seq[(String, TemporaryFile)]) : ProjectUpdate = {
		val timeSubmitted = new Date();

		val update = ProjectUpdate(
			content = content,
			author = author,
			projectId = projectId,
			files = files.map(temporaryFile => ProjectFile.saveFile(temporaryFile, author, projectId, timeSubmitted).filename),
			timeSubmitted = timeSubmitted
		)

		return CassieCommunicator.addUpdateForProject(update)
	}

	def delete(update : ProjectUpdate) {
		update.files.foreach(file => {
			ProjectFile.delete(update.projectId, update.timeSubmitted, file)
		})
		CassieCommunicator.removeUpdate(update);
	}

	implicit def fromRow (row : Row) : ProjectUpdate = {
		row match {
			case null => return ProjectUpdate.undefined
			case row : Row => {
				return ProjectUpdate(
					row.getString("content"),
					row.getString("author"),
					row.getInt("project_id"),
					row.getDate("time_submitted"),
					row.getList("files", classOf[String])
				)
			}
		}
	}
	
}

case class ProjectUpdate (
	content: String,
	author : String = "",
	projectId : Int = -1,
	timeSubmitted: Date = new Date(),
						files : Seq[String] = List[String](),
						isDefined : Boolean = true) {
	def this (content : String, author : String, projectId :  Int, timeSubmitted : Date) = this(content, author, projectId, timeSubmitted, List[String](), true)


	implicit def toJson() : JsObject = { 
		return JsObject(
				Seq(
					"content" -> JsString(content), 
					"author" -> JsString(author), 
					"projectId" -> JsNumber(projectId),
					"timeSubmitted" -> JsString(timeSubmitted.toString),
					"files" -> Json.toJson(files)
			)
		)
	}

	def delete () { ProjectUpdate.delete(this) }
}