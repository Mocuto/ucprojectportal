package model

import java.util.Date
import java.util.UUID
import java.text.SimpleDateFormat

import com.github.nscala_time.time.Imports._
import com.websudos.phantom.iteratee.Iteratee

import scala.concurrent.{ Future => ScalaFuture }

import org.joda.time.{Days, Weeks, Interval}

import com.datastax.driver.core.{ ResultSet, Row }

import com.websudos.phantom.Implicits._

import com.twitter.conversions.time._

import scala.concurrent.Await
import scala.concurrent.duration._

sealed class UserOfficeHourTable extends CassandraTable[UserOfficeHourTable, UserOfficeHour] {
  object date extends DateColumn(this) with PrimaryKey[Date]
  object username extends StringColumn(this) with PartitionKey[String]
  object project_id extends IntColumn(this) with PrimaryKey[Int]

  object log extends MapColumn[UserOfficeHourTable, UserOfficeHour, String, Double](this)

  def fromRow(row: Row): UserOfficeHour = {
    UserOfficeHour(
      username(row),
      date(row),
      project_id(row),
      log(row)
    )
  }
}


object UserOfficeHourTable extends UserOfficeHourTable {
  override lazy val tableName = "user_office_hours"

  implicit val session = utils.nosql.CassieCommunicator.session

  def add(officeHour: UserOfficeHour): ScalaFuture[ResultSet] = {
    insert.value(_.username, officeHour.username)
      .value(_.date, officeHour.date)
      .value(_.project_id, officeHour.projectId)
      .value(_.log, officeHour.log)
      .future()
  }

  def edit(officeHour: UserOfficeHour): ScalaFuture[ResultSet] = {
    update.where(_.username eqs officeHour.username)
      .and(_.date eqs officeHour.date)
      .and(_.project_id eqs officeHour.projectId)
      .modify(_.log put officeHour.log.head)
      .future()
  }

  def all = select.fetch()

  def get(username: String): ScalaFuture[Seq[UserOfficeHour]] = {
    select.where(_.username eqs username).fetch()
  }

  def get(username: String, date: Date): ScalaFuture[Seq[UserOfficeHour]] = {
    select.where(_.username eqs username).and(_.date eqs date).fetch()
  }

  def get(username: String, date: Date, projectId: Int): ScalaFuture[Option[UserOfficeHour]] = {
    select.where(_.username eqs username).and(_.date eqs date).and(_.project_id eqs projectId).one()
  }

  def getInRange(username : String, startDate : Date, endDate : Date) : ScalaFuture[Seq[UserOfficeHour]] = {
    select.where(_.username eqs username)
      .and(_.date gte startDate)
      .and(_.date lte endDate)
      .fetch()
  }

  def allUninterruptibly = scala.concurrent.Await.result(all, constants.Cassandra.defaultTimeout)

  def getUninterruptibly(username : String) = scala.concurrent.Await.result(get(username), constants.Cassandra.defaultTimeout)
  def getUninterruptibly(username: String, date: Date) = scala.concurrent.Await.result(get(username, date), constants.Cassandra.defaultTimeout)
  def getUninterruptibly(username: String, date: Date, projectId: Int) = scala.concurrent.Await.result(get(username, date, projectId), constants.Cassandra.defaultTimeout)

  def getInRangeUninterruptibly(username : String, startDate : Date, endDate : Date) = scala.concurrent.Await.result(
      getInRange(username, startDate, endDate),
      constants.Cassandra.defaultTimeout
  )
}

object UserOfficeHour {

  private val dateFormatter = new SimpleDateFormat("MM/dd/yyyy")

  def add(officeHour : UserOfficeHour) = {
    UserOfficeHourTable.add(officeHour)
    Project.addOfficeHours(officeHour.projectId, officeHour.log.head._2)
  }

  def edit(officeHour : UserOfficeHour) : Unit = {
    val original = get(officeHour.username, officeHour.date, officeHour.projectId)
    
    println(s"edit $original $officeHour")
    if(original.isDefined) {
      //Update project row
      if(original.projectId != -1 && original.projectId == officeHour.projectId) {
        if(original.log.contains(officeHour.log.head._1)) {
          var logMap:Map[String, Double] = Map()
          logMap += (officeHour.log.head._1 -> (officeHour.log.head._2 + original.log(officeHour.log.head._1)))

          val sumOfficeHour = UserOfficeHour(officeHour.username, officeHour.date, officeHour.projectId, logMap)

          UserOfficeHourTable.edit(sumOfficeHour)
        }
        else {
          
          UserOfficeHourTable.edit(officeHour)
        }

        Project.addOfficeHours(original.projectId, officeHour.log.head._2)
      }
      else if(original.projectId == -1) {
        if(original.log.contains(officeHour.log.head._1)) {
          var logMap:Map[String, Double] = Map()
          logMap += (officeHour.log.head._1 -> (officeHour.log.head._2 + original.log(officeHour.log.head._1)))

          val sumOfficeHour = UserOfficeHour(officeHour.username, officeHour.date, officeHour.projectId, logMap)

          UserOfficeHourTable.edit(sumOfficeHour)
        }
        else {
          UserOfficeHourTable.edit(officeHour)
        }
        
      }
    }
    else {
      add(officeHour)
    }
  }
  
  def get(username: String) = UserOfficeHourTable.getUninterruptibly(username)

  def get(username: String, date: Date) = UserOfficeHourTable.getUninterruptibly(username, date)

  def get(username: String, date: Date, projectId: Int) = UserOfficeHourTable.getUninterruptibly(username, date, projectId).getOrElse {UserOfficeHour.undefined}

  def getInRange(username : String, startDate : Date, endDate : Date) = UserOfficeHourTable.getInRangeUninterruptibly(username, startDate, endDate)

  def getToday(username : String) : Seq[UserOfficeHour] = {
    val date = new Date();

    val formatted = dateFormatter.format(date)

    return get(username, new Date(formatted))
  }

  def getThisWeek(username : String) : Seq[UserOfficeHour] = {
    val now = DateTime.now


    val mondayDate = now.withDayOfWeek(1).toDate
    val sundayDate = now.withDayOfWeek(7).toDate

    val mondayFormatted = dateFormatter.format(mondayDate)
    val sundayFormatted = dateFormatter.format(sundayDate)

    return getInRange(username, new Date(mondayFormatted), new Date(sundayFormatted))

  }

  def getThisWeekFriendly(username : String) = {
    val startDate = new Date(dateFormatter.format(DateTime.now.withDayOfWeek(1).toDate))
    
    for(num <- List(1,2,3,4,5,6,7)) yield {

      if(num == 1) {
        (1, getAmount(get(username, startDate)))
      }
      else {
        val endDate = new Date(dateFormatter.format(DateTime.now.withDayOfWeek(num).toDate))
        (num, getAmount(getInRange(username, startDate, endDate)))
      }
    }
  }

  def getThisWeekFriendlyAllLoggers = {
    val startVal = for(num <- List(1,2,3,4,5,6,7)) yield (num, 0.0)
    User.all.filter(_.officeHourRequirement > 0).map((x : User) => getThisWeekFriendly(x.username)).foldLeft[Seq[(Int, Double)]](startVal)((item, accum) => {
      for((a, b) <- item.zip(accum)) yield {
        (a._1, a._2 + b._2)
      }
    })
  }

  def getAmount(officeHours : Seq[UserOfficeHour]) : Double = {
      officeHours.foldLeft(0.0)((accum : Double, officeHour : UserOfficeHour) => {
        accum + officeHour.log.values.foldLeft(0.0)((a : Double, b : Double) => a + b)
    })
  }

  def undefined = UserOfficeHour("", isDefined = false)
}

case class UserOfficeHour(
  username: String,
  date: java.util.Date = new java.util.Date(),
  projectId: Int = -1,
  log: Map[String, Double] = Map.empty[String, Double],
  isDefined : Boolean = true
)
