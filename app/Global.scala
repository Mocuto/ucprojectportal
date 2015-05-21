import controllers._;

import com.kenshoo.play.metrics.MetricsFilter

import model._;
import model.UserPrivileges;

import play.api._
import play.api.data._
import play.api.mvc._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.libs.json._

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

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
								 //AccessFilter("accountability", "accountability"),
								 AccessFilter(UserPrivilegesView.admin(_ : String), "admin", "deleteProject", "deleteUser", "metrics"),
								 AccessFilter(UserPrivilegesView.moderator(_ : String), "moderator", "moderation"),
								 MetricsFilter) {

}