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

object NotificationController extends Controller with SessionHandler {

	def resetUnread = Action { implicit request =>
		authenticated match {
			case Some(username) => {
				val authenticatedUser = User.get(username);
				Notification.resetUnreadForUser(authenticatedUser);

				val response = JsObject(
					Seq(
						"response" -> JsString("notifications have been reset")
					)
				)

				Ok(response);
			}
		}
	}

	def getUnreadCount = Action { implicit request =>
		authenticated match {
			case Some(username) => {
				val authenticatedUser = User.get(username);

				val response = JsObject(
					Seq(
						"count" -> JsNumber(authenticatedUser.unreadNotifications),
						"html" -> JsString(views.html.common.notificationsListView(authenticatedUser).toString)
					)
				)

				Ok(response);
			}
		}
	}

	def ignore (timeCreated : String) = Action { implicit request =>
		authenticated match {
			case Some(username) => {
				val notification = Notification(username, utils.Conversions.strToDate(timeCreated));
				notification.delete();
				val response = JsObject(
					Seq(
						"response" -> JsString("notification has been ignored")
					)
				)

				Ok(response);
			}
		}
	}

	def clearAll = Action { implicit request =>
		authenticated match {
			case Some(username) => {
				Notification.clearAllForUser(User.get(username));
				val response = JsObject(
					Seq(
						"response" -> JsString("notifications have been ignored")
					)
				)
				Ok(response);
			}
		}
	}
}