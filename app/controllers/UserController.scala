package controllers

import actors.masters.ActivityMaster

import java.util.Date

import model._
import model.UserPrivileges
import model.form.Forms._

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

	def user(username : String) = Action { implicit request => 
		whenAuthorized (authenticatedUsername => {
	
			val user = User.get(username)

			val viewPermissions = UserPrivilegesView.getUninterruptibly(authenticatedUsername).getOrElse { UserPrivileges.View(authenticatedUsername, false, false, false, false, false) };
			val isModerator = viewPermissions.moderator;

			implicit val userPrivilegeSet = UserPrivileges.get(username);
			implicit val authenticatedPrivilegeSet = UserPrivileges.get(authenticatedUsername);

			val canEdit = authenticatedPrivilegeSet.edit.userPermissions;

			if(viewPermissions.users == false && username != authenticatedUsername) {
				NotFound(views.html.messages.notFound("You do not have permission to view this user"));
			}
			else if (user.isDefined == false || user.hasConfirmed == false) {
				NotFound(views.html.messages.notFound("This user does not exist"));
			}
			else {
				val loggedInUser = User.get(authenticatedUsername);

				ActivityMaster.logViewUser(viewer = authenticatedUsername, viewee = username)

				val filledForm = ModerationController.verifyUserForm.fill(model.form.Forms.UserForm(user.firstName, user.lastName, user.preferredPronouns, user.position, user.officeHourRequirement))

				Ok(views.html.user(user, loggedInUser)(username == authenticatedUsername, canEdit)(userPrivilegeSet, authenticatedPrivilegeSet)(filledForm));
			}
		})
	}

	def profilePic(username : String) = Action { implicit request =>
		whenAuthorized(authUsername => {

			val editPermissions = UserPrivilegesEdit.getUninterruptibly(authUsername).getOrElse { UserPrivilegesEdit.undefined(authUsername) }
			val canEdit = username == authUsername || editPermissions.userPermissions;

			if(!canEdit) {
				Status(404)("You do not have permission to edit this user's profile.")
			}
			else if(!User.get(username).isDefined) {
				NotFound("This user does not exist");
			}
			else {
				val multipartFormData = request.body.asMultipartFormData.get
				val files = multipartFormData.files.map(filepart => (filepart.filename, filepart.ref));

				if(files.length > 0) {
					val file = files(0)

					val filename = User.setProfilePic(username, file)

				    val response = JsObject(
				    	Seq(
			    			"path" -> JsString(filename)
			    		)
				    )

				    Ok(response);
				}
				else {
				    BadRequest("A file is needed.")
				}
			}
		})
	}

	def follow(follower : String, toFollow : String) = Action { implicit request =>
		whenAuthorized(authUsername => {
			val editPermissions = UserPrivilegesEdit.getUninterruptibly(authUsername).getOrElse { UserPrivilegesEdit.undefined(authUsername) }
			val followPermissions = UserPrivilegesFollow.getUninterruptibly(authUsername).getOrElse {UserPrivilegesFollow.undefined(authUsername)}
			val canChange = (follower == authUsername && followPermissions.usersAll)  || (follower != authUsername && editPermissions.userPermissions)

			if(!canChange) {
				Status(401)("You do not have privileges necessary to change the following settings for this user.")
			}
			else if(!User.get(follower).isDefined) {
				NotFound(s"follower $follower does not exist")
			}
			else if(!User.get(toFollow).isDefined) {
				NotFound(s"toFollow $toFollow does not exist")
			}
			else {
				User.addFollower(toFollow, follower)

				val response = JsObject( 
					Seq(
						"response" -> JsString("followed user")
					)
				)

				ActivityMaster.logFollowUser(follower, toFollow)

				Ok(response);
			}
		})
	}

	def unfollow(follower : String, toFollow : String) = Action { implicit request =>
		whenAuthorized(authUsername => {
			val editPermissions = UserPrivilegesEdit.getUninterruptibly(authUsername).getOrElse { UserPrivilegesEdit.undefined(authUsername) }
			val followPermissions = UserPrivilegesFollow.getUninterruptibly(authUsername).getOrElse {UserPrivilegesFollow.undefined(authUsername)}
			val canChange = (follower == authUsername && followPermissions.usersAll)  || (follower != authUsername && editPermissions.userPermissions)

			if(!canChange) {
				Status(401)("You do not have privileges necessary to change the following settings for this user.")
			}
			else if(!User.get(follower).isDefined) {
				NotFound(s"follower $follower does not exist")
			}
			else if(!User.get(toFollow).isDefined) {
				NotFound(s"toFollow $toFollow does not exist")
			}
			else {
				User.removeFollower(toFollow, follower)

				val response = JsObject( 
					Seq(
						"response" -> JsString("unfollowed user")
					)
				)

				ActivityMaster.logUnfollowUser(follower, toFollow)

				Ok(response);
			}
		})
	}
}