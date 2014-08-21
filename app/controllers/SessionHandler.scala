package controllers

import play.api.mvc.RequestHeader

trait SessionHandler {
	def authenticated(implicit request : RequestHeader) : Some[String] = Some(request.session.get("authenticated").get)
}