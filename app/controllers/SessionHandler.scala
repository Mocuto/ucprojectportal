package controllers

import play.api.mvc.RequestHeader

trait SessionHandler {
	def authenticated(implicit request : RequestHeader) : Option[String] = request.session.get("authenticated")
}