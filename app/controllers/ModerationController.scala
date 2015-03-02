package controllers

import com.typesafe.plugin._

import java.util.Date

import model._

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.libs.json._

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

import utils._

object ModerationController extends Controller with SessionHandler {

	def moderation = Action { implicit request =>
		authenticated match {
			case Some(username) => {
				val authenticatedUser = User.get(username);

				Ok(views.html.moderation(authenticatedUser));
			}
		}
	}

}