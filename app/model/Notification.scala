package model

import com.datastax.driver.core.Row
import com.github.nscala_time.time.Imports._

import java.util.Date

import play.api.libs.json._
import play.api.mvc._

import scala.collection.JavaConversions._

object Notification {

	val SENDER = "sender"

	def getForUser(user : User) : Seq[Notification] = return CassieCommunicator.getNotificationsForUser(user);

	def undefined : Notification = return Notification("", new Date(), Map(), NotificationType.NA, false)

	def createRequest(receiver : User, sender : User, project : Project) : Boolean = {
		val request = ProjectRequest.get(project.id, owner = receiver.username, requester = sender.username);
		println(s"request ${request.isDefined}")
		if(request.isDefined == true) { 
			return false;
		}

		val content = Map("sender" -> sender.username, "project_id" -> project.id.toString);
		
		val notification = Notification.create(receiver, content, NotificationType.REQUEST);

		ProjectRequest.create(project.id, receiver.username, sender.username, notification.timeCreated);

		return true;
	}

	def createUpdate(receiver : User, sender : User, project : Project) {
		val content = Map("sender" -> sender.username, "project_id" -> project.id.toString);

		Notification.create(receiver, content, NotificationType.UPDATE);
	}

	def createMessage(receiver : User, message : String) {
		val content = Map("value" -> message);

		Notification.create(receiver, content, NotificationType.MESSAGE);
	}

	def create(user : User, content : Map[String, String], notificationType : NotificationType.Value) : Notification = {
		val notification = Notification(user.username, new Date(), content, notificationType);

		CassieCommunicator.setUserUnreadNotifications(user, user.unreadNotifications + 1);
		CassieCommunicator.addNotification(notification);
	}

	def resetUnreadForUser(user : User) {
		CassieCommunicator.setUserUnreadNotifications(user, 0);
	}

	implicit def fromRow(row : Row) : Notification = {
		row match {
			case null => return Notification.undefined
			case row : Row => {
				return Notification(
					row.getString("username"),
					row.getDate("time_created"),
					row.getMap("content", classOf[String], classOf[String]).toMap,
					NotificationType.fromString(row.getString("type"))
				)
			}
		}
	}
}

case class Notification(username : String, timeCreated : Date, content : Map[String, String] = Map(), notificationType : NotificationType.Value = NotificationType.NA,
	isDefined : Boolean = true) {
	def toDisplayedText : String = return "";

	def delete() {
		CassieCommunicator.removeNotification(this);
	}
}