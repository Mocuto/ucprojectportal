package controllers

import com.codahale.metrics.Counter
import com.kenshoo.play.metrics.MetricsRegistry
import com.typesafe.plugin._

import java.util.Date
import java.text.SimpleDateFormat;

import model._

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.data.validation.Constraints._
import play.api.libs.json._

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Future => ScalaFuture }
import scala.util.{Success, Failure}

import utils._

object OfficeHourController extends Controller with SessionHandler {
  private val OfficeHourForm = Form(
    mapping(
      "date" -> date("MM/dd/yyyy"),
      "projectId" -> number,
      "hours" -> of(doubleFormat),
      "comment" -> text,
      "markAsUpdate" -> boolean
    ) (OfficeHour.apply)(OfficeHour.unapplyIncomplete)
  )

  def logHour = Action { implicit request => {
    authenticated match {
      case Some(username) => {
        OfficeHourForm.bindFromRequest.fold(
          formWithErrors => {
            // return errors to client
            var errorMessage : String = "";
				    formWithErrors.errors map { error  => {
				    		errorMessage = s"${error.key}";
				    	}
				    }
					  BadRequest(errorMessage);
          },
          incompleteOfficeHour => {
            // insert into database and return a 200
            var logMap:Map[Double, String] = Map()
            logMap += (incompleteOfficeHour.hours -> incompleteOfficeHour.comment)
            val df = new SimpleDateFormat("MM/dd/yyyy")
            val officeHour = nosql.UserOfficeHour(username, df.format(incompleteOfficeHour.date),
                                            incompleteOfficeHour.projectId, logMap)

            val f: ScalaFuture[Seq[nosql.UserOfficeHour]] = nosql.UserOfficeHours.getUserOfficeHoursForDateAndProject(username, officeHour.date, officeHour.projectId)

            val officeHours: Seq[nosql.UserOfficeHour] = Await.result(f, duration.Duration.Inf)

            if (officeHours.size > 0){
              nosql.UserOfficeHours.updateRecord(officeHour)
            } else {
              nosql.UserOfficeHours.insertNewRecord(officeHour)
            }
            Ok("Success")
          }
        )
      }
    }
  }}
}
