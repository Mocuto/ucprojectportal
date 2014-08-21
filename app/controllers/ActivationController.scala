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

object ActivationController extends Controller {

	val USER_ACCOUNT_DOES_NOT_EXIST =
		s"""
			|this user account does not exist. for support,
			|please contact <a href='${constants.ServerSettings.ADMIN_EMAIL}'>${constants.ServerSettings.ADMIN_NAME.toLowerCase()}</a>
		""".stripMargin

	val ACTIVATION_EMAIL_HAS_BEEN_SENT = "the activation email has been sent. be sure to check your spam folder!"

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
				Ok(views.html.prettyMessage(play.twirl.api.Html(ACTIVATION_EMAIL_HAS_BEEN_SENT)))
			}
		)

	}}

	def activate(username : String, uuid : String) = Action { implicit request => {
		User.getActivationCode(username) match {
			case None => BadRequest(views.html.notFound(PAGE_NOT_FOUND))
			case Some(correctUUID) if correctUUID == uuid => Ok(views.html.activate(username, uuid)(activateForm));
			case _ => BadRequest(views.html.notFound(PAGE_NOT_FOUND))
		}
	}}

	def tryActivate = Action { implicit request => {
		activateForm.bindFromRequest.fold(
			formWithErrors => {
				val username = formWithErrors("username").value.get;
				val code = formWithErrors("code").value.get;

				User.getActivationCode(username) match {
					case None => {
						if(User.get(username) == User.undefined) {
							BadRequest(views.html.prettyMessage(play.twirl.api.Html(USER_ACCOUNT_DOES_NOT_EXIST)))
						}
						else {
							BadRequest(views.html.prettyMessage(play.twirl.api.Html(USER_ACCOUNT_ALREADY_ACTIVATED)))
						}
					}
					case Some(correctCode) => {
						if(correctCode != code) {
							BadRequest(views.html.prettyMessage(play.twirl.api.Html(ACTIVATION_CODE_INCORRECT)))
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

}