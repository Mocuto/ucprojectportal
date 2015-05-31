package model

import com.datastax.driver.core.Row

import com.github.nscala_time._
import com.github.nscala_time.time._
import com.github.nscala_time.time.Imports._

import java.util.Date
import java.text.SimpleDateFormat;

import org.joda.time.{Weeks, Years}

import play.api.libs.json._
import play.api.mvc._

import scala.collection.JavaConversions._
import scala.collection.mutable.MutableList
import scala.util.Random

import utils._

object OfficeHour {
  def apply(date: Date, projectId: Int, hours: Double, comment: String, markAsUpdate: Boolean) : OfficeHour = {
    new OfficeHour("", date, projectId, hours, comment, markAsUpdate)
  }

  def unapplyIncomplete(officeHour: OfficeHour): Option[(Date, Int, Double, String, Boolean)] = Some(officeHour.date, officeHour.projectId, officeHour.hours, officeHour.comment, officeHour.markAsUpdate)
}

case class OfficeHour(
  username: String,
  date: Date,
  projectId: Int,
  hours: Double,
  comment: String,
  markAsUpdate: Boolean
)
