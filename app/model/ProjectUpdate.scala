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
	object time_editted extends DateColumn(this) with PrimaryKey[Date]
	object time_submitted extends DateColumn(this) with PrimaryKey[Date]
	object categories extends SetColumn[ProjectUpdateTable, ProjectUpdate, String](this)
	object content extends StringColumn(this)
	object files extends ListColumn[ProjectUpdateTable, ProjectUpdate, String](this)
	object likes extends SetColumn[ProjectUpdateTable, ProjectUpdate, String](this)

	override def fromRow(r : Row) = ProjectUpdate(
		projectId = project_id(r),
		author = author(r),
		timeSubmitted = time_submitted(r),
		timeEditted = time_editted(r),
		content = content(r),
		files = files(r),
		likes = likes(r).toSeq
	)
}

private object ProjectUpdateTable extends ProjectUpdateTable {
	override val tableName = "project_updates";
	implicit val session = CassieCommunicator.session

	def all : Future[Seq[ProjectUpdate]] = select.fetch()

	def allUninterruptibly : Seq[ProjectUpdate] = scala.concurrent.Await.result(all, constants.Cassandra.defaultTimeout)

	def get(projectId : Int, author : String, timeSubmitted : Date, timeEditted : Date) : Future[Option[ProjectUpdate]] = select
		.where(_.project_id eqs projectId)
		.and(_.author eqs author)
		.and(_.time_submitted eqs timeSubmitted)
		.and(_.time_editted eqs timeEditted)
		.one();

	def get(projectId : Int, author : String, timeSubmitted : Date) : Future[Seq[ProjectUpdate]] = select
		.where(_.project_id eqs projectId)
		.and(_.author eqs author)
		.and(_.time_submitted eqs timeSubmitted)
		.fetch();

	def get(projectId : Int) : Future[Seq[ProjectUpdate]] = select
		.where(_.project_id eqs projectId)
		.fetch();

	def getUninterruptibly(projectId : Int) : Seq[ProjectUpdate] = scala.concurrent.Await.result(get(projectId), constants.Cassandra.defaultTimeout)

	def add(update : ProjectUpdate) = insert.value(_.project_id, update.projectId)
		.value(_.author, update.author)
		.value(_.time_submitted, update.timeSubmitted)
		.value(_.time_editted, update.timeEditted)
		.value(_.files, update.files.toList)
		.value(_.content, update.content)
		.future();

	def edit(projectId : Int, author : String, timeSubmitted : Date, newContent : String) : ProjectUpdate = {
		val oldUpdate = ProjectUpdate.getLatest(projectId, author, timeSubmitted)
		val files = oldUpdate.files;

		val timeEditted = new Date()

		insert.value(_.project_id, projectId)
			.value(_.author, author)
			.value(_.time_submitted, timeSubmitted)
			.value(_.files, files.toList)
			.value(_.content, newContent)
			.value(_.time_editted, timeEditted)
			.future();

		ProjectUpdate(
			projectId = projectId,
			author = author,
			timeSubmitted = timeSubmitted,
			content = newContent,
			files = files,
			likes = oldUpdate.likes,
			timeEditted = timeEditted)
	}

	def addLike(username : String, projectId : Int, author : String, timeSubmitted : Date, timeEditted : Date) : Unit = {
		update
			.where(_.author eqs author)
			.and(_.project_id eqs projectId)
			.and(_.time_submitted eqs timeSubmitted)
			.and(_.time_editted eqs timeEditted)
			.modify(_.likes add username)
			.future();
	}

	def removeLike(username : String, projectId : Int, author : String, timeSubmitted : Date, timeEditted : Date) : Unit = {
		update
			.where(_.author eqs author)
			.and(_.project_id eqs projectId)
			.and(_.time_submitted eqs timeSubmitted)
			.and(_.time_editted eqs timeEditted)
			.modify(_.likes remove username)
			.future();
	}
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

	def all : Seq[ProjectUpdate] = ProjectUpdateTable.allUninterruptibly

	def get(user : User) : Seq[ProjectUpdate] = return CassieCommunicator.getUpdatesForUser(user.username);

	def get(projectId : Int, author : String, timeSubmitted : Date, timeEditted : Date) = scala.concurrent.Await.result(ProjectUpdateTable.get(projectId, author, timeSubmitted, timeEditted), constants.Cassandra.defaultTimeout)

	def get(projectId : Int, author : String, timeSubmitted : Date) = scala.concurrent.Await.result(ProjectUpdateTable.get(projectId, author, timeSubmitted), constants.Cassandra.defaultTimeout)

	def getLatest(projectId : Int, author : String, timeSubmitted :Date) : ProjectUpdate = {
		try {
			get(projectId, author, timeSubmitted).reduce((a, b) => {
				if(a.timeEditted.after(b.timeEditted)) {
					a
				}
				else {
					b
				}
			})
		}
		catch {
			case e : java.lang.UnsupportedOperationException => ProjectUpdate.undefined
		}

	}

	def create (content: String, author: String, projectId : Int, files : Seq[(String, TemporaryFile)]) : ProjectUpdate = {
		val timeSubmitted = new Date();

		val update = ProjectUpdate(
			content = content,
			author = author,
			projectId = projectId,
			files = files.map(temporaryFile => ProjectFile.saveFile(temporaryFile, author, projectId, timeSubmitted).filename),
			timeSubmitted = timeSubmitted,
			timeEditted = timeSubmitted
		)

		ProjectUpdateTable.add(update)

		//return CassieCommunicator.addUpdateForProject(update)
		return update;
	}

	def edit(projectId : Int, author : String, timeSubmitted : Date, newContent : String) : ProjectUpdate = {
		ProjectUpdateTable.edit(projectId, author, timeSubmitted, newContent);
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
					content = row.getString("content"),
					author = row.getString("author"),
					projectId = row.getInt("project_id"),
					timeSubmitted = row.getDate("time_submitted"),
					timeEditted = row.getDate("time_editted"),
					files = row.getList("files", classOf[String]),
					likes = row.getSet("likes", classOf[String]).toList
				)
			}
		}
	}

	def addLike (username : String, projectId : Int, author : String, timeSubmitted : Date) : Unit = {
		val latest = getLatest(projectId, author, timeSubmitted)
		ProjectUpdateTable.addLike(username, projectId, author, timeSubmitted, latest.timeEditted);
	}
	
	def removeLike(username : String, projectId : Int, author : String, timeSubmitted : Date) : Unit = {
		val latest = getLatest(projectId, author, timeSubmitted)
		ProjectUpdateTable.removeLike(username, projectId, author, timeSubmitted, latest.timeEditted);
	}
}

case class ProjectUpdate (
	content: String,
	author : String = "",
	projectId : Int = -1,
	timeSubmitted : Date = new Date(),
	timeEditted : Date = new Date(),
	files : Seq[String] = List[String](),
	likes : Seq[String] = List[String](),
	isDefined : Boolean = true) {
	def this (content : String, author : String, projectId :  Int, timeSubmitted : Date, timeEditted : Date) = this(
		content,
		author,
		projectId,
		timeSubmitted,
		timeEditted,
		List[String](),
		List[String](),
		true
	)


	implicit def toJson() : JsObject = { 
		return JsObject(
				Seq(
					"content" -> JsString(content), 
					"author" -> JsString(author), 
					"projectId" -> JsNumber(projectId),
					"timeSubmitted" -> JsString(utils.Conversions.dateToStr(timeSubmitted)),
					"timeEditted" -> JsString(utils.Conversions.dateToStr(timeEditted)),
					"files" -> Json.toJson(files)
			)
		)
	}

	def delete () { ProjectUpdate.delete(this) }
}