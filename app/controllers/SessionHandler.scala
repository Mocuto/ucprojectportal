package controllers

import play.api.mvc.{RequestHeader, Result}
import play.api.mvc.Results.Status

trait SessionHandler {
	def authenticated(implicit request : RequestHeader) : Some[String] = Some(request.session.get("authenticated").get)

	def whenAuthorized(f : String => Result)
		(implicit request : RequestHeader,
		orElse : Result = Status(401)("You are not authorized. try logging in.")) = {

		request.session.get("authenticated") match {
			case Some(username) => f(username)
			case _ => orElse
		}
	}
}