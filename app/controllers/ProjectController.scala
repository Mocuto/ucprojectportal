package controllers

import com.typesafe.plugin._

import java.util.Date

import model._

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.libs.json._

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

import utils._

object ProjectController extends Controller with SessionHandler {
	private val projectUpdateForm = Form(
		mapping(
			"content" -> nonEmptyText,
			"project_id" -> number,
			"date" -> ignored(new Date())
		) (ProjectUpdate.applyIncomplete)(ProjectUpdate.unapplyIncomplete)
	)

	implicit val projectForm = Form(
		mapping(
			"name" -> nonEmptyText,
			"description" -> nonEmptyText,
			"categories" -> list(nonEmptyText),
			"team-members" -> list(nonEmptyText)
		) (Project.apply)(Project.unapplyIncomplete).verifying("Insert invalid project categories message here", fields => fields match {
			case project => { println("project.categories.length: " + project.categories.length);  project.categories.length > 0}
		})
	)


	def project(id : Int) = Action { implicit request => {
		authenticated match {
			case Some(username) => {

				val project = Project.get(id);
				if(project.isDefined == false) {
					NotFound(views.html.notFound("This project does not exist"));
				}
				else {
					val updates = CassieCommunicator.getStatusesForProject(id);

					val isPrimaryContact = project.primaryContact == username;

					Ok(views.html.project(project, updates, username, isPrimaryContact)(None)(projectUpdateForm))
				}
			}
		}

	}}

	def newProject = Action { implicit request => {
		authenticated match {
			case Some(username) => Ok(views.html.newProject(User.get(username)));
		}
	}}

	def submitUpdate = Action { implicit request =>
		authenticated match {
			case Some(username) => {
				projectUpdateForm.bindFromRequest.fold(
				  formWithErrors => {
				    // binding failure, you retrieve the form containing errors:
				    //BadRequest(views.html.user(formWithErrors))
				    var errorMessage : String = "";
				    formWithErrors.errors map { error  => {
				    		errorMessage = s"$errorMessage ${error.key}";
				    	}
				    }
					BadRequest(errorMessage);
				  },
				  update => {
				    /* binding success, you get the actual value. */
				    val project = Project.get(update.projectId);

				    if(project.teamMembers.contains(username) == false) {
				    	Status(462)("You are not a member of this project");
				    }
				    else if(project.isDefined == false) {
				    	Status(404)("This project does not exist")
				    }
				    else {
					    val multipartFormData = request.body.asMultipartFormData.get
					    val files = multipartFormData.files.map(filepart => (filepart.filename, filepart.ref));

					    val completeUpdate = ProjectUpdate.create(update.content, username, update.projectId, files = files);

					    Future {
					    	project.notifyMembersExcluding(username, completeUpdate.content);
						}

					    val response = JsObject(
					    	Seq(
				    			"html" -> JsString(views.html.updateView(completeUpdate).toString)
					    	)
					    )

					    Ok(response);
				    }
				  }
				)
			}
		}
	}

	def submitProject = Action { implicit request =>
		authenticated match {
			case Some(username) => {
				projectForm.bindFromRequest.fold(
					formWithErrors => {
						BadRequest(views.html.newProject(User.get(username))(formWithErrors))
					},
					incompleteProject => {
						var completeProject = Project.create(incompleteProject.name, incompleteProject.description, username,
							incompleteProject.categories, incompleteProject.teamMembers);
						Redirect(routes.ProjectController.project(completeProject.id));
					}
				)
			}
		}

	}

	def editProject(id : Int) = Action { implicit request =>
		authenticated match {
			case Some(username) => {
				val project = Project.get(id);

				if(project.primaryContact != username && !UserGroup.isAdmin(username)) 
				{
					Status(401)("You are not authorized to edit this project");
				}
				else if(request.body.asMultipartFormData == None) 
				{
					Status(400)("No data found");
				}
				else if(project.isDefined == false) {
					Status(400)("This project does not exist")
				}
				else 
				{
					val dataParts = request.body.asMultipartFormData.get.dataParts

					val state = dataParts.getOrElse("state", List(project.state))(0)

					val isFinished = (state == ProjectState.COMPLETED || state == ProjectState.CLOSED);

					val updatedProject = Project(
						id = project.id,
						name = project.name,
						description = dataParts.getOrElse("description", List(project.description))(0),
						categories = dataParts.getOrElse("categories", project.categories),
						state = state,
						stateMessage = dataParts.getOrElse("state-message", List(project.stateMessage))(0),
						teamMembers = dataParts.getOrElse("team-members", project.teamMembers),
						primaryContact = dataParts.getOrElse("primary-contact", List(project.primaryContact))(0),
						timeFinished = if(isFinished) new Date() else null
					)

					Project.update(updatedProject)

					for(otherUsername <- updatedProject.teamMembers ++ project.teamMembers) {
						val user = User.get(otherUsername);
						if(project.teamMembers.contains(otherUsername) == false) {
							Project.addUser(id, user);
						}
						else if(updatedProject.teamMembers.contains(otherUsername) == false && otherUsername != updatedProject.primaryContact) {
							Project.removeUser(id, user);
						}
					}

					if(updatedProject.primaryContact != project.primaryContact) {
						Project.changePrimaryContact(id, User.get(project.primaryContact), User.get(updatedProject.primaryContact))
					}

					val response = JsObject( 
						Seq(
							"response" -> JsString("project edited")
						)
					)

					Ok(response);
				}
			}
		}
	}

	def leaveProject(id : Int) = Action { implicit request =>
		authenticated match {
			case Some(username) => {
				val project = Project.get(id);

				if(project.teamMembers.contains(username) == false) {
					Status(404)("You are not on this project team")
				}
				else if(project.isDefined == false) {
					Status(400)("This project does not exist")
				}
				else {
					val authenticatedUser = User.get(username)

					Project.removeUser(id, authenticatedUser);
					
					val response = JsObject( 
						Seq(
							"response" -> JsString("left project")
						)
					)

					Ok(response);
				}
			}
		}

	}
}