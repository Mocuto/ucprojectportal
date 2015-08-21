import controllers._;

import actors.masters._

import com.kenshoo.play.metrics.MetricsFilter

import java.nio.file.{Files, Paths}

import model._;
import model.UserPrivileges;

import play.api._
import play.api.data._
import play.api.mvc._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.libs.json._
import play.api.Logger

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

import com.github.marklister.base64.Base64.Decoder
import actors.Scheduler

object AccessFilter {
	def apply(requiredViewPrivilegeFunc : (String => UserPrivileges.View), actionNames: String*) = new AccessFilter(requiredViewPrivilegeFunc, actionNames)
}

class AccessFilter(requiredViewPrivilegeFunc : (String => UserPrivileges.View), actionNames: Seq[String]) extends Filter {
	def apply(next: (RequestHeader) => Future[Result])(request: RequestHeader): Future[Result] = {

		if(authorizationRequired(request)) {
			request.session.get("authenticated") match {
				case None => Future { Results.Redirect("/login/" + request.path) }
				case Some(username) => {
					lazy val requiredViewPrivilege = requiredViewPrivilegeFunc(username)
					(UserPrivilegesView.get(username) zip next(request)).map {
						case (Some(UserPrivileges.View(username, a, b, c, d, e)), result : Result) if (UserPrivileges.View(
								username,
								a & requiredViewPrivilege.projects,
								b & requiredViewPrivilege.users,
								c & requiredViewPrivilege.accountability,
								d & requiredViewPrivilege.moderator,
								e & requiredViewPrivilege.admin) == requiredViewPrivilege) => result;

						case _ => Results.NotFound(views.html.messages.notFound("this page does not exist"));
					}
				}
			}
		}
		else {
			next(request);
		}
	}

	private def authorizationRequired(request: RequestHeader) : Boolean = {
		val actionInvoked: String = request.tags.getOrElse(play.api.routing.Router.Tags.RouteActionMethod, "")
		return actionNames.contains(actionInvoked)
	}
}

object AuthorizedFilter {
  def apply(actionNames: String*) = new AuthorizedFilter(actionNames)
}

class AuthorizedFilter(actionNames: Seq[String]) extends Filter {

	def apply(next: (RequestHeader) => Future[Result])(request: RequestHeader): Future[Result] = {
		if(authorizationRequired(request)) {
		  request.session.get("authenticated") match {
		  	case None => Future {
		  		if(constants.ServerSettings.AuthenticationMode == enums.AuthenticationMode.Login) {
		  			Results.Redirect(routes.Application.login(request.path))		  			
		  		}
		  		else {
		  			Results.Redirect(routes.ShibbolethController.secure(request.path))
		  		}

		  	}
		  	case Some(username) => {
		  		val user = User.get(username);
		  		if (user.hasConfirmed) {
		  			next(request)
		  		}
		  		else {
		  			Future {
	  					Results.Redirect(routes.ActivationController.activateNEW(request.path))
		  			}
		  		}
		  	}
		  }
		}
		else {
			next(request)
		}
	}

	private def authorizationRequired(request: RequestHeader) : Boolean = {
		val actionInvoked: String = request.tags.getOrElse(play.api.routing.Router.Tags.RouteActionMethod, "")

		return !actionNames.contains(actionInvoked)
	}


}

object Global extends WithFilters(AuthorizedFilter("login", "tryLogin",
													"secure", "at", 
													"activate", "resendActivation", "tryResendActivation", "tryActivate", "activateNEW", "tryActivateNEW",
													"forgotPassword", "tryForgotPassword", "resetPassword", "tryResetPassword"),

/*object Global extends WithFilters(AuthorizedFilter("index", "project", "newProject", "filter", "user",
													"submitProject", "leaveProject", "editProject",
													"feedback" , "submitUpdate", "uploads",
													"resetUnread", "getUnreadCount", "ignore",
													"clearAll",
													"decide", "signout"),*/
								 //AccessFilter("accountability", "accountability"),
								 AccessFilter(UserPrivilegesView.admin(_ : String), "admin", "deleteProject", "deleteUser", "metrics"),
								 AccessFilter(UserPrivilegesView.moderator(_ : String), "moderator", "moderation"),
								 MetricsFilter) {


	def cleanLockFiles() : Unit = {
		val writeLock = Paths.get(constants.Directories.INDEXES, "write.lock")

		val deleted = Files.deleteIfExists(writeLock)

		if(deleted) {
			Logger.info("Indexes lock file deleted")
		}
	}

	/** This method will create the /uploads and /indexes directory

	*/
	def createDirectories() : Unit = {
		val uploadsDir = Paths.get(constants.Directories.Uploads);
		val indexesDir = Paths.get(constants.Directories.INDEXES);

		if(Files.exists(uploadsDir) == false)
		{
			Files.createDirectory(uploadsDir)
			Logger.info(s"Created uploads directory: $uploadsDir")
		}

		if(Files.exists(indexesDir) == false)
		{
			Files.createDirectory(indexesDir)
			Logger.info(s"Created indexes directory: $indexesDir")
		}
	}

	def startDaemons() : Unit = {
		IndexerMaster.start();

		ActivityMaster.start()
	}

	def stopDaemons() : Unit = {
		IndexerMaster.stop();

		ActivityMaster.stop();
	}

	override def onStart(app: Application) : Unit = {
		Logger.info("Application has started")

		createDirectories();

		cleanLockFiles();

		startDaemons();
	}


	override def onStop(app: Application) : Unit = {
		Logger.info("Application shutdown...")

		stopDaemons();
	}
}