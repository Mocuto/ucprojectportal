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

object AdminController extends Controller with SessionHandler {

	implicit val newUserForm = Form(
		single(
			"usernames" -> nonEmptyText
		)
	)

	def admin = Action { implicit request =>
		authenticated match {
			case Some(username) => {
				val authenticatedUser = User.get(username);

				Ok(views.html.admin(authenticatedUser));
			}
		}
	}

	def createUser = Action { implicit request => 
		authenticated match {
			case Some(username) => {
				newUserForm.bindFromRequest.fold(
					formWithErrors => {
						BadRequest(views.html.messages.prettyMessage(play.twirl.api.Html("error adding users!")))

					}, newUserData => {
						newUserData match {
							case usernamesString : String => {
								usernamesString.split(Array[Char](',', '\n')).map(username => username.trim.toLowerCase()).foreach(username => User.create(User(username)))
								Redirect(routes.AdminController.admin);
							}
						}
					}
				)
			}
		}
	}

	def deleteUser(username : String) = Action { implicit request =>
		authenticated match {
			case Some(authenticatedUsername) => {
				User.delete(username);
				Redirect(routes.AdminController.admin);
			}
		}
	}

	def deleteProject(id : Int) = Action { implicit request =>
		authenticated match {
			case Some(username) => {
				Project.delete(id);
				Redirect(routes.AdminController.admin);
			}
		}
	}
}