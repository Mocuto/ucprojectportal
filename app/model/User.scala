package model

import com.datastax.driver.core.{Cluster, ResultSet, ResultSetFuture, Row}
import com.github.nscala_time.time.Imports._
import constants.Cassandra._

import java.io.File
import java.nio.file.{Paths, Files}
import java.util.Date

import play.api.libs.json._
import play.api.mvc._
import play.api.mvc.MultipartFormData._
import play.api.libs.Files._

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

	def all : Seq[User] = UserTable.allUninterruptibly//CassieCommunicator.getUsers

	def allConfirmed : Seq[User] = User.all.filter(_.position != "")

	def get(username : String) : User = {
		if (username.length == 0) {
			return User.undefined
		}
		return UserTable.getUninterruptibly(username).getOrElse(User(username, isDefined = false))
		//return CassieCommunicator.getUserWithUsername(username);
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

	def createFromShibboleth(username : String, firstName : String, lastName : String) : User = {

		val user = User.get(username);
		if(User.get(username).isDefined == true) {
			return user;
		}

		//TODO: Probably generate the office hour row for this user as well
		UserTable.create(username, firstName, lastName);
		UserPrivileges.create(username);

		return User.get(username);
	}

	def setupSG(username : String, firstName : String, lastName : String, preferredPronouns : String, position : String, officeHourRequirement : Double, cellNumber : String) = {
		UserTable.setupSG(username, firstName, lastName, preferredPronouns, position, officeHourRequirement, cellNumber);
	}

	def setupNonSG(username : String) = UserTable.setupNonSG(username);

	def verifyWithPosition(username : String, position : String) : Unit = {
		UserTable.verifyWithPosition(username, position);
		UserPosition.add(username, User.get(username).position)
	}

	def emeritus(username : String, value : Boolean) : Unit = {
		UserTable.emeritus(username, value);

		if(value == true) { //Freeze all their projects
			Project.getPrimaryForUsername(username).map(p => {
			
				if(p.state == ProjectState.IN_PROGRESS || p.state == ProjectState.IN_PROGRESS_NEEDS_HELP) {
					Project.freeze(p)
				}
			})
		}
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
		user.projects.map(Project.get(_ : Int)).foreach(project => project.removeUser(user));
		Notification.getForUser(user).foreach(notification => notification.delete())

		CassieCommunicator.removeUser(user);
	}

	def setProfilePic(username : String, temporaryFile : (String, TemporaryFile)) : String = {
		val uuid = java.util.UUID.randomUUID.toString;

		println(temporaryFile)

		val originalName = temporaryFile._1
		val filename = uuid + "--" + temporaryFile._1

		if(Files.exists(Paths.get(constants.Directories.Root, constants.Directories.Uploads)) == false) {
			val uploadsDir = new File(Paths.get(constants.Directories.Root, constants.Directories.Uploads).toString);
			uploadsDir.mkdir();
		} 

		val path = Paths.get(constants.Directories.Root, constants.Directories.Uploads, filename).toString;

		val file = new File(path);
		
	    temporaryFile._2.moveTo(file, true);

	    UserTable.setProfilePic(username, "/" + Paths.get(constants.Directories.Uploads, filename).toString)

	    return "/" + Paths.get(constants.Directories.Uploads, filename).toString;
	}

	def updateLastActivity(username : String, date : Date) = UserTable.updateLastActivity(username, date)


	def addFollower(username : String, follower : String) : Unit = {
		addUserToFollow(follower, username)
		UserTable.addFollower(username, follower)
	}

	def removeFollower(username : String, follower : String) : Unit = {
		removeUserToFollow(follower, username)
		UserTable.removeFollower(username, follower)
	}

	def addProjectToFollow(username : String, projectId : Int) = UserTable.addProjectToFollow(username, projectId);

	def removeProjectToFollow(username : String, projectId : Int) = UserTable.removeProjectToFollow(username, projectId);

	def addUserToFollow(username : String, toFollow : String) = UserTable.addUserToFollow(username, toFollow)

	def removeUserToFollow(username : String, toFollow : String) = UserTable.removeUserToFollow(username, toFollow)

	implicit def fromRow(row : Row) : User = {
		row match {
			case null => return User.undefined
			case row : Row => {
				return User(
					username = row.getString("username"),
					firstName = row.getString("first_name"),
					lastName = row.getString("last_name"),
					position = row.getString("position"),
					preferredPronouns = row.getString("preferred_pronouns"),
					profile = Some(row.getString("profile")),
					unreadNotifications = row.getInt("unread_notifications"),
					lastLogin = row.getDate("last_login")
				)
			}
		}
	}
}

import com.websudos.phantom.Implicits._

sealed class UserTable extends CassandraTable[UserTable, User] {

	object username extends StringColumn(this) with PartitionKey[String]
	object first_name extends StringColumn(this)
	object followers extends SetColumn[UserTable, User, String](this)
	object last_activity extends DateColumn(this)
	object last_login extends DateColumn(this)
	object last_name extends StringColumn(this)
	object cell_number extends OptionalStringColumn(this)
	object office_hour_requirement extends DoubleColumn(this)
	object position extends StringColumn(this)
	object preferred_pronouns extends StringColumn(this)
	object primary_contact_projects extends SetColumn[UserTable, User, Int](this)
	object profile extends OptionalStringColumn(this)
	object projects extends SetColumn[UserTable, User, Int](this)
	object projects_following extends SetColumn[UserTable, User, Int](this)
	object unread_notifications extends IntColumn(this)
	object users_following extends SetColumn[UserTable, User, String](this)
	object verified extends BooleanColumn(this)
	object emeritus_ extends BooleanColumn(this) {
		override val name = "emeritus"
	}

	override def fromRow(r : Row) = User(
			username(r),
			firstName = first_name(r),
			lastName = last_name(r),
			cellNumber = cell_number(r),
			officeHourRequirement = office_hour_requirement(r),
			position = position(r),
			preferredPronouns = preferred_pronouns(r),
			profile = profile(r),
			primaryContactProjects = primary_contact_projects(r).toSeq,
			projects = projects(r).toSeq,
			projectsFollowing = projects_following(r).toSeq,
			unreadNotifications = unread_notifications(r),
			lastLogin = last_login(r),
			lastActivity = last_activity(r),
			verified = verified(r),
			emeritus = emeritus_(r),
			usersFollowing = users_following(r).toSeq,
			followers = followers(r).toSeq,
			isDefined = true
		)
}

object UserTable extends UserTable {
	override val tableName = "users";
	implicit val session = CassieCommunicator.session

	def add(user : User) : FutureResultSet = {
		insert.value(_.username, user.username)
			.value(_.first_name, user.firstName)
			.value(_.last_name, user.lastName)
			.value(_.last_login, user.lastLogin)
			.value(_.last_activity, user.lastActivity)
			//.value(_.primary_contact_projects, user.primaryContactProjects.toSet)
			//.value(_.projects, user.projects.toSet)
			.value(_.emeritus_, user.emeritus)
			.value(_.position, user.position)
			.value(_.preferred_pronouns, user.preferredPronouns)
			.value(_.primary_contact_projects, Set[Int]())
			.value(_.profile, user.profile)
			.value(_.projects, Set[Int]())
			.value(_.projects_following, Set[Int]())
			.value(_.unread_notifications, user.unreadNotifications)
			.value(_.users_following, user.usersFollowing.toSet)
			.value(_.verified, user.verified)
			.value(_.office_hour_requirement, user.officeHourRequirement)
			.value(_.cell_number, user.cellNumber)
			.future()
	}

	def create(username : String, firstName : String, lastName : String) = add(User(username, firstName = firstName, lastName = lastName))

	def all = select.fetch()

	def allUninterruptibly = scala.concurrent.Await.result(all, constants.Cassandra.defaultTimeout)

	def get(username : String) : Future[Option[User]] = select.where(_.username eqs username).one()

	def getUninterruptibly(username : String) = scala.concurrent.Await.result(get(username), constants.Cassandra.defaultTimeout)

	def setupSG(
		username : String,
		firstName : String,
		lastName : String,
		preferredPronouns : String,
		position : String,
		officeHourRequirement : Double,
		cellNumber : String) = {

		update
			.where(_.username eqs username)
			.modify(_.first_name setTo firstName)
			.and(_.last_name setTo lastName)
			.and(_.preferred_pronouns setTo preferredPronouns)
			.and(_.position setTo position)
			.and(_.verified setTo false)
			.and(_.office_hour_requirement setTo officeHourRequirement)
			.and(_.cell_number setTo Some(cellNumber))
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

	def verifyWithPosition(username : String, position : String) = update
		.where(_.username eqs username)
		.modify(_.verified setTo true)
		.and(_.position setTo position)
		.future();

	def emeritus(username : String, value : Boolean) = update.where(_.username eqs username).modify(_.emeritus_ setTo value).future();

	def setProfilePic(username : String, path : String) = update.where(_.username eqs username).modify(_.profile setTo Some(path)).future();

	def updateLastActivity(username : String, date : Date) = update.where(_.username eqs username).modify(_.last_activity setTo date).future();

	def addFollower(username : String, follower : String) = update.where(_.username eqs username).modify(_.followers add follower).future()
	def removeFollower(username : String, follower : String) = update.where(_.username eqs username).modify(_.followers remove follower).future();

	def addProjectToFollow(username : String, id : Int) = update.where(_.username eqs username).modify(_.projects_following add id).future();
	def removeProjectToFollow(username : String, id : Int) = update.where(_.username eqs username).modify(_.projects_following remove id).future();

	def addUserToFollow(username : String, toFollow : String) = update.where(_.username eqs username).modify(_.users_following add toFollow).future();
	def removeUserToFollow(username : String, toFollow : String) = update.where(_.username eqs username).modify(_.users_following remove toFollow).future();
}

case class User (
		username : String, 
		firstName : String = "", 
		lastName : String = "",
		position : String = "",
		preferredPronouns : String = "",
		profile : Option[String] = None,
		projects : Seq[Int] = List[Int](),
		projectsFollowing : Seq[Int] = List[Int](),
		primaryContactProjects : Seq[Int] = List[Int](),
		unreadNotifications : Int = 0,
		lastLogin : Date = new Date(), 
		lastActivity : Date = new Date(),
		verified : Boolean = false,
		emeritus : Boolean = false,
		usersFollowing : Seq[String] = List[String](),
		followers : Seq[String] = List[String](),
		officeHourRequirement : Double = 0.0,
		cellNumber : Option[String] = None,
		isDefined : Boolean = true) {

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

	def initials = (firstName, lastName) match {
		case ("", "") => ""
		case ("", _) => (_ : String).substring(0, 1)
		case (_, "") => (_ : String).substring(0, 1)
		case (f, l) => (f.substring(0, 1) + l.substring(0, 1)).toUpperCase
	}

	def hasConfirmed = position != "";
}