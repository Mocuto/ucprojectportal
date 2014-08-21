package model

import com.datastax.driver.core.Row
import com.github.nscala_time.time.Imports._

import java.util.Date

import play.api.libs.json._
import play.api.mvc._

import utils._

object User {

	def undefined : User = {
		return User(
			isDefined = false,
			username = "",
			hasConfirmed = false,
			firstName = "no",
			lastName = "one"
		)
	}

	def authenticate(username : String, password: String) : User = {

		return CassieCommunicator.getUserWithUsernameAndPassword(username, password);
	}

	def activate(username: String, firstName: String, lastName : String, password : String) : User = {
		val salt = java.util.UUID.randomUUID.toString;
		val hashedPassword = PasswordHasher.hash(password, salt);

		val fixedFirstName = firstName.split(" ").map(x => x.capitalize).mkString(" ");
		val fixedLastName = lastName.split(" ").map(x => x.capitalize).mkString(" ");

		CassieCommunicator.setUserActivated(User.get(username), hashedPassword, fixedFirstName, fixedLastName)

		val user = User.get(username);

		Notification.createMessage(user, "Welcome to the Project Portal!")
		return user;
	}

	def updateLastLogin(username : String) {
		CassieCommunicator.setUserLastLogin(User.get(username), new Date());
	}

	def all : Seq[User] = CassieCommunicator.getUsers

	def get(username : String) : User = {
		if (username.length == 0) {
			return User.undefined
		}
		return CassieCommunicator.getUserWithUsername(username);
	}

	def getFullName(username : String) : String = { val user = CassieCommunicator.getUserWithUsername(username); return s"${user.firstName} ${user.lastName}"}

	def create(user : User) : User =  {
		CassieCommunicator.addUser(user);
		UserGroup.NORMAL.addUser(user);
		makeActivation(user) match {
			case Some(code) => SMTPCommunicator.sendActivationEmail(user.username, code);
			case None => Predef.assert(false, "makeActivation in model.User returned None")
		}
		return user;
	}

	def makeActivation(user : User) : Option[String] = {
		if (user.hasConfirmed) {
			return None;
		}

		return Some(getActivationCode(user).getOrElse[String] {
			val uuid = java.util.UUID.randomUUID.toString;
			CassieCommunicator.addActivationCodeForUser(user, uuid);
			uuid
		});
	}

	def getActivationCode(user : User) : Option[String] = CassieCommunicator.getActivationCodeForUser(user);

	def getActivationCode(username : String) : Option[String] = CassieCommunicator.getActivationCodeForUser(User.get(username));

	implicit def fromRow(row : Row) : User = {
		row match {
			case null => return User.undefined
			case row : Row => {
				return User(
					row.getString("username"),
					row.getString("first_name"),
					row.getString("last_name"),
					row.getInt("unread_notifications"),
					row.getBool("has_confirmed"),
					row.getDate("last_login")
				)
			}
		}
	}
}

case class User (username : String, firstName : String = "", lastName : String = "", unreadNotifications : Int = 0,
				hasConfirmed : Boolean = false, lastLogin : Date = new Date(), isDefined : Boolean = true) {
	def projects : Seq[Project] = Project.get(username);
	def primaryProjects : Seq [Project] = Project.getPrimaryForUsername(username);
	implicit def toJson : JsObject = {
		return JsObject(
			Seq(
				"username" -> JsString(username.toLowerCase()),
				"firstName" -> JsString(firstName.toLowerCase()),
				"lastName" -> JsString(lastName.toLowerCase()),
				"unreadNotifications" -> JsNumber(unreadNotifications)
			)
		)
	}

	def fullName : String = s"$firstName $lastName";
}