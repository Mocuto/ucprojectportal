import controllers._;

import model._;

import play.api._
import play.api.data._
import play.api.mvc._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.libs.json._

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

object AdminFilter {
	def apply(actionNames: String*) = new AdminFilter(actionNames)
}

class AdminFilter(actionNames: Seq[String]) extends Filter {
	def apply(next: (RequestHeader) => Future[Result])(request: RequestHeader): Future[Result] = {
		if(authorizationRequired(request)) {
		  request.session.get("authenticated") match {
		  	case None => Future {
		  		Results.Redirect("/login/" + request.path)
		  	}
		  	case authenticatedUser if UserGroup.isUserInGroup(User.get(authenticatedUser.get), "admin") == false => Future {
		  		Results.NotFound(views.html.notFound("this page does not exist"));
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
		  		Results.Redirect("/login/" + request.path)
		  	}
		  	case Some(username) => {
		  		val user = User.get(username);
		  		if (user.hasConfirmed) {
		  			next(request)
		  		}
		  		else {
		  			Future {
	  					Results.Redirect("/login/" + request.path)
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
													"submitProject", "submitUpdate", "uploads", "acceptRequest", "ignoreRequest",
													"resetUnreadNotifications", "getUnreadNotificationCount", "ignoreNotification",
													"decideRequest"),
								 AdminFilter("admin", "deleteProject", "deleteUser")) {

}