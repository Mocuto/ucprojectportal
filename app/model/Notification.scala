package model

import actors.masters.ActivityMaster

import com.codahale.metrics.Counter
import com.datastax.driver.core.Row
import com.github.nscala_time.time.Imports._
import com.kenshoo.play.metrics.MetricsRegistry
import com.typesafe.plugin._

import enums.NotificationType
import enums.NotificationType._

import java.util.Date

import play.api.libs.json._
import play.api.mvc._

import scala.collection.JavaConversions._
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

import utils._
import utils.nosql.CassieCommunicator

object Notification {

	val Sender = "sender" //Move into constants

	val createdMeter = MetricsRegistry.default.meter("notifications.created")
	val updateMeter = MetricsRegistry.default.meter("notifications.updates.created")
	val requestMeter = MetricsRegistry.default.meter("notifications.requests.created");
	val messageMeter = MetricsRegistry.default.meter("notifications.messages.created");
	val addedToProjectMeter = MetricsRegistry.default.meter("notifications.added-to-project.created");

	def getForUser(user : User) : Seq[Notification] = return CassieCommunicator.getNotificationsForUser(user);

	def undefined : Notification = return Notification("", new Date(), Map(), NotificationType.NA, false)

	def createRequest(receiver : User, sender : User, project : Project) : Boolean = {
		val request = ProjectRequest.get(project.id, owner = receiver.username, requester = sender.username);
		println(s"request ${request.isDefined}")
		if(request.isDefined == true) { 
			return false;
		}

		if(receiver == User.undefined) {
			Project.changePrimaryContact(project.id, receiver, sender);
			return true;
		}

		val content = Map(Sender -> sender.username, "project_id" -> project.id.toString);
		
		val notification = Notification.create(receiver, content, NotificationType.REQUEST);

		ProjectRequest.create(project.id, receiver.username, sender.username, notification.timeCreated);

		return true;
	}

	def createUpdate(receiver : User, sender : User, project : Project, updateContent : String) {
		val content = Map("sender" -> sender.username, "project_id" -> project.id.toString, "content" -> updateContent);

		updateMeter.mark();
		Notification.create(receiver, content, NotificationType.UPDATE);
	}

	def createMessage(receiver : User, message : String) {
		val content = Map("value" -> message);

		messageMeter.mark()
		Notification.create(receiver, content, NotificationType.MESSAGE);
	}

	def createAddedToProject(receiver : User, project : Project) {
		val content = Map("project_id" -> project.id.toString)

		addedToProjectMeter.mark();
		Notification.create(receiver, content, NotificationType.ADDED_TO_PROJECT)
	}

	def createProjectFrozen(receiver : User, project : Project) {
		val content = Map("project_id" -> project.id.toString)

		Notification.create(receiver, content, NotificationType.ProjectFrozen)
	}

	def createProjectLiked(user : User, project : Project) : Unit = {
		val content = Map("project_id" -> project.id.toString, Sender -> user.username)

		project.teamMembers.foreach(receiver => Notification.create(User.get(receiver), content, NotificationType.ProjectLiked))

		ActivityMaster.scheduleProjectLikedEmail(user, project)
	}

	def create(user : User, content : Map[String, String], notificationType : NotificationType.Value) : Notification = {
		val notification = Notification(user.username, new Date(), content, notificationType);

		CassieCommunicator.setUserUnreadNotifications(user, user.unreadNotifications + 1);
		CassieCommunicator.addNotification(notification);

		Future {
			SMTPCommunicator.sendNotificationEmail(notification);
		}

		createdMeter.mark();
		return notification;
	}

	def resetUnreadForUser(user : User) {
		CassieCommunicator.setUserUnreadNotifications(user, 0);
	}

	def clearAllForUser(user : User) {
		Notification.getForUser(user).foreach(notification => {
			if(notification.notificationType == NotificationType.REQUEST) {
				val request = ProjectRequest.get(notification.content("project_id").toInt, notification.username, notification.content("sender"))
				request.ignore();
			}
			else {
				notification.delete();
			}
		})
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