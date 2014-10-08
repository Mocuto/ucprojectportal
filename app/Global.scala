import controllers._;

import com.kenshoo.play.metrics.MetricsFilter

import model._;

import play.api._
import play.api.data._
import play.api.mvc._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.libs.json._

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

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

}