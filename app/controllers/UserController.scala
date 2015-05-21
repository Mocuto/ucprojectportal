package controllers

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

object UserController extends Controller with SessionHandler {

	def user(username : String) = Action { implicit request => {
		authenticated match {
			case Some(authenticatedUsername) => {
				val user = User.get(username)

				val viewPermissions = UserPrivilegesView.getUninterruptibly(authenticatedUsername).getOrElse { UserPrivileges.View(authenticatedUsername, false, false, false, false, false) };

				if(viewPermissions.users == false && username != authenticatedUsername) {
					NotFound(views.html.messages.notFound("You do not have permission to view this user"));
				}
				else if (user.isDefined == false || user.hasConfirmed == false) {
					NotFound(views.html.messages.notFound("This user does not exist"));
				} else {
					val loggedInUser = User.get(authenticatedUsername);
					Ok(views.html.user(user, loggedInUser)(username == authenticatedUsername));
				}
			}
		}
	}}
}