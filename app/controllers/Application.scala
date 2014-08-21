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

object Application extends Controller with SessionHandler {

	implicit val loginForm : Form[(String, String)] = Form(
		tuple(
			"username" -> nonEmptyText,
			"password" -> nonEmptyText
		).verifying("incorrect username or password", fields => fields match {
			case (username, password) => { User.authenticate(username, password).isDefined}
		}).verifying("user account not yet confirmed", fields => fields match {
			case (username, password) => { User.get(username).hasConfirmed}
		})
	)

	def checkAuthenticated (request : Request[play.api.mvc.AnyContent])(success : (String) => Result ) : Result = { 
		request.session.get("authenticated").map {
			authenticatedUser => success(authenticatedUser)
		}.getOrElse {
			Redirect(routes.Application.login(request.path));
		}
	}

	def javascriptRoutes = Action { implicit request =>
		import routes.javascript._
		Ok(
			Routes.javascriptRouter("jsRoutes")
			(
			 routes.javascript.ProjectController.submitUpdate,
			 routes.javascript.ProjectController.editProject,
			 routes.javascript.ProjectController.leaveProject,
			 routes.javascript.RequestController.requestJoin,
			 routes.javascript.RequestController.acceptRequest,
			 routes.javascript.RequestController.ignoreRequest,
			 routes.javascript.NotificationController.resetUnreadNotifications,
			 routes.javascript.NotificationController.getUnreadNotificationCount,
			 routes.javascript.NotificationController.ignoreNotification
			 )
		).as("text/javascript")
	}

	def index = Action { implicit request => {
		val authenticatedUser = request.session.get("authenticated").get;
		val user = User.get(authenticatedUser);
		Ok(views.html.index(user));

	}}

	def filter(filterStr : String) = Action { implicit request => {
		authenticated match {
			case Some(username) => {
				val authenticatedUser = User.get(username);
				Ok(views.html.filter(authenticatedUser, filterStr))
			}
		}
	}}

	def login(path : String) = Action {
		Ok(views.html.login(path)(loginForm));
	}

	def login : Action[play.api.mvc.AnyContent] = login("");


	def uploads(filename : String) = Action {
		val file : java.io.File = new java.io.File(s"uploads/$filename");
		if (file.exists()) {
		  	Ok.sendFile(
			    content = file,
			    fileName = _ => utils.Conversions.stripUUID(filename)
	  		)
		}
		else {
			BadRequest(s"$filename does not exist")
		}

	}

	def tryLogin(path : String) = Action { implicit request =>
		loginForm.bindFromRequest.fold(
			formWithErrors => {
				BadRequest(views.html.login()(formWithErrors))
			},
			loginData => {
				println(loginData._1)
				if(path == "") {
					Redirect(routes.Application.index).withSession(
						"authenticated" -> loginData._1			)
				}
				else {
					Redirect(path).withSession(
						"authenticated" -> loginData._1			)
				}

			}
		)
	}



	def signout = Action { implicit request =>
		Redirect(routes.Application.login("")).withSession(
			request.session - "authenticated"
		)

	}

}