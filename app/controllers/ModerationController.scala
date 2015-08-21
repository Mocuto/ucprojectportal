package controllers

import java.util.Date

import model._
import model.form.Forms._

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.data.validation.Constraints._
import play.api.libs.json._

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

import utils._

object ModerationController extends Controller with SessionHandler {


	protected[controllers] val verifyUserForm = Form(
		mapping(
			"first_name" -> nonEmptyText,
			"last_name" -> nonEmptyText,
			"preferred_pronouns" -> nonEmptyText,
			"position" -> nonEmptyText,
			"office_hour_requirement" -> of(doubleFormat)
		)(UserForm.apply)(UserForm.unapply) verifying("Cannot have negative office hour requirement", _.officeHourRequirement >= 0)
	)

	def moderation = Action { implicit request =>
		authenticated match {
			case Some(username) => {
				val authenticatedUser = User.get(username);

				Ok(views.html.moderation(authenticatedUser));
			}
		}
	}

	def editUserFollowing(username : String) = Action { implicit request =>
		whenAuthorized(authUsername => {

			val canEdit = (UserPrivilegesEdit.getUninterruptibly(authUsername).getOrElse { UserPrivilegesEdit.undefined(authUsername) }).userPermissions
			if(!canEdit) {
				Status(404)("You do not have permission to edit user permissions");
			}
			else {
				val dataParts = request.body.asMultipartFormData.get.dataParts

				val users : Set[String] = dataParts.getOrElse("users-following", List()).toSet

				val followPrivileges = UserPrivilegesFollow.getUninterruptibly(username).getOrElse { UserPrivilegesFollow.undefined(username) }

				User.get(username).usersFollowing.foreach(User.removeFollower(_, username))

				val follow = UserPrivileges.Follow(username, usersAll = followPrivileges.usersAll, projectsAll = followPrivileges.projectsAll)

				UserPrivilegesFollow.replace(username, follow)

				users.foreach(User.addFollower(_, username))

				val response = JsObject( 
					Seq(
						"response" -> JsString("project update edited")
					)
				)

				Ok(response);
			}
		})
	}

	def editUserPrivileges(username : String) = Action { implicit request =>
		whenAuthorized(authUsername => {

			import constants.Ajax._

			val userPrivileges = UserPrivileges.get(username)
			val authPrivileges = UserPrivileges.get(authUsername);

			implicit val authView = authPrivileges.view;
			implicit val authCreate = authPrivileges.create;
			implicit val authEdit = authPrivileges.edit;
			implicit val authFollow = authPrivileges.follow;
			implicit val authDelete = authPrivileges.delete;

			def editPrivilege[A <: UserPrivileges[A]](privilege : A, property : A, on : Boolean)(implicit editorPrivilege : A) : A = {
				if( !((editorPrivilege & property).isEmpty)) { //The editor should have the privilege that they're trying to edit on other users
					return (privilege & !property) | (if (on) property else (property & !property))
				}
				else {
					Logger.error(s"$authUsername does not have the privileges to edit the following privilege:")
					Logger.error(s"$username - $privilege - $property - $editorPrivilege")
					Logger.error(s"Returning existing privilege value ($privilege)")
					return privilege
				}
			}

			def setBooleanPrivilege(n : String, v : Boolean) : Unit = {
				n match {
					case ViewProjects => UserPrivilegesView.replace(username, editPrivilege(userPrivileges.view, UserPrivileges.View("", projects = true), v))

					case ViewUsers => UserPrivilegesView.replace(username, editPrivilege(userPrivileges.view, UserPrivileges.View("", users = true), v))

					case ViewAccountability => UserPrivilegesView.replace(username, editPrivilege(userPrivileges.view, UserPrivileges.View("", accountability = true), v))

					case ViewModerator => UserPrivilegesView.replace(username, editPrivilege(userPrivileges.view, UserPrivileges.View("", moderator = true), v))

					case ViewAdmin => UserPrivilegesView.replace(username, editPrivilege(userPrivileges.view, UserPrivileges.View("", admin = true), v))

					case CreateProjects => UserPrivilegesCreate.replace(username, editPrivilege(userPrivileges.create, UserPrivileges.Create("", projects = true), v))

					case CreateUpdatesTheirProjects => UserPrivilegesCreate.replace(username, editPrivilege(userPrivileges.create, UserPrivileges.Create("", updatesTheirProjects = true), v))

					case CreateUpdatesAllProjects => UserPrivilegesCreate.replace(username, editPrivilege(userPrivileges.create, UserPrivileges.Create("", updatesAllProjects = true), v))

					case CreateUsers => UserPrivilegesCreate.replace(username, editPrivilege(userPrivileges.create, UserPrivileges.Create("", users = true), v))

					case EditJoinProjects => UserPrivilegesEdit.replace(username, editPrivilege(userPrivileges.edit, UserPrivileges.Edit("", joinProjects = true), v))
					
					case EditProjectsOwn => UserPrivilegesEdit.replace(username, editPrivilege(userPrivileges.edit, UserPrivileges.Edit("", projectsOwn = true), v))

					case EditProjectsAll => UserPrivilegesEdit.replace(username, editPrivilege(userPrivileges.edit, UserPrivileges.Edit("", projectsAll = true), v))

					case EditUpdatesOwn => UserPrivilegesEdit.replace(username, editPrivilege(userPrivileges.edit, UserPrivileges.Edit("", updatesOwn = true), v))

					case EditUpdatesAll => UserPrivilegesEdit.replace(username, editPrivilege(userPrivileges.edit, UserPrivileges.Edit("", updatesAll = true), v))

					case EditUserPermissions => UserPrivilegesEdit.replace(username, editPrivilege(userPrivileges.edit, UserPrivileges.Edit("", userPermissions = true), v))

					case FollowUsersAll => UserPrivilegesFollow.replace(username, editPrivilege(userPrivileges.follow, UserPrivileges.Follow("", usersAll = true), v))

					case FollowProjects => UserPrivilegesFollow.replace(username, editPrivilege(userPrivileges.follow, UserPrivileges.Follow("", projectsAll = true), v))

					case DeleteUpdatesOwn => UserPrivilegesDelete.replace(username, editPrivilege(userPrivileges.delete, UserPrivileges.Delete("", updatesOwn = true), v))

					case DeleteUpdatesAll => UserPrivilegesDelete.replace(username, editPrivilege(userPrivileges.delete, UserPrivileges.Delete("", updatesAll = true), v))

					case DeleteUsers => UserPrivilegesDelete.replace(username, editPrivilege(userPrivileges.delete, UserPrivileges.Delete("", users = true), v))

					case DeleteProjects => UserPrivilegesDelete.replace(username, editPrivilege(userPrivileges.delete, UserPrivileges.Delete("", projects = true), v))

					case otherwise => Logger.error(s"Tried to edit some unknown privilege: $otherwise")

				}
			}

			if(authEdit.userPermissions == false) {
				Status(404)("You do not have permission to edit user permissions");
			}
			else {
				val dataParts = request.body.asMultipartFormData.get.dataParts

				val privilegeName = dataParts.getOrElse(PrivilegeName, List(""))(0)
				val privilegeValue = (dataParts.getOrElse(PrivilegeValue, List("false"))(0)).toBoolean

				setBooleanPrivilege(privilegeName, privilegeValue)

				val response = JsObject( 
					Seq(
						"response" -> JsString("project update edited")
					)
				)

				Ok(response);
			}
		})
	}

	def verify(username : String) = Action { implicit request =>
		whenAuthorized(authUsername => {
				verifyUserForm.bindFromRequest.fold(
					formWithErrors => {
					    var errorMessage : String = "";
					    formWithErrors.errors map { error  => {
					    		errorMessage = s"$errorMessage ${error.key}";
					    	}
					    }
						BadRequest(errorMessage);
					},
					{
						case UserForm(firstName, lastName, preferredPronouns, position, officeHourRequirement) => {
							val canEdit = (UserPrivilegesEdit.getUninterruptibly(authUsername).getOrElse { UserPrivilegesEdit.undefined(authUsername) }).userPermissions
							if(!canEdit) {
								Status(404)("You do not have permission to edit user permissions");
							}
							else {
								User.setupSG(username, firstName, lastName, preferredPronouns, position, officeHourRequirement)
									.onSuccess({case _ => User.verifyWithPosition(username, position)})

								val response = JsObject( 
									Seq(
										"response" -> JsString("project update edited")
									)
								)

								Ok(response);		
							}
						}

					})
		})
	}

	def emeritus(username : String) = Action { implicit request =>
		whenAuthorized(authUsername => {

			val canEdit = (UserPrivilegesEdit.getUninterruptibly(authUsername).getOrElse { UserPrivilegesEdit.undefined(authUsername) }).userPermissions
			if(!canEdit) {
				Status(404)("You do not have permission to edit user permissions");
			}
			else {
				val dataParts = request.body.asMultipartFormData.get.dataParts

				val value = dataParts.getOrElse("value", List("false"))(0).toBoolean

				User.emeritus(username, value)

				val response = JsObject( 
					Seq(
						"response" -> JsString("project update edited")
					)
				)

				Ok(response);
			}
		})
	}
}