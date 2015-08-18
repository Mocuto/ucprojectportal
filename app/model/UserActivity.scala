package model

import com.websudos.phantom.Implicits._
import com.datastax.driver.core.{Cluster, ResultSet, ResultSetFuture, Row}

import enums.ActivityType
import enums.ActivityType._

import java.util.Date

import scala.collection.Map

import scala.concurrent._

import utils._
import utils.nosql._

sealed protected class UserActivityTable extends CassandraTable[UserActivityTable, UserActivity] {
	object username extends StringColumn(this) with PartitionKey[String]
	object activity_type extends StringColumn(this) with PrimaryKey[String]
	object time_submitted extends DateColumn(this) with PrimaryKey[Date]
	object activity_detail extends MapColumn[UserActivityTable, UserActivity, String, String](this)

	override def fromRow(r : Row) : UserActivity = UserActivity(
		username = username(r),
		activityType = ActivityType.fromString(activity_type(r)),
		timeSubmitted = time_submitted(r),
		detail = activity_detail(r),
		isDefined = true
	)
}

object UserActivityTable extends UserActivityTable {
	override val tableName = "user_activity";
	implicit val session = CassieCommunicator.session

	def add(username : String, activityType : ActivityType, detail : scala.collection.Map[String,String]) : UserActivity = {
		val activity = UserActivity(username, activityType, detail = detail)
		insert
			.value(_.username, username)
			.value(_.activity_type, ActivityType.toString(activityType))
			.value(_.time_submitted, activity.timeSubmitted)
			.value(_.activity_detail, detail.toMap)
			.future()

		activity
	}

	def get(username : String) : Future[Seq[UserActivity]] = select
		.where(_.username eqs username)
		.orderBy(_.time_submitted.desc)
		.fetch()

	def getLimited(username : String, limit : Int) : Future[Seq[UserActivity]] = select
		.where(_.username eqs username)
		.orderBy(_.time_submitted.desc)
		.limit(limit)
		.fetch()

	def getUninterruptibly(username : String) : Seq[UserActivity] = scala.concurrent.Await.result(get(username), constants.Cassandra.defaultTimeout)

	def getLimitedUninterruptibly(username : String, limit : Int) : Seq[UserActivity] = scala.concurrent.Await.result(
		getLimited(username, limit),
		constants.Cassandra.defaultTimeout
	)
}

object UserActivity {
	def add(username : String, activityType : ActivityType, detail : Map[String, String]) = UserActivityTable.add(username, activityType, detail)

	def get(username : String, max : Int, activityTypes : Seq[ActivityType]) : Seq[UserActivity] = {
		(((UserActivityTable getLimitedUninterruptibly (username, 1000)) filter( (x: UserActivity) => activityTypes contains x.activityType )) take max)
	}

	def getNeutralized(username : String, max : Int, activityTypes : Seq[ActivityType]) : Seq[UserActivity] = {
		((((neutralize 
				(UserActivityTable getLimitedUninterruptibly (username, 1000))) 
			filter( (x: UserActivity) => activityTypes contains x.activityType ))) take max)
	}

	def neutralize(arr : Seq[UserActivity]) : Seq[UserActivity] = {

		type FoldType = ((ActivityType, Int), UserActivity)

		val startVal = ((ActivityType.LikeProject, -1), UserActivity(username = "", activityType = ActivityType.LikeProject, timeSubmitted = new Date(Long.MinValue)))

		val removals = scala.collection.mutable.MutableList[UserActivity]();
		val likeMap = arr
			.filter( (x : UserActivity) => ActivityType.Toggleables contains(x.activityType))
			.groupBy ( (x : UserActivity) => (x.activityType, Predef.augmentString(x.detail("project-id")) toInt))
			.map( { case (key, seq) => seq.foldLeft(startVal)({ (z : FoldType, a : UserActivity) =>
					if(a.timeSubmitted.after(z._2.timeSubmitted)) {
						(key, a)
					}
					else {
						removals += a;
						(key, z._2)
					}
				})
			})

		println(likeMap)

		for( ((activityType, projectId), activity) <- likeMap if activityType == ActivityType.LikeProject || activityType == ActivityType.LikeUpdate || activityType == ActivityType.FollowProject ) {
			val antiType = ActivityType.invert(activityType)

			if(likeMap contains (antiType, projectId) ) {
				if( likeMap(antiType, projectId).timeSubmitted after(activity.timeSubmitted) ) {
					removals += activity
				}
			}
		}
		arr diff removals
	}
}

case class UserActivity(
	username : String,
	activityType : ActivityType,
	timeSubmitted : Date = new Date(),
	detail : Map[String, String] = Map.empty[String, String],
	isDefined : Boolean = true)