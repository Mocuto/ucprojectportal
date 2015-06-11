package utils.nosql

import java.util.UUID
import com.websudos.phantom.iteratee.Iteratee

import scala.concurrent.{ Future => ScalaFuture }

import org.joda.time.DateTime

import com.datastax.driver.core.{ ResultSet, Row }

import com.websudos.phantom.dsl._

import com.twitter.conversions.time._

import scala.concurrent.Await
import scala.concurrent.duration._

case class UserOfficeHour(
  username: String,
  date: String,
  projectId: Int,
  log: Map[String, Double]
)

sealed class UserOfficeHours extends CassandraTable[UserOfficeHours, UserOfficeHour] {
  object date extends StringColumn(this) with PrimaryKey[String]
  object username extends StringColumn(this) with PrimaryKey[String]
  object projectId extends IntColumn(this) with PrimaryKey[Int]

  object log extends MapColumn[UserOfficeHours, UserOfficeHour, String, Double](this)

  def fromRow(row: Row): UserOfficeHour = {
    UserOfficeHour(
      username(row),
      date(row),
      projectId(row),
      log(row)
    )
  }
}


object UserOfficeHours extends UserOfficeHours with BasicConnector {
  override lazy val tableName = "user_office_hours"

  def insertNewRecord(officeHour: UserOfficeHour): ScalaFuture[ResultSet] = {
    insert.value(_.username, officeHour.username)
      .value(_.date, officeHour.date)
      .value(_.projectId, officeHour.projectId)
      .value(_.log, officeHour.log)
      .future()
  }

  def updateRecord(officeHour: UserOfficeHour): ScalaFuture[ResultSet] = {
    update.where(_.username eqs officeHour.username)
      .and(_.date eqs officeHour.date)
      .and(_.projectId eqs officeHour.projectId)
      .modify(_.log put officeHour.log.head)
      .future()
  }

  def getUserOfficeHours(username: String): ScalaFuture[Seq[UserOfficeHour]] = {
    select.where(_.username eqs username).fetch()
  }

  def getHoursForDate(date: String): ScalaFuture[Seq[UserOfficeHour]] = {
    select.where(_.date eqs date).fetch()
  }

  def getUserHoursForDate(username: String, date: String): ScalaFuture[Seq[UserOfficeHour]] = {
    select.where(_.username eqs username).and(_.date eqs date).fetch()
  }

  def getUserOfficeHoursForDateAndProject(username: String, date: String, projectId: Int): ScalaFuture[Seq[UserOfficeHour]] = {
    select.where(_.username eqs username).and(_.date eqs date).and(_.projectId eqs projectId).fetch()
  }
}
