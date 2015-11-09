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

sealed protected class ActivityTable extends CassandraTable[ActivityTable, Activity] {
	object id extends UUIDColumn(this) with PartitionKey[java.util.UUID]
	object activity_type extends StringColumn(this) with PrimaryKey[String] with ClusteringOrder[String] with Ascending
	object activity_detail extends MapColumn[ActivityTable, Activity, String, String](this)
	object time_submitted extends DateColumn(this) with PrimaryKey[Date] with ClusteringOrder[Date] with Descending
	object username extends StringColumn(this) with PrimaryKey[String] with ClusteringOrder[String] with Ascending

	override def fromRow(r : Row) : Activity = Activity(
		id = id(r),
		username = username(r),
		activityType = ActivityType.fromString(activity_type(r)),
		timeSubmitted = time_submitted(r),
		detail = activity_detail(r),
		isDefined = true
	)
}

object ActivityTable extends ActivityTable {
	override val tableName = "activity";
	implicit val session = CassieCommunicator.session

	def all = select.fetch();

	def allLimited(limit : Int) = select.limit(limit).fetch()

	def allUninterruptibly(limit : Int) = scala.concurrent.Await.result(allLimited(limit), constants.Cassandra.defaultTimeout)

	def allUninterruptibly = scala.concurrent.Await.result(select.fetch(), constants.Cassandra.defaultTimeout)

	def add(id : java.util.UUID, timeSubmitted : Date, activityType : ActivityType, username : String, detail : Map[String, String]) = {
		insert
			.value(_.id, id)
			.value(_.time_submitted, timeSubmitted)
			.value(_.activity_type, ActivityType.toString(activityType))
			.value(_.username, username)
			.value(_.activity_detail, detail.toMap)
			.future();

		Activity(id, timeSubmitted, activityType, username, detail);
	}

}

object Activity {

	def all : Seq[Activity] = ActivityTable.allUninterruptibly.sortWith((a, b) => b.timeSubmitted.after(a.timeSubmitted))

	def get(limit : Int) : Seq[Activity] = {
		ActivityTable.allUninterruptibly(limit)
	}

	def add(activityType : ActivityType, username : String, detail : Map[String, String]) : Activity = {
		val date = new Date();


		add(activityType, username, detail, date)
	}

	def add(activityType : ActivityType, username : String, detail : Map[String, String], date : Date) : Activity = {
		val uuid = java.util.UUID.randomUUID

		ActivityTable.add(uuid, date, activityType, username, detail)
	}
}

case class Activity(
	id : java.util.UUID,
	timeSubmitted : Date = new Date(),
	activityType : ActivityType,
	username : String,
	detail : Map[String, String] = Map.empty[String, String],
	isDefined : Boolean = true)