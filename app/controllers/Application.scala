package controllers

import com.codahale.metrics.{Counter, Meter}
import com.kenshoo.play.metrics.MetricsRegistry

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

	val loginMeter = MetricsRegistry.defaultRegistry.meter("logins")
	val feedbackCounter = MetricsRegistry.defaultRegistry.counter("feedback");

	implicit val loginForm : Form[(String, String)] = Form(
		tuple(
			"username" -> nonEmptyText,
			"password" -> nonEmptyText
		).verifying("incorrect username or password", fields => fields match {
			case (username, password) => { User.authenticate(username, password).isDefined}
		}).verifying("user account not yet confirmed", fields => fields match {
			case (username, password) => { val user = User.get(username); user.hasConfirmed || !user.isDefined}
		})
	)

	val feedbackForm = Form(
		tuple(
			"author" -> nonEmptyText,
			"content" -> nonEmptyText,
			"type" -> nonEmptyText
		)
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
			play.api.routing.JavaScriptReverseRouter("jsRoutes")
			(
			 routes.javascript.Application.search,
			 routes.javascript.UserController.profilePic,
			 routes.javascript.UserController.follow,
			 routes.javascript.UserController.unfollow,
			 routes.javascript.ProjectController.edit,
			 routes.javascript.ProjectController.leave,
			 routes.javascript.ProjectController.like,
			 routes.javascript.ProjectController.unlike,
			 routes.javascript.ProjectController.follow,
			 routes.javascript.ProjectController.unfollow,
			 routes.javascript.ProjectController.jsonForAll,
			 routes.javascript.ProjectController.jsonForUser,
			 routes.javascript.ProjectUpdateController.submit,
			 routes.javascript.ProjectUpdateController.edit,
			 routes.javascript.ProjectUpdateController.delete,
			 routes.javascript.ProjectUpdateController.like,
			 routes.javascript.ProjectUpdateController.unlike,
			 routes.javascript.RequestController.join,
			 routes.javascript.RequestController.accept,
			 routes.javascript.RequestController.ignore,
			 routes.javascript.NotificationController.resetUnread,
			 routes.javascript.NotificationController.getUnreadCount,
			 routes.javascript.NotificationController.ignore,
			 routes.javascript.NotificationController.clearAll,
			 routes.javascript.ModerationController.editUserPrivileges,
			 routes.javascript.ModerationController.editUserFollowing,
			 routes.javascript.ModerationController.emeritus,
			 routes.javascript.ModerationController.verify,
			 routes.javascript.AdminController.deleteUser,
			 routes.javascript.AdminController.deleteProject
			 )
		).as("text/javascript")
	}

	def index = Action { implicit request => {
		authenticated match {
			case Some(authenticatedUsername) => {
				val authenticatedUser = User.get(authenticatedUsername);
				Ok(views.html.index(authenticatedUser));
			}
		}


	}}

	def filter(filterStr : String) = Action { implicit request =>
		whenAuthorized(username => {
			val authenticatedUser = User.get(username);
			val editPrivileges = UserPrivilegesEdit.getUninterruptibly(username).getOrElse {UserPrivilegesEdit.undefined(username)}
			val followPrivileges = UserPrivilegesFollow.getUninterruptibly(username).getOrElse {UserPrivilegesFollow.undefined(username)}
			val canJoin = editPrivileges.joinProjects
			val canFollow = followPrivileges.projectsAll
			Ok(views.html.filter(authenticatedUser, filterStr, canJoin, canFollow))
		})
	}

	def login(path : String) = Action {
		if(constants.ServerSettings.AuthenticationMode == enums.AuthenticationMode.Shibboleth) {
			Redirect(routes.ShibbolethController.secure(path))
		}
		else {
			path match {
				case "/" => Redirect(routes.Application.login(""))
				case _ => Ok(views.html.login(path)(loginForm));
			}
		}
		
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
				loginMeter.mark();
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

	def workshops = TODO

	def feedback = Action { implicit request =>
		authenticated match {
			case Some(username) => {
				val user = User.get(username);

				Ok(views.html.feedback(user)(feedbackForm))
			}
		}
	}

	def search(query : String) = Action { implicit request =>
		whenAuthorized(username => {
			val user = User.get(username);

			val projects = if (query.length == 0) {
				List[Project]();
			} else {
				ProjectSearcher.search(query);
			}
			val editPrivileges = UserPrivilegesEdit.getUninterruptibly(username).getOrElse {UserPrivilegesEdit.undefined(username)}
			val canJoin = editPrivileges.joinProjects
			val followPrivileges = UserPrivilegesFollow.getUninterruptibly(username).getOrElse {UserPrivilegesFollow.undefined(username)}
			val canFollow = followPrivileges.projectsAll

			Ok(views.html.search(user, canJoin, canFollow)(projects, query))
		})
	}

	def submitFeedback = Action { implicit request =>
		authenticated match {
			case Some(username) => {
				val user = User.get(username);
				feedbackForm.bindFromRequest.fold(
					formWithErrors => {
						BadRequest(views.html.feedback(user)(formWithErrors))
					},
					{ case (author : String, content : String, feedbackType : String) => {
						Feedback.create(author, content, feedbackType);

						feedbackCounter.inc();
						Ok(views.html.messages.prettyMessage(play.twirl.api.Html("your feedback has been sent!")))
					}}
				)
			}
		}
	}

	def gettingStarted = TODO

}