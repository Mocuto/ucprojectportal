package model

import com.datastax.driver.core.Row
import com.github.nscala_time.time.Imports._

import java.util.Date

import play.api.libs.json._
import play.api.mvc._

import scala.collection.JavaConversions._

import utils._
import utils.nosql.CassieCommunicator

object UserGroup {

	def undefined : UserGroup = return UserGroup("", List[String](), false)

	def all : Seq[UserGroup] = return CassieCommunicator.getUserGroups
	
	def get (name : String) : UserGroup = CassieCommunicator.getUserGroup(name);

	def isUserInGroup(username : String, name : String) : Boolean = {
		val userGroup = UserGroup.get(name);

		return userGroup.users.contains(username);
	}

	def isUserInGroup(user : User, name : String) : Boolean =  isUserInGroup(user.username, name)

	def isAdmin(username : String) : Boolean = isUserInGroup(username, "admin")

	def isAdmin(user : User) : Boolean = isAdmin(user.username)

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