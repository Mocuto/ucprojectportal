package utils.nosql

import com.datastax.driver.core.{Cluster, ResultSet, ResultSetFuture, Row}
import com.datastax.driver.core.exceptions._

import java.util.Date

import model._

import scala.collection.JavaConversions._

import utils.{Conversions, PasswordHasher}

trait UserSqlCommunicator extends BaseSqlCommunicator {

	private[nosql] val USERS : String
	private[nosql] val USER_INSERT_FIELDS : String

	private[nosql] val USER_GROUPS : String

	private[nosql] val USER_ACTIVATION : String
	private[nosql] val USER_ACTIVATION_INSERT_FIELDS : String

	private[nosql] val USER_AUTHENTICATION : String
	private[nosql] val USER_AUTHENTICATION_INSERT_FIELDS : String

	def setUserUnreadNotifications(user : User, num : Int) {
	    val executeString = s"update $USERS set unread_notifications = $num where username = '${user.username}'"
	    execute(executeString);
	  }

	  def setUserLastLogin(user : User, date : Date) {
	    val timestamp = utils.Conversions.dateToStr(date);
	    val executeString = s"update $USERS set last_login = '$timestamp' where username = '${user.username}'";

	    execute(executeString);
	  }

	  def setUserActivated(user : User, hashedPassword : String, firstName : String, lastName : String) {
      val cleanFirstName = firstName.replace("'", "''");
      val cleanLastName = lastName.replace("'", "''");

	    val authenticationString = s"insert into $USER_AUTHENTICATION($USER_AUTHENTICATION_INSERT_FIELDS) values ('${user.username}', '$hashedPassword')";
	    execute(authenticationString) match {
	      case None => println(s"user account activation unsuccessful for user: ${user.username}")
	      case _ => {
	        val hasConfirmedString = s"update $USERS set has_confirmed = true, first_name = '$cleanFirstName', last_name = '$cleanLastName' where username = '${user.username}'";
	        removeActivationCodeForUser(user);
	        execute(hasConfirmedString);
	      }
	    }

	  }

  def setUserForgotPassword(user : User) {
    val executeString = s"update users set has_confirmed = false where username = '${user.username}'";

    executeAsync(executeString);
  }

  def getUserWithUsernameAndPassword(username : String, password : String) : User = {
    val executeString = s"select * from $USER_AUTHENTICATION where username='$username'";
    executeAsync(executeString) match {
      case None => return User.undefined
      case Some(r : ResultSetFuture) => {
        val row = r.getUninterruptibly().one();

        row match {
          case null => return User.undefined
          case row : Row => {
            val storedPassword = row.getString("password");

            if(PasswordHasher.check(password, storedPassword) == true) {
              User.updateLastLogin(username);
              return User.get(username);
            }
            else {
              User.undefined
            }
          }
        }
      }
    }
  }

  def getUserWithUsername(username : String) : User = {
    val executeString = s"select * from $USERS where username='$username'";
    executeAsync(executeString, doesDebug = false) match {
      case None => return User.undefined;
      case Some(r : ResultSetFuture) => {
        val row = r.getUninterruptibly().one();

        User.fromRow(row);        
      }
    }
  }

  def getUsers : Seq[User] = {
    val executeString = s"select * from $USERS";
    executeAsync(executeString) match {
      case None => return List[User]();
      case Some(r : ResultSetFuture) => {
        val rows = r.getUninterruptibly().all();
        return rows.map(row => User.fromRow(row));
      }
    }
  }

  def getUserGroups : Seq[UserGroup] = {
      val executeString = s"select * from $USER_GROUPS";
      executeAsync(executeString) match {
        case None => List[UserGroup]();
        case Some(r : ResultSetFuture) => {
          val rows = r.getUninterruptibly().all();
          return rows.map(row => UserGroup.fromRow(row))
        }
      }
  }

  def getUserGroup(name : String) : UserGroup = {
    val executeString = s"select * from $USER_GROUPS where name = '$name'"
    executeAsync(executeString) match {
      case None => UserGroup.undefined;
      case Some(r : ResultSetFuture) => {
        val row = r.getUninterruptibly().one();
        return UserGroup.fromRow(row);
      }
    }
  }

  def getActivationCodeForUser(user : User) : Option[String] = {
    if (user.isDefined == false) {
      return None
    }
    val executeString = s"select code from $USER_ACTIVATION where username = '${user.username}'"
    executeAsync(executeString) match {
      case None => None;
      case Some(r : ResultSetFuture) => {
        val row = r.getUninterruptibly().one();
        if (row == null) {
          return None
        } else {
          return Some(row.getUUID("code").toString)         
        }

      }
    }
  }

	def removeUser(user : User) {
	    val username = user.username;

	    val executeString = s"delete from users where username = '$username'";
	    execute(executeString);
  	}

	def addUserToGroup(user : User, userGroup : UserGroup) {
		val executeString = s"update $USER_GROUPS set users = users + {'${user.username}'} where name = '${userGroup.name}'"
		executeAsync(executeString);
	}

	def addUser(user : User) {
		val executeString = s"insert into $USERS($USER_INSERT_FIELDS) VALUES('${user.username}', '', '', '', {}, {}, 0, false, '')"
		executeAsync(executeString);
	}

	def addActivationCodeForUser(user : User, uuid : String) {
		val executeString = s"insert into $USER_ACTIVATION($USER_ACTIVATION_INSERT_FIELDS) values('${user.username}', $uuid)";
		execute(executeString)
	}

	def removeActivationCodeForUser(user : User) {
		val executeString = s"delete from $USER_ACTIVATION where username = '${user.username}'"
		executeAsync(executeString);
	}
	
}