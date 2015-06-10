package controllers

import com.codahale.metrics.Counter
import com.kenshoo.play.metrics.MetricsRegistry
import com.typesafe.plugin._

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

object ProjectController extends Controller with SessionHandler {

	implicit val projectForm = Form(
		mapping(
			"name" -> nonEmptyText,
			"description" -> nonEmptyText,
			"categories" -> list(nonEmptyText),
			"state" -> nonEmptyText,
			"state-message" -> text,
			"team-members" -> list(nonEmptyText)
		) (Project.apply)(Project.unapplyIncomplete).verifying("at least one category is needed", fields => fields match {
			case project => {  project.categories.length > 0}
		})
	)

	val projectsCreatedCounter = MetricsRegistry.default.counter("projects.created")

	def project(id : Int) = Action { implicit request => {
		authenticated match {
			case Some(username) => {

				val project = Project.get(id);

				val viewingPermissions = UserPrivilegesView.getUninterruptibly(username).getOrElse { UserPrivilegesView.undefined(username)}

				if (viewingPermissions.projects == false) {
					NotFound(views.html.messages.notFound("You do not have permission to view this project"))
				}

				else if(project.isDefined == false) {
					NotFound(views.html.messages.notFound("This project does not exist"));
				}
				else {
					val updates = Project.getUpdates(id);

					val editPermissions = UserPrivilegesEdit.getUninterruptibly(username).getOrElse { UserPrivilegesEdit.undefined(username) }

					val canEdit = editPermissions.projectsAll || (editPermissions.projectsOwn && project.primaryContact == username);
					val canJoin = editPermissions.joinProjects;

					val createPermissions = UserPrivilegesCreate.getUninterruptibly(username).getOrElse { UserPrivilegesCreate.undefined(username)}
					val canUpdate = createPermissions.updatesAllProjects || (createPermissions.updatesTheirProjects && project.teamMembers.contains(username))

					Ok(views.html.project(project, updates, username, canEdit, canUpdate, canJoin)(None)(ProjectUpdateController.projectUpdateForm))
				}
			}
		}

	}}

	def create = Action { implicit request => {
		authenticated match {
			case Some(username) => { 

				val createPermissions = UserPrivilegesCreate.getUninterruptibly(username).getOrElse { UserPrivilegesCreate.undefined(username)}
				if(createPermissions.projects == false) {
					Status(462)(views.html.messages.notFound("You do not have permission to create projects"));
				}
				else {
					Ok(views.html.newProject(User.get(username))); 					
				}


			}
		}
	}}

	def submit = Action { implicit request =>
		authenticated match {
			case Some(username) => {
				projectForm.bindFromRequest.fold(
					formWithErrors => {
						BadRequest(views.html.newProject(User.get(username))(formWithErrors))
					},
					incompleteProject => incompleteProject match {
						case Project(
							_,
							name,
							description,
							timeStarted,
							timeFinished,
							categories,
							_,
							_,
							teamMembers,
							state,
							stateMessage,
							_) => {
						val createPermissions = UserPrivilegesCreate.getUninterruptibly(username).getOrElse { UserPrivilegesCreate.undefined(username)}
						if(createPermissions.projects == false) {
							Status(462)("You do not have permission to create projects");
						}
						else {
							val completeProject = 
								(state, stateMessage) match {
									case (ProjectState.IN_PROGRESS_NEEDS_HELP, stateMessage) => Project.create(name, description, username, categories, ProjectState.IN_PROGRESS_NEEDS_HELP, stateMessage, teamMembers);
									case (state, _) => Project.create(name, description, username, categories, state, "", teamMembers);
							}
							projectsCreatedCounter.inc();
							Redirect(routes.ProjectController.project(completeProject.id));							
						}
					}
				})
			}
		}

	}

	def edit(id : Int) = Action { implicit request =>
		authenticated match {
			case Some(username) => {
				val project = Project.get(id);

				val editPermissions = UserPrivilegesEdit.getUninterruptibly(username).getOrElse { UserPrivilegesEdit.undefined(username) }

				val canEdit = editPermissions.projectsAll || (editPermissions.projectsOwn && project.primaryContact == username);

				if(canEdit == false) 
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

					println(dataParts);

					val state = dataParts.getOrElse("state", List(project.state))(0)

					val isFinished = (state == ProjectState.COMPLETED || state == ProjectState.CLOSED);

					val updatedProject = Project(
						id = project.id,
						name = dataParts.getOrElse("name", List(project.name))(0),
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

	def leave(id : Int) = Action { implicit request =>
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