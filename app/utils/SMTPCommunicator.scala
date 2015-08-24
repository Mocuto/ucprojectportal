package utils

import play.api.libs.mailer._

import enums._
import enums.ProjectActivityStatus._

import javax.inject._

import model._

import play.api.Play.current
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.libs.json._
import play.api.Logger

import scala.concurrent.ExecutionContext.Implicits.global

import views._


object SMTPCommunicator {


	private val host = constants.ServerSettings.ChosenHost;
	//private val mail = use[MailerPlugin].email

	private val MAX_TRIES = 5;

	def sendEmail(recipient : String, subject : String, content : String) {

		if(User.get(recipient).emeritus == true) {
			println("Stopped emeritus user from receiving email")
			return;
		}

		var tries = 0;
		var wasSent : Boolean = false;
		val recipientEmail = if (UserGroup.isUserInGroup(recipient, "faculty/staff"))
		 	{
				recipient + "@ucmail.uc.edu" 
			} 
			else { 
				recipient + "@mail.uc.edu" 
			}
			
		

		Logger.info(s"recipient: $recipientEmail")
		Logger.info(s"subject: $subject")
		Logger.info(s"content: $content");

		while(tries < MAX_TRIES && wasSent == false) {
			try {
				tries += 1;
				val email = Email(
			      subject,
			      s"Project Portal <noreply@${constants.ServerSettings.ProductionUrl}>",
			      Seq(s"${User.getFullName(recipient)} <$recipientEmail>"),
			      // adds attachment
			      //attachments = Seq(
			        //AttachmentFile("attachment.pdf", new File("/some/path/attachment.pdf")),
			        // adds inline attachment from byte array
			        //AttachmentData("data.txt", "data".getBytes, "text/plain", Some("Simple data"), Some(EmailAttachment.INLINE))
			      //),
			      // sends text, HTML or both...
			      bodyHtml = Some(content)
			    )

			    MailerPlugin.send(email)

				//mail.setSubject(subject)
				//mail.setRecipient(s"${User.getFullName(recipient)} <$recipientEmail>", s"$recipientEmail")
				//or use a list
				//mail.setFrom(s"Project Portal <noreply@ucprojectportal.com>")
				//sends html
				//mail.sendHtml(content);
				wasSent = true;
			}
			catch {
				case e : Exception => {
					println(e)
					Logger.error(s"Exception with sendEmail", e);
					Thread.sleep(1000)
				}
			}
		}

		if(wasSent == false) {
			Logger.error("The email coud not be sent:")
			Logger.error(recipient)
			Logger.error(subject)
			Logger.error(content);
		}
	}

	def sendAllVerifyUserEmail(username : String) : Unit = {
		val privilege = UserPrivileges.Edit("", userPermissions = true);
		(UserPrivilegesEdit.whoMatchOrExceed(privilege)) onSuccess {
			case usernames => usernames.map(x => sendVerifyUserEmail(x.username, username))
		}
	}

	def sendVerifyUserEmail(to : String, about : String) : Unit = {
		val subject = "Verify New User Account"
		val content = views.html.email.emailPrivileges(User.get(about)).toString
		Logger.info(s"Verify email sent to $to, about $about")
		sendEmail(to, subject, content);
	}

	def sendActivationEmail(recipient : String, uuid : String) {
		val subject = "Activate Your Project Portal Account";
		val content = views.html.email.emailActivation(User.get(recipient), uuid).toString;

		sendEmail(recipient, subject, content);
	}

	def sendForgotPasswordEmail(recipient : String, uuid : String) {
		val subject = "Reset Your Project Portal Password";
		val content = views.html.email.emailResetPassword(User.get(recipient), uuid).toString;

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
		
		val subject = s"You Have Been Added to the Project: ${project.name}"
		val content = views.html.email.emailMessage(play.twirl.api.Html(subject), "", "visit the project",
		 				constants.ServerSettings.HostUrl + controllers.routes.ProjectController.project(projectId)).toString;

		sendEmail(recipient, subject, content);
	}

	def sendNotificationFrozenProject(recipient : String, projectId : Int) {

		val project = Project.get(projectId)

		val subject = constants.Messages.ProjectFrozenInactivity.split(" ").map(_.capitalize).mkString(" ")

		val content = views.html.email.emailProjectFrozen(project).toString

		sendEmail(recipient, subject, content)
	}

	def sendNotificationOfficeHourFailed(recipient : String, failure : String) {

		val subject = s"${User.get(failure).fullName} failed to do their office hours on time!"

		val content = views.html.email.emailMessage(
			play.twirl.api.Html(subject),
			"this user did not do their required amount of office hours",
			"view user profile",
			controllers.routes.UserController.user(failure).toString).toString

		sendEmail(recipient, subject, content)
	}

	def sendDigestEmail(
		recipient : String,
		numberOfProjects : Int,
		hotProjectsToCount : Seq[(Project, Int)],
		hotContributorsToCount : Seq[(User, Int)],
		projectsForTemperature : Map[ProjectActivityStatus, Seq[Project]],
		newProjects : Seq[Project],
		completedProjects : Seq[Project],
		userProjectsForTemperature : Map[ProjectActivityStatus, Seq[Project]],
		userProjectsFollowingForTemperature : Map[ProjectActivityStatus, Seq[Project]]
		) : Unit = {

		val subject = "Your Project Portal Weekly Digest!"


		def percentagize(p : Map[ProjectActivityStatus, Seq[Project]]) : Map[ProjectActivityStatus, Int] = {
			val temp = p
					.map({case ((temperature, projects)) => (temperature, ((projects.length.toDouble / numberOfProjects) * 100).toInt)})

			val sum = utils.Conversions.sumList(temp.values.toList)
			if(sum < 100 && temp.size > 0) {
				temp + (temp.keys.toList(0) -> (temp(temp.keys.toList(0)) + (100 - sum)))
			}
			else {
				temp
			}
		}

		val percentagesForStatus = percentagize(projectsForTemperature)
		val userPercentagesForStatus = percentagize(userProjectsForTemperature)
		val userPercentagesForFollowingStats = percentagize(userProjectsFollowingForTemperature)

		val content = views.html.email.emailDigest(
			User.get(recipient),
			numberOfProjects,
			hotProjectsToCount,
			hotContributorsToCount,
			projectsForTemperature,
			percentagesForStatus,
			newProjects,
			completedProjects,
			userPercentagesForStatus,
			userPercentagesForFollowingStats,
			userProjectsFollowingForTemperature).toString

		sendEmail(recipient, subject, content);
	}

	def sendProjectLikedEmail(projectId : Int) : Unit = {

		val project = Project.get(projectId)

		if(project.likes.length == 0) {
			return;
		}

		def convertUsername(username : String) = play.twirl.api.Html(views.html.links.userLinkEmail(User.get(username)).toString).toString

		val phrase = if(project.likes.length == 1) convertUsername(project.likes(0)) else {
			(project.likes.take(project.likes.length - 1).map(convertUsername(_)).mkString(", ")) +
			(" and" + (project.likes.drop(project.likes.length - 1).map(convertUsername(_)).mkString("")))
		} 

		val subject = constants.Messages.capitalize(constants.Messages.ProjectLiked)
		val content = views.html.email.emailLikeProject(project, phrase).toString;

		project.teamMembers.foreach(recipient => sendEmail(recipient, subject, content))

	}

	def sendUpdateLikedEmail(projectId : Int, author : String, timeSubmitted : java.util.Date) : Unit = {

		val update = ProjectUpdate.getLatest(projectId, author, timeSubmitted)

		if(update.likes.length == 0) {
			return;
		}

		def convertUsername(username : String) = play.twirl.api.Html(views.html.links.userLinkEmail(User.get(username)).toString).toString

		val phrase = if(update.likes.length == 1) convertUsername(update.likes(0)) else {
			(update.likes.take(update.likes.length - 1).map(convertUsername(_)).mkString(", ")) +
			(" and" + (update.likes.drop(update.likes.length - 1).map(convertUsername(_)).mkString("")))
		} 

		val subject = constants.Messages.capitalize(constants.Messages.UpdateLiked)
		val content = views.html.email.emailLikeUpdate(update, Project.get(projectId), phrase).toString;

		sendEmail(update.author, subject, content)
		println(content)

	}

	def sendOfficeHourDigestEmail(officeHourTuples : Seq[(User, Double)], amountOfFailures : Int) : Unit = {
		val subject = constants.Messages.capitalize(constants.Messages.OfficeHourDigest)
		val content = views.html.email.emailOfficeHourDigest(officeHourTuples, amountOfFailures).toString;


		UserPrivilegesView.allUninterruptibly.filter(_.admin).foreach((v : UserPrivileges.View) => sendEmail( v.username, subject, content))

	}


	def sendNotificationEmail(notification : Notification) : Unit = {
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

			case NotificationType.ProjectFrozen => sendNotificationFrozenProject(recipient, notification.content("project_id").toInt)

			case NotificationType.OfficeHourFailed => sendNotificationOfficeHourFailed(recipient, notification.content("sender"))

			case _ => {}
		}
	}
}