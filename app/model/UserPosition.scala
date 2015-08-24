package model

import com.datastax.driver.core.{Cluster, ResultSet, ResultSetFuture, Row}
import com.github.nscala_time.time.Imports._
import com.websudos.phantom.Implicits._

import java.util.Date

import utils._
import utils.nosql.CassieCommunicator

protected sealed class UserPositionTable extends CassandraTable[UserPositionTable, UserPosition] {
	object name extends StringColumn(this) with PartitionKey[String]
	object username extends StringColumn(this)
	object time_created extends DateColumn(this)

	override def fromRow(r : Row) = UserPosition(name(r), username(r), time_created(r))
}

private object UserPositionTable extends UserPositionTable {
	override val tableName = "user_positions";
	implicit val session = CassieCommunicator.session

	def all = select.fetch();

	def allUninterruptibly = scala.concurrent.Await.result(all, constants.Cassandra.defaultTimeout)

	def add(a : UserPosition) = insert
		.value(_.name, a.name)
		.value(_.username, a.username)
		.value(_.time_created, a.timeCreated)
		.future();
}

object UserPosition {
	def add(username : String, position : String) = UserPositionTable.add(UserPosition(position, username, new Date())) 

	def all = UserPositionTable.allUninterruptibly
}

case class UserPosition(name : String, username : String, timeCreated : Date)