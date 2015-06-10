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

object ActivationController extends Controller with SessionHandler {

	val USER_ACCOUNT_DOES_NOT_EXIST =
		s"""
			|this user account does not exist. for support,
			|please contact <a href='${constants.ServerSettings.ADMIN_EMAIL}'>${constants.ServerSettings.ADMIN_NAME.toLowerCase()}</a>
		""".stripMargin

	val ACTIVATION_EMAIL_HAS_BEEN_SENT = "the activation email has been sent. be sure to check your spam folder!"

	val FORGOT_PASSWORD_EMAIL = "a password reset email has been sent. be sure to check your spam folder!"

	val USER_ACCOUNT_ALREADY_ACTIVATED = 
		s"""
			|this user account has already been activated. please <a href='/login'>log in</a>
		""".stripMargin

	val PAGE_NOT_FOUND = "this page does not exist"

	val ACTIVATION_CODE_INCORRECT = 
		s"""
			|activation code is incorrect. please check your email for the latest activation email, or 
			|<a href='${routes.ActivationController.resendActivation}'>click here to resend the activation email</a>
		""".stripMargin

	val resendActivationForm = Form (
		"username" -> nonEmptyText
		.verifying("invalid username", fields => fields match {
			case (username) => User.get(username).isDefined
		}).verifying("this account has already been confirmed", fields => fields match {
			case (username) => !User.get(username).hasConfirmed
		})
	)

	val activateForm = Form(
		tuple(
			"first_name" -> nonEmptyText,
			"last_name" -> nonEmptyText,
			"password" -> nonEmptyText,
			"confirm_password" ->nonEmptyText,
			"username" -> nonEmptyText,
			"code" -> nonEmptyText
		).verifying("password and confirm password do not match", fields => fields match {
			case(firstName, lastName, password, confirmPassword, username, code) => password == confirmPassword
		}).verifying("invalid activation code", fields => fields match {
			case(firstName, lastName, password, confirmPassword, username, code) => println(s"code is $code"); User.getActivationCode(username).get == code
		})
	)

	val forgotPasswordForm = Form (
		"username" -> nonEmptyText
		.verifying("account is unactivated", fields => fields match {
			case(username) => User.get(username).lastLogin != null
		})
	)

	val resetPasswordForm = Form(
		tuple(
			"password" -> nonEmptyText,
			"confirm_password" -> nonEmptyText,
			"username" -> nonEmptyText,
			"code" -> nonEmptyText
		).verifying("password and confirm password do not match", fields => fields match {
			case(password, confirmPassword, username, code) => password == confirmPassword
		}).verifying("invalid reset code", fields => fields match {
			case(password, confirmPassword, username, code) => println(s"code is $code"); User.getActivationCode(username).get == code
		}).verifying("account is unactivated", fields => fields match {
			case(_, _, username : String, _) => User.get(username).lastLogin != null
		})
	)


	case class UserForm(firstName : String, lastName : String, preferredPronouns : String, position : String);

	val sgAccountForm = Form(
		mapping(
			"first_name" -> nonEmptyText,
			"last_name" -> nonEmptyText,
			"preferred_pronouns" -> nonEmptyText,
			"position" -> nonEmptyText
		) (UserForm.apply) (UserForm.unapply)
	)

	def resendActivation = Action { implicit request => {
		Ok(views.html.resendActivation(resendActivationForm));
	}}


	def tryResendActivation = Action { implicit request => {
		resendActivationForm.bindFromRequest.fold(
			formWithErrors => {
				BadRequest(views.html.resendActivation(formWithErrors))
			},
			username => {
				//Send confirmation email
				val uuid = User.makeActivation(User.get(username))
				SMTPCommunicator.sendActivationEmail(username, uuid.getOrElse(""));
				Ok(views.html.messages.prettyMessage(play.twirl.api.Html(ACTIVATION_EMAIL_HAS_BEEN_SENT)))
			}
		)

	}}

	def forgotPassword = Action { implicit request => {
		Ok(views.html.forgotPassword(forgotPasswordForm));
	}}

	def tryForgotPassword = Action { implicit request => {
		forgotPasswordForm.bindFromRequest.fold(
			formWithErrors => {
				BadRequest(views.html.forgotPassword(formWithErrors))
			},
			username => {
				val user = User.get(username);
				val uuid = User.forgotPassword(user)

				if(user.lastLogin == null) {
					SMTPCommunicator.sendActivationEmail(username, uuid.getOrElse(""));
					Ok(views.html.messages.prettyMessage(play.twirl.api.Html(ACTIVATION_EMAIL_HAS_BEEN_SENT)))
				} 
				else {
					SMTPCommunicator.sendForgotPasswordEmail(username, uuid.getOrElse(""));
					Ok(views.html.messages.prettyMessage(play.twirl.api.Html(FORGOT_PASSWORD_EMAIL)))
				}
			}
		)	
	}}

	def activate(username : String, uuid : String) = Action { implicit request => {
		User.getActivationCode(username) match {
			case None => BadRequest(views.html.messages.notFound(PAGE_NOT_FOUND))
			case Some(correctUUID) if correctUUID == uuid => Ok(views.html.activate(username, uuid)(activateForm));
			case _ => BadRequest(views.html.messages.notFound(PAGE_NOT_FOUND))
		}
	}}

	def activateNEW(path : String) = Action { implicit request => authenticated match {
		case Some(username) => Ok(views.html.activateNEW(path)(sgAccountForm))
	}}

	def resetPassword(username: String, uuid : String) = Action {implicit request => {
		User.getActivationCode(username) match {
			case None => BadRequest(views.html.messages.notFound(PAGE_NOT_FOUND))
			case Some(correctUUID) if correctUUID == uuid => Ok(views.html.resetPassword(username, uuid)(resetPasswordForm));
			case _ => BadRequest(views.html.messages.notFound(PAGE_NOT_FOUND))
		}
	}}
	def tryResetPassword = Action { implicit request => {
		resetPasswordForm.bindFromRequest.fold(
			formWithErrors => {
				val username = formWithErrors("username").value.get;
				val code = formWithErrors("code").value.get;

				User.getActivationCode(username) match {
					case None => {

						BadRequest(views.html.messages.prettyMessage(play.twirl.api.Html(USER_ACCOUNT_DOES_NOT_EXIST)))
						
					}
					case Some(correctCode) => {
						if(correctCode != code) {
							BadRequest(views.html.messages.prettyMessage(play.twirl.api.Html(ACTIVATION_CODE_INCORRECT)))
						} else {
							BadRequest(views.html.resetPassword(username, code)(formWithErrors))
						}
					}
				}
			},
			passwordData => {
				passwordData match {
					case (password, confirmPassword, username, code) => {
						val user = User.get(username);
						User.activate(username, user.firstName, user.lastName, password)
						User.authenticate(username, password);
						Redirect(routes.Application.index).withSession("authenticated" -> username)
					}
				}
			}
		)
	}}


	def tryActivate = Action { implicit request => {
		activateForm.bindFromRequest.fold(
			formWithErrors => {
				val username = formWithErrors("username").value.get;
				val code = formWithErrors("code").value.get;

				User.getActivationCode(username) match {
					case None => {
						if(User.get(username) == User.undefined) {
							BadRequest(views.html.messages.prettyMessage(play.twirl.api.Html(USER_ACCOUNT_DOES_NOT_EXIST)))
						}
						else {
							BadRequest(views.html.messages.prettyMessage(play.twirl.api.Html(USER_ACCOUNT_ALREADY_ACTIVATED)))
						}
					}
					case Some(correctCode) => {
						if(correctCode != code) {
							BadRequest(views.html.messages.prettyMessage(play.twirl.api.Html(ACTIVATION_CODE_INCORRECT)))
						} else {
							BadRequest(views.html.activate(username, code)(formWithErrors))
						}
					}
				}
			},
			activateData => {
				activateData match {
					case (firstName, lastName, password, confirmPassword, username, code) => {
						User.activate(username, firstName, lastName, password)
						User.authenticate(username, password);
						Redirect(routes.Application.index).withSession("authenticated" -> username)
					}
				}
			}
		)
	}}

	def tryActivateNEW = Action { implicit request =>
		sgAccountForm.bindFromRequest.fold(
			formWithErrors => {
				BadRequest(views.html.activateNEW()(formWithErrors))
			},
			userForm => (request.session.get("authenticated"), userForm) match {
				case (Some(username : String), UserForm(firstName, lastName, pronouns, position)) => {
					User.setupSG(username, firstName, lastName, pronouns, position);
					println(username);
					SMTPCommunicator.sendAllVerifyUserEmail(username);
					Redirect(routes.Application.gettingStarted)
				}
				case (None, _) => Redirect(routes.ShibbolethController.secure)
			}
		)
	}

	def activateNonSG = Action { implicit request =>
		whenAuthorized(username => {
				User.setupNonSG(username);
				Redirect(routes.Application.gettingStarted)
			})(orElse = Redirect(routes.ShibbolethController.secure))
		
	}

}