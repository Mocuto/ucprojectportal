import controllers._;

import com.kenshoo.play.metrics.MetricsFilter

import java.nio.file.{Files, Paths}

import model._;
import model.routines._

import play.api._
import play.api.data._
import play.api.mvc._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.libs.json._

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

import actors.Scheduler

object AccessFilter {
	def apply(userGroupName : String, actionNames: String*) = new AccessFilter(userGroupName, actionNames)
}

class AccessFilter(userGroupName : String, actionNames: Seq[String]) extends Filter {
	def apply(next: (RequestHeader) => Future[Result])(request: RequestHeader): Future[Result] = {
		if(authorizationRequired(request)) {
		  request.session.get("authenticated") match {
		  	case None => Future {
		  		Results.Redirect("/login/" + request.path)
		  	}
		  	case authenticatedUser if UserGroup.isUserInGroup(User.get(authenticatedUser.get), userGroupName) == false => Future {
		  		Results.NotFound(views.html.messages.notFound("this page does not exist"));
		  	}
		  	case _ => {
			  next(request)
		  	}
		  }
		}
		else {
			next(request)
		}
	}

	private def authorizationRequired(request: RequestHeader) : Boolean = {
		val actionInvoked: String = request.tags.getOrElse(play.api.Routes.ROUTE_ACTION_METHOD, "")
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
		  		Results.Redirect(routes.Application.login(request.path))
		  	}
		  	case Some(username) => {
		  		val user = User.get(username);
		  		if (user.hasConfirmed) {
		  			next(request)
		  		}
		  		else {
		  			Future {
	  					Results.Redirect(routes.Application.login(request.path))
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
		val actionInvoked: String = request.tags.getOrElse(play.api.Routes.ROUTE_ACTION_METHOD, "")
		return actionNames.contains(actionInvoked)
	}


}

object Global extends WithFilters(AuthorizedFilter("index", "project", "newProject", "filter", "user",
													"submitProject", "leaveProject", "editProject",
													"feedback" , "submitUpdate", "uploads",
													"resetUnread", "getUnreadCount", "ignore",
													"clearAll",
													"decide", "signout"),
								 AccessFilter("admin", "admin", "deleteProject", "deleteUser", "metrics"),
								 AccessFilter("moderator", "moderation"),
								 MetricsFilter) {



	def startCassandra() : Unit = {
		Logger.info("Attempting to start Cassandra...")
		//TODO: Implement this
	}

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
		val uploadsDir = Paths.get(constants.Directories.UPLOADS);
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
		Scheduler.schedule[Project](Routine.IndexingRoutine)
	}

	override def onStart(app: Application) : Unit = {
		Logger.info("Application has started")

		createDirectories();

		cleanLockFiles();

		startCassandra();

		//startDaemons();

	}

}