package model

import com.datastax.driver.core.Row
import com.github.nscala_time.time.Imports._

import java.util.Date

import play.api.libs.json._
import play.api.mvc._

object User {

	def undefined : User = {
		return User(
			isDefined = false,
			username = "",
			password = ""
		)
	}

	def authenticate(username : String, password: String) : User = {
		return CassieCommunicator.getUserWithUsernameAndPassword(username, password);
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
		
		return user;
	}

	implicit def fromRow(row : Row) : User = {
		row match {
			case null => return User.undefined
			case row : Row => {
				return User(
					row.getString("username"),
					row.getString("password"),
					row.getString("first_name"),
					row.getString("last_name"),
					row.getInt("unread_notifications")
				)
			}
		}
	}
}

case class User (username : String, password : String, firstName : String = "", lastName : String = "", unreadNotifications : Int = 0, isDefined : Boolean = true) {
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

	def addToProject(project : Project) {
		CassieCommunicator.addUserToProject(this, project);
		Notification.createMessage(this, s"you have been added to the project ${project.name}!");
	}

	def removeFromProject(project : Project) {
		CassieCommunicator.removeUserFromProject(this, project);
	}

	def fullName : String = s"$firstName $lastName";
}