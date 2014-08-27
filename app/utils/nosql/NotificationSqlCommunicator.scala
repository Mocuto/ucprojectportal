package utils.nosql

import com.datastax.driver.core.{Cluster, ResultSet, ResultSetFuture, Row}
import com.datastax.driver.core.exceptions._

import java.util.Date

import model._

import scala.collection.JavaConversions._

import utils.{Conversions, PasswordHasher}

trait NotificationSqlCommunicator extends BaseSqlCommunicator {

	private[nosql] val NOTIFICATIONS : String
	private[nosql] val NOTIFICATIONS_INSERT_FIELDS : String

	def getNotificationsForUser (user : User) : Seq[Notification] = {
		val executeString = s"select * from $NOTIFICATIONS where username = '${user.username}' order by time_created desc"
		executeAsync(executeString, doesDebug = false) match {
		  case None => return List[Notification]();
		  case Some(r : ResultSetFuture) => {
		    val rows = r.getUninterruptibly().all();

		    return rows.map(row => Notification.fromRow(row));
		  }
		}
	}

	def addNotification(notification : Notification) : Notification = {
	    val timestamp = utils.Conversions.dateToStr(notification.timeCreated)
	    val contentStr = utils.Conversions.mapToStr(notification.content).replace("'", "''")
	    val typeStr = NotificationType.toStr(notification.notificationType);
	    val executeString = s"insert into $NOTIFICATIONS ($NOTIFICATIONS_INSERT_FIELDS) values ('${notification.username}', '$timestamp', $contentStr, '$typeStr')";

	    execute(executeString);

	    return notification;
	}

	def removeNotification(notification : Notification) {
		val timestamp = utils.Conversions.dateToStr(notification.timeCreated);
		val executeString = s"delete from $NOTIFICATIONS where username='${notification.username}' and time_created='$timestamp'"

		execute(executeString);
	}

}