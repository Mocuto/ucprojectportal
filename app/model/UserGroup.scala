package model

import com.datastax.driver.core.Row
import com.github.nscala_time.time.Imports._

import java.util.Date

import play.api.libs.json._
import play.api.mvc._

import scala.collection.JavaConversions._

object UserGroup {

	def undefined : UserGroup = return UserGroup("", List[String](), false)
	
	def get (name : String) : UserGroup = CassieCommunicator.getUserGroup(name);

	def isUserInGroup(user : User, name : String) : Boolean = {
		val userGroup = UserGroup.get(name);

		return userGroup.users.contains(user.username);
	}

	def addUserToGroup(user : User, name : String) {
		val userGroup = UserGroup.get(name);
		CassieCommunicator.addUserToGroup(user, userGroup);
	}

	implicit def fromRow (row : Row) : UserGroup = {
		row match {
			case null => UserGroup.undefined
			case _ => UserGroup(
				row.getString("name"),
				row.getSet("users", classOf[String]).toList,
				isDefined = true
			)
		}
	}

	def NORMAL : UserGroup = UserGroup.get("normal");
}

case class UserGroup(name : String, users : Seq[String], isDefined : Boolean = true) {
	def addUser(user : User) {
		UserGroup.addUserToGroup(user, name);
	}

}