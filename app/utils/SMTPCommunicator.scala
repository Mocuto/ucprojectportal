package utils

import com.typesafe.plugin._

import model._

import play.api.Play.current
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.libs.json._

import views._

object SMTPCommunicator {

	private val host = constants.ServerSettings.CHOSEN_HOST;
	private val mail = use[MailerPlugin].email
	private val MAX_TRIES = 5;

	private val logger = Logger(this.getClass())

	def sendEmail(recipient : String, subject : String, content : String) {
		var tries = 0;
		var wasSent : Boolean = false;
		val recipientEmail = recipient + "@mail.uc.edu"
		logger.debug(s"recipient: $recipientEmail")
		logger.debug(s"subject: $subject")
		logger.debug(s"content: $content");

		while(tries < MAX_TRIES && wasSent == false) {
			try {
				tries += 1;
				mail.setSubject(subject)
				mail.setRecipient(s"${User.getFullName(recipient)} <$recipientEmail>", s"$recipientEmail")
				//or use a list
				mail.setFrom(s"Project Portal <noreply@ucprojectportal.com>")
				//sends html
				mail.sendHtml(content);
				wasSent = true;

			}
			catch {
				case e : Exception => {
					println(e)
					logger.error(s"Exception with sendEmail", e);
					Thread.sleep(1000)
				}
			}
		}

		if(wasSent == false) {
			println("The email coud not be sent:")
			println(recipient)
			println(subject)
			println(content);
		}
	}

	def sendActivationEmail(recipient : String, uuid : String) {
		val subject = "Activate Your Project Portal Account";
		val content = views.html.email.emailActivation(User.get(recipient), uuid).toString;

		sendEmail(recipient, subject, content);
	}

	def sendNotificationUpdateEmail(recipient: String, updater : String, projectId : Int, updateContent : String) {
		val subject = s"${model.User.getFullName(updater)} Posted an Update in the Project ${model.Project.get(projectId).name}!";
		val content = views.html.email.emailUpdate(User.get(updater), Project.get(projectId), updateContent).toString
		
		sendEmail(recipient, subject, content);
	}

	def sendNotificationMessageEmail(recipient : String, message : String) {
		val subject = message;
		val content = views.html.email.emailMessage(play.twirl.api.Html(message), "").toString

		sendEmail(recipient, subject, content);
	}

	def sendNotificationRequestEmail(recipient : String, requester : String, projectId : Int) {
		val requesterUser = User.get(requester);
		val project = Project.get(projectId);
		
		val subject = s"${requesterUser.fullName} has requested to join the project ${project.name}"
		val content = views.html.email.emailRequest(requesterUser, project).toString;

		sendEmail(recipient, subject, content);
	}

	def sendNotificationAddedToProjectEmail(recipient : String, projectId : Int) {
		val project = Project.get(projectId);
		
		val subject = s"you have been added to the project ${project.name}"
		val content = views.html.email.emailMessage(play.twirl.api.Html(subject), "", "visit the project", s"/project/$projectId").toString;

		sendEmail(recipient, subject, content);
	}

	def sendNotificationEmail(notification : Notification) {
		val recipient = notification.username;
		notification.notificationType match {
			case NotificationType.UPDATE => {
				sendNotificationUpdateEmail(notification.username, notification.content("sender"), notification.content("project_id").toInt, notification.content("content"))
			}
			case NotificationType.MESSAGE => {
				sendNotificationMessageEmail(notification.username, notification.content("value"))
			}
			case NotificationType.REQUEST => {
				sendNotificationRequestEmail(notification.username, notification.content("sender"), notification.content("project_id").toInt)
			}

			case NotificationType.ADDED_TO_PROJECT => {
				sendNotificationAddedToProjectEmail(notification.username, notification.content("project_id").toInt)
			}
		}
	}
}