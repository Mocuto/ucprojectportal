package controllers

import actors.masters.{ActivityMaster, IndexerMaster}

import com.codahale.metrics.Counter
import com.kenshoo.play.metrics.MetricsRegistry

import enums.ActivityType
import enums.ActivityType._

import java.util.Date

import model._
import model.UserPrivileges

import org.joda.time._

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

	val projectsCreatedCounter = MetricsRegistry.defaultRegistry.counter("projects.created")

	def project(id : Int) = Action { implicit request =>
		whenAuthorized(username => {
			
			val updates = Project.getUpdates(id);
			val viewingPermissions = (UserPrivilegesView getUninterruptibly username).getOrElse { UserPrivilegesView.undefined(username)};
			val editPermissions = UserPrivilegesEdit.getUninterruptibly(username).getOrElse { UserPrivilegesEdit.undefined(username)};
			val createPermissions = UserPrivilegesCreate.getUninterruptibly(username).getOrElse { UserPrivilegesCreate.undefined(username)};
			val deletePermissions = UserPrivilegesDelete.getUninterruptibly(username).getOrElse { UserPrivilegesDelete.undefined(username)};
		
			val project = Project.get(id);

			val mostRecentUpdates = (
				updates.groupBy(u => (u.projectId, u.author, u.timeSubmitted))
					.map({ 
						case (key, sublist) => sublist reduce { 
							(a, b) =>
								if( a.timeEditted.after(b.timeEditted)) {
									a
								}
								else {
									b
								}
							}
					})
			).toSeq.sortWith((a,b) => a.timeSubmitted.after(b.timeSubmitted));

			if (viewingPermissions.projects == false) {
				NotFound(views.html.messages.notFound("You do not have permission to view this project"))
			}

			else if(project.isDefined == false) {
				NotFound(views.html.messages.notFound("This project does not exist"));
			}
			else {
				val canEdit = editPermissions.projectsAll || (editPermissions.projectsOwn && project.primaryContact == username);
				val canEditAllUpdates = editPermissions.updatesAll;
				val canEditOwnUpdates = editPermissions.updatesOwn;
				val canJoin = editPermissions.joinProjects;

				val canUpdate = createPermissions.updatesAllProjects || (createPermissions.updatesTheirProjects && project.teamMembers.contains(username))
				val canDeleteAllUpdates = deletePermissions.updatesAll
				val canDeleteOwnUpdates = deletePermissions.updatesOwn

				ActivityMaster.logProjectActivity(username, id, ActivityType.ViewProject);

				Ok(views.html.project(
					project,
					mostRecentUpdates,
					username,
					canEdit,
					canUpdate,
					canJoin,
					canEditAllUpdates,
					canEditOwnUpdates,
					canDeleteAllUpdates,
					canDeleteOwnUpdates)(None)(ProjectUpdateController.projectUpdateForm))
			}
		})
	}

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
		whenAuthorized(username => {
			projectForm.bindFromRequest.fold(
				formWithErrors => {
					BadRequest(views.html.newProject(User.get(username))(formWithErrors))
				},
				incompleteProject => {
					val createPermissions = UserPrivilegesCreate.getUninterruptibly(username).getOrElse { UserPrivilegesCreate.undefined(username)}
					if(createPermissions.projects == false) {
						Status(462)("You do not have permission to create projects");
					}
					else {
						val completeProject = 
							(incompleteProject.state, incompleteProject.stateMessage) match {
								case (ProjectState.IN_PROGRESS_NEEDS_HELP, stateMessage) => Project.create(
									incompleteProject.name,
									incompleteProject.description,
									username,
									incompleteProject.categories,
									ProjectState.IN_PROGRESS_NEEDS_HELP,
									incompleteProject.stateMessage, 
									incompleteProject.teamMembers);

								case (state, _) => Project.create(
									incompleteProject.name,
									incompleteProject.description,
									username,
									incompleteProject.categories,
									incompleteProject.state,
									"",
									incompleteProject.teamMembers);
						}
						projectsCreatedCounter.inc();

						IndexerMaster.index(completeProject)

						ActivityMaster.logProjectActivity(username, completeProject.id, ActivityType.SubmitProject);

						Redirect(routes.ProjectController.project(completeProject.id));							
					}
			})
		})
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
						timeFinished = if(isFinished) Some(new Date()) else None
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

					IndexerMaster.index(updatedProject);

					ActivityMaster.logProjectActivity(username, id, ActivityType.EditProject);

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

					IndexerMaster index project

					ActivityMaster.logProjectActivity(username, id, ActivityType.LeaveProject);

					Ok(response);
				}
			}
		}
	}

	def like(id : Int) = Action { implicit request =>
		whenAuthorized(username => {
			if(!Project.get(id).isDefined) {
				NotFound("This project does not exist")
			}
			else {
				Project.addLike(id, username);

				val response = JsObject( 
					Seq(
						"response" -> JsString("liked project")
					)
				)

				ActivityMaster.logProjectActivity(username, id, ActivityType.LikeProject);

				Notification.createProjectLiked(User.get(username), Project.get(id))

				Ok(response);
			}
		})
	}

	def unlike(id : Int) = Action { implicit request =>
		whenAuthorized(username => {
			if(!Project.get(id).isDefined) {
				NotFound("This project does not exist")
			}
			Project.removeLike(id, username);

			val response = JsObject( 
				Seq(
					"response" -> JsString("unliked project")
				)
			)

			ActivityMaster.logProjectActivity(username, id, ActivityType.UnlikeProject)

			Ok(response);
		})
	}

	def follow(id : Int) = Action { implicit request =>
		whenAuthorized(username => {
			val followPermissions = UserPrivilegesFollow.getUninterruptibly(username).getOrElse {UserPrivilegesFollow.undefined(username)}

			if(!followPermissions.projectsAll) {
				Status(401)("You are not authorized to follow projects")
			}
			else if(!Project.get(id).isDefined) {
				NotFound("This project does not exist")
			}
			Project.addFollower(id, username);

			val response = JsObject( 
				Seq(
					"response" -> JsString("followed project")
				)
			)

			ActivityMaster.logProjectActivity(username, id, ActivityType.FollowProject)

			Ok(response);
		})
	}

	def unfollow(id : Int) = Action { implicit request =>
		whenAuthorized(username => {
			val followPermissions = UserPrivilegesFollow.getUninterruptibly(username).getOrElse {UserPrivilegesFollow.undefined(username)}

			if(!followPermissions.projectsAll) {
				Status(401)("You are not authorized to unfollow projects")
			}
			else if(!Project.get(id).isDefined) {
				NotFound("This project does not exist")
			}
			Project.removeFollower(id, username);

			val response = JsObject( 
				Seq(
					"response" -> JsString("unfollowed project")
				)
			)

			ActivityMaster.logProjectActivity(username, id, ActivityType.UnfollowProject)

			Ok(response);
		})
	}

	def jsonForAll = Action { implicit request =>
		whenAuthorized(username => {
			val response = Json.toJson((Project all) map(_.name))

			Ok(response)
		})
	}

	def jsonForUser = Action { implicit request =>
		whenAuthorized(username => {
			val response = Json.toJson((Project get username) map (_.name)) 

			Ok(response);
		})
	}
}