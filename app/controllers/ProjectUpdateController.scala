package controllers

import actors.masters.ActivityMaster

import com.codahale.metrics.Counter
import com.kenshoo.play.metrics.MetricsRegistry

import java.util.Date

import model._
import model.UserPrivileges

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.libs.json._

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

import utils._
import utils.Conversions._

object ProjectUpdateController extends Controller with SessionHandler {

	private[controllers] val projectUpdateForm = Form(
		mapping(
			"content" -> nonEmptyText,
			"project_id" -> number,
			"date" -> ignored(new Date())
		) (ProjectUpdate.applyIncomplete)(ProjectUpdate.unapplyIncomplete)
	)

	val updatesCreatedCounter = MetricsRegistry.defaultRegistry.counter("projects.created")

	def submit(projectId : Int) = Action { implicit request =>
		whenAuthorized(username => {
			projectUpdateForm.bindFromRequest.fold(
			  formWithErrors => {
			    // binding failure, you retrieve the form containing errors:
			    var errorMessage : String = "";
			    formWithErrors.errors map { error  => {
			    		errorMessage = s"$errorMessage ${error.key}";
			    	}
			    }
				BadRequest(errorMessage);
			  },
			  update => {
			    /* binding success, you get the actual value. */
			    val project = Project.get(projectId);

				val createPermissions = UserPrivilegesCreate.getUninterruptibly(username).getOrElse { UserPrivilegesCreate.undefined(username)}
				val canUpdate = createPermissions.updatesAllProjects || (createPermissions.updatesTheirProjects && project.teamMembers.contains(username))

			    if(canUpdate == false) {
			    	Status(462)("You are not a member of this project");
			    }
			    else if(project.isDefined == false) {
			    	Status(404)("This project does not exist")
			    }
			    else {
				    val multipartFormData = request.body.asMultipartFormData.get
				    val files = multipartFormData.files.map(filepart => (filepart.filename, filepart.ref));

				    val completeUpdate = ProjectUpdate.create(update.content, username, projectId, files = files);

				    Future {
				    	project.notifyFollowersAndMembersExcluding(username, completeUpdate.content);
				    	
					}

					val editPermissions = UserPrivilegesEdit.getUninterruptibly(username).getOrElse { UserPrivilegesEdit.undefined(username) }
					val canEdit = editPermissions.updatesAll || editPermissions.updatesOwn && completeUpdate.author == username

					val deletePermissions = UserPrivilegesDelete.getUninterruptibly(username).getOrElse { UserPrivilegesDelete.undefined(username)}
					val canDelete = deletePermissions.updatesAll || deletePermissions.updatesOwn && completeUpdate.author == username

				    val response = JsObject(
				    	Seq(
			    			"html" -> JsString(views.html.common.updateView(completeUpdate, username, canEdit, canDelete).toString),
			    			"fileHtml" -> JsString(
			    				completeUpdate.files.map(x => views.html.common.fileUpdateView(ProjectFile.get(projectId, completeUpdate.timeSubmitted, x)).toString).mkString
		    				)
			    		)
				    )

				    updatesCreatedCounter.inc();

				    ActivityMaster.logSubmitUpdate(username, projectId, completeUpdate.timeSubmitted, update.content)

					Future {
						ActivityMaster.startRankingActivity();
					}

				    Ok(response);
			    }
			})
		})
	}

	def edit(projectId : Int, author : String, timeSubmittedStr : String) = Action { implicit request =>
		whenAuthorized(username => {

			val editPermissions = UserPrivilegesEdit.getUninterruptibly(username).getOrElse { UserPrivilegesEdit.undefined(username) }

			val canEdit = editPermissions.updatesAll || editPermissions.updatesOwn && author == username;

			val timeSubmitted = utils.Conversions.strToDate(timeSubmittedStr)
			

			if(canEdit == false) {
				Status(401)("You cannot edit updates you did not create")
			}
			else if(!ProjectUpdate.getLatest(projectId, author, timeSubmitted).isDefined) {
				NotFound("This update does not exist")
			}
			else {
				val dataParts = request.body.asMultipartFormData.get.dataParts

				val newContent : String = dataParts.getOrElse("content", List(""))(0)

				val update = ProjectUpdate.edit(projectId, author, timeSubmitted, newContent);

				val response = JsObject( 
					Seq(
						"response" -> JsString("project update edited")
					)
				)

				ActivityMaster.logEditUpdate(username, projectId, author, timeSubmitted, update.timeEditted)

				Ok(response);
			}

		})
	}

	def delete(projectId : Int, author : String, timeSubmittedStr : String) = Action { implicit request =>
		whenAuthorized(username => {
			val deletePermissions = UserPrivilegesDelete.getUninterruptibly(username).getOrElse { UserPrivilegesDelete.undefined(username)}
			val canDelete = deletePermissions.updatesAll || (deletePermissions.updatesOwn && author == username)

			val timeSubmitted = utils.Conversions.strToDate(timeSubmittedStr)

			if(canDelete == false) {
				Status(462)("You cannot delete updates you did not create.")
			}
			else if(!ProjectUpdate.getLatest(projectId, author, timeSubmitted).isDefined) {
				NotFound("This update does not exist")
			}
			else {
				ProjectUpdate.get(projectId, author, timeSubmittedStr).foreach(_.delete())

				val response = JsObject(Seq("response" -> JsString("left project")))

				ActivityMaster.logDeleteUpdate(username, projectId, author, utils.Conversions.strToDate(timeSubmittedStr))

				Future {
					ActivityMaster.startRankingActivity();
				}


				Ok(response)
			}
		})
	}

	def like(projectId : Int, author : String, timeSubmittedStr : String) = Action { implicit request =>
		whenAuthorized(username => {
			val timeSubmitted = utils.Conversions.strToDate(timeSubmittedStr)

			if(!ProjectUpdate.getLatest(projectId, author, timeSubmitted).isDefined) {
				NotFound("This update does not exist")
			}
			else {

				ProjectUpdate.addLike(username, projectId, author, timeSubmitted);

				val response = JsObject( 
					Seq(
						"response" -> JsString("liked update")
					)
				)

				ActivityMaster.logLikeUpdate(username, projectId, author, timeSubmitted);

				Notification.createUpdateLiked(User.get(username), ProjectUpdate.getLatest(projectId, author, timeSubmitted))

				Ok(response);
			}

		})
	}

	def unlike(projectId : Int, author : String, timeSubmittedStr : String) = Action { implicit request =>
		whenAuthorized(username => {
			val timeSubmitted = utils.Conversions.strToDate(timeSubmittedStr)
			
			if(!ProjectUpdate.getLatest(projectId, author, timeSubmitted).isDefined) {
				NotFound("This update does not exist")
			}
			else {

				ProjectUpdate.removeLike(username, projectId, author, timeSubmitted);

				val response = JsObject( 
					Seq(
						"response" -> JsString("unliked update")
					)
				)

				ActivityMaster.logUnlikeUpdate(username, projectId, author, timeSubmitted);

				Ok(response);
			}
		})
	}
}