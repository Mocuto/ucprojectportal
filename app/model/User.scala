package model

import com.datastax.driver.core.{Cluster, ResultSet, ResultSetFuture, Row}
import com.github.nscala_time.time.Imports._
import constants.Cassandra._

import java.util.Date

import play.api.libs.json._
import play.api.mvc._

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

import utils._
import utils.nosql.CassieCommunicator

object User {

	final val PositionNonSG : String = "NON-SG"

	def undefined : User = {
		return User(
			isDefined = false,
			username = "",
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

		val fixedFirstName = ((firstName split(" ")).map (_.capitalize)).mkString(" ");
		val fixedLastName = ((lastName split(" ")) map (_.capitalize)) mkString(" ");

		CassieCommunicator.setUserActivated(User.get(username), hashedPassword, fixedFirstName, fixedLastName)

		val user = User.get(username);

		Notification.createMessage(user, "Welcome to the Project Portal!")
		return user;
	}

	def updateLastLogin(username : String) {
		CassieCommunicator.setUserLastLogin(User.get(username), new Date());
	}

	def all : Seq[User] = CassieCommunicator.getUsers

	def allConfirmed : Seq[User] = User.all.filter(_.position != "")

	def get(username : String) : User = {
		if (username.length == 0) {
			return User.undefined
		}
		return CassieCommunicator.getUserWithUsername(username);
	}

	def getAsync(username : String) : Future[Option[User]] = UserTable.get(username)

	def getFullName(username : String) : String = { val user = CassieCommunicator.getUserWithUsername(username); return s"${user.firstName} ${user.lastName}"}

	def create(user : User) : User =  {

		if (User.get(user.username).isDefined == false) {
			CassieCommunicator.addUser(user);
			UserGroup.NORMAL.addUser(user);			
		}

		makeActivation(user) match {
			case Some(code) => SMTPCommunicator.sendActivationEmail(user.username, code);
			case None => Predef.assert(false, "makeActivation in model.User returned None")
		}
		return user;
	}

	def createFromShibboleth(username : String) : User = {

		val user = User.get(username);
		if(User.get(username).isDefined == true) {
			return user;
		}

		//TODO: Probably generate the office hour row for this user as well
		UserTable.create(username);
		UserPrivileges.create(username);

		return User.get(username);
	}

	def setupSG(username : String, firstName : String, lastName : String, preferredPronouns : String, position : String) = {
		UserTable.setupSG(username, firstName, lastName, preferredPronouns, position);
	}

	def setupNonSG(username : String) = UserTable.setupNonSG(username);

	def verify(username : String) : Unit = {
		UserTable.verify(username);
		UserPosition.add(username, User.get(username).position)
	}

	def makeActivation(user : User) : Option[String] = {

		return Some(getActivationCode(user).getOrElse[String] {
			val uuid = java.util.UUID.randomUUID.toString;
			CassieCommunicator.addActivationCodeForUser(user, uuid);
			uuid
		});
	}

	def forgotPassword(user : User) : Option[String] = {
		CassieCommunicator.setUserForgotPassword(user);
		return Some(getActivationCode(user).getOrElse[String] {
			val uuid = java.util.UUID.randomUUID.toString;
			CassieCommunicator.addActivationCodeForUser(user, uuid);
			uuid
		});
	}

	def getActivationCode(user : User) : Option[String] = CassieCommunicator.getActivationCodeForUser(user);

	def getActivationCode(username : String) : Option[String] = CassieCommunicator.getActivationCodeForUser(User.get(username));

	def delete(username : String ) {
		val user = User.get(username);
		user.projects.foreach(project => project.removeUser(user));
		Notification.getForUser(user).foreach(notification => notification.delete())

		CassieCommunicator.removeUser(user);
	}

	implicit def fromRow(row : Row) : User = {
		row match {
			case null => return User.undefined
			case row : Row => {
				return User(
					row.getString("username"),
					row.getString("first_name"),
					row.getString("last_name"),
					row.getString("position"),
					row.getInt("unread_notifications"),
					row.getDate("last_login")
				)
			}
		}
	}
}

import com.websudos.phantom.Implicits._

sealed class UserTable extends CassandraTable[UserTable, User] {
	object username extends StringColumn(this) with PartitionKey[String]
	object first_name extends StringColumn(this)
	object last_name extends StringColumn(this)
	object last_login extends DateColumn(this)
	object last_activity extends DateColumn(this)
	object position extends StringColumn(this)
	object preferred_pronouns extends StringColumn(this)
	object primary_contact_projects extends SetColumn[UserTable, User, Int](this)
	object projects extends SetColumn[UserTable, User, Int](this)
	object unread_notifications extends IntColumn(this)
	object verified extends BooleanColumn(this)

	override def fromRow(r : Row) = User(
			username(r),
			firstName = first_name(r),
			lastName = last_name(r),
			position = position(r),
			unreadNotifications = unread_notifications(r),
			lastLogin = last_login(r),
			lastActivity = last_activity(r),
			verified = verified(r),
			isDefined = true)
}

private object UserTable extends UserTable {
	override val tableName = "users";
	implicit val session = CassieCommunicator.session

	def add(user : User) : FutureResultSet = {
		insert.value(_.username, user.username)
			.value(_.first_name, user.firstName)
			.value(_.last_name, user.lastName)
			.value(_.last_login, user.lastLogin)
			.value(_.last_activity, user.lastActivity)
			.value(_.position, user.position)
			.value(_.primary_contact_projects, Set[Int]())
			.value(_.projects, Set[Int]())
			.value(_.unread_notifications, user.unreadNotifications)
			.value(_.verified, user.verified)
			.future()
	}

	def create(username : String) = add(User(username))

	def get(username : String) : Future[Option[User]] = select.where(_.username eqs username).one()

	def setupSG(username : String, firstName : String, lastName : String, preferredPronouns : String, position : String) = {
		update
			.where(_.username eqs username)
			.modify(_.first_name setTo firstName)
			.and(_.last_name setTo lastName)
			.and(_.preferred_pronouns setTo preferredPronouns)
			.and(_.position setTo position)
			.and(_.verified setTo false)
			.onlyIf(_.position eqs "")
			.future();
	}

	def setupNonSG(username : String) = {
		update
			.where(_.username eqs username)
			.modify(_.position setTo User.PositionNonSG)
			.and(_.verified setTo true)
			.onlyIf(_.position eqs "")
			.future();
	}

	def verify(username : String) = update.where(_.username eqs username).modify(_.verified setTo true);
}

case class User (
		username : String, 
		firstName : String = "", 
		lastName : String = "", 
		position : String = "",
		unreadNotifications : Int = 0,
		lastLogin : Date = new Date(), 
		lastActivity : Date = new Date(),
		verified : Boolean = false,
		isDefined : Boolean = true) {

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

	def hasConfirmed = position != "";
}