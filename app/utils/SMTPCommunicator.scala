package utils

import com.typesafe.plugin._
import play.api.Play.current
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.libs.json._
import views._

object SMTPCommunicator {
	val mail = use[MailerPlugin].email
	def sendEmail() {
		mail.setSubject("mailer")
		mail.setRecipient("Test Recipient <akomolot@mail.uc.edu>","akomolot@mail.uc.edu")
		//or use a list
		mail.setFrom("Project Portal <noreply@ucprojectportal.com>")
		//sends html
		mail.sendHtml(views.html.emailMessage("This is a test message too!").toString);
		//sends text/text
		//mail.send( "text" )
		//sends both text and html
		//mail.send( "text", "<html>html</html>")
	}
}