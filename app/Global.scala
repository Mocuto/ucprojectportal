import controllers._;

import play.api._
import play.api.data._
import play.api.mvc._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.libs.json._

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

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

object Global extends WithFilters(AuthorizedFilter("index", "project", "newProject", "filter", "admin", "user", "submitProject", "submitUpdate", "uploads")) {

}