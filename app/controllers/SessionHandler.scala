package controllers

import play.api.mvc.{RequestHeader, Result}

trait SessionHandler {
	def authenticated(implicit request : RequestHeader) : Some[String] = Some(request.session.get("authenticated").get)

	def whenAuthorized(f : String => Result)(orElse : Result)(implicit request : RequestHeader) = {
		request.session.get("authenticated") match {
			case Some(username) => f(username)
			case _ => orElse
		}
	}
}