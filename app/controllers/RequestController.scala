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

object RequestController extends Controller with SessionHandler {

	def requestJoin(projectId : Int) = Action { implicit request =>
		authenticated match {
			case Some(username) => {
				val project = Project.get(projectId);
				val receiver = User.get(project.primaryContact);
				val authenticatedUser = User.get(username);

				project match{
					case project if project.isDefined == false => { //Check if the project with that id does not exist
						Status(404)(s"project with id = $projectId does not exist")
					}
					case project if authenticatedUser.projects.contains(project) => { //Check if they're already in that project
						Status(460)(s"already member of project with id = $projectId")
					}
					case project if project.state == "completed" => {
						Status(460)(s"cannot join a completed project")
					}
					case _ => {

						val success = Notification.createRequest(receiver, authenticatedUser, project);

						if (success == false) {
							Status(461)(s"project request already sent")
						}
						else {
							val response = JsObject(
								Seq(
									"response" -> JsString("your request has been sent")
								)
							)

							Ok(response);
						}
						
					}
				}
			}
		}
	}

	def acceptRequest(projectId : Int, requester : String)  = Action{ implicit request =>
		authenticated match {
			case Some(username) => {
				val projectRequest = ProjectRequest.get(projectId, username, requester);
				
				if (projectRequest.isDefined == false) {
					println(404)
					Status(404)("request does not exist");
				}
				else {
					val project = Project.get(projectId);
					if(project.isDefined == false) {
						println(404)
						NotFound("project does not exist");
					}
					else {
						projectRequest.accept();

						val response = JsObject(
							Seq(
								"response" -> JsString("request accepted")
							)
						)

						val user = User.get(requester);
						print("Ok");

						Ok(response)				
					}
				}
			}
		}
	}

	def ignoreRequest(projectId : Int, requester : String)  = Action{ implicit request =>
		authenticated match {
			case Some(username) => {
				val projectRequest = ProjectRequest.get(projectId, username, requester);
				
				if (projectRequest.isDefined == false) {
					Status(404)("request does not exist");
				}
				else {
					projectRequest.ignore();

					val response = JsObject(
						Seq(
							"response" -> JsString("request ignored")
						)
					)

					Ok(response)
				}
			}
		}
	}

	def decideRequest(projectId : Int, requester : String, doesAccept : Boolean) = Action { implicit request => {
		val requesterFirstName = User.get(requester).firstName;
		if (doesAccept) {
			acceptRequest(projectId, requester)(request);
			Redirect(routes.ProjectController.project(projectId))
		}
		else {
			ignoreRequest(projectId, requester)(request);
			Redirect(routes.ProjectController.project(projectId))
		}
	}}

}