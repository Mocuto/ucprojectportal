package controllers

import actors.masters.ActivityMaster

import com.codahale.metrics.Counter
import com.kenshoo.play.metrics.MetricsRegistry

import java.util.Date
import java.text.SimpleDateFormat;

import model._
import model.form.Forms._

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.data.validation.Constraints._
import play.api.libs.json._
import play.api.libs.Files._

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
      "comment" -> nonEmptyText,
      "markAsUpdate" -> boolean
    ) (OfficeHour.apply)(OfficeHour.unapplyIncomplete) verifying("you can't log negative hours", _.hours >= 0)
  )

  val updatesCreatedCounter = MetricsRegistry.defaultRegistry.counter("projects.created")

  def logHour = Action { implicit request => {
    authenticated match {
      case Some(username) => {
        OfficeHourForm.bindFromRequest.fold(
          formWithErrors => {
            // return errors to client
            var errorMessage : String = "";
				    formWithErrors.errors map { error  => {
				    		errorMessage = errorMessage + " " + s"${error.key}";
				    	}
				    }
            formWithErrors("hours").value match {
              //case Some(x) if x.toDouble < 0 => BadRequest("office hours cannot be negative")
              case _ => BadRequest(errorMessage);
            }
					  
          },
          incompleteOfficeHour => {

            val project = Project.get(incompleteOfficeHour.projectId)
            if(project.isDefined == false && incompleteOfficeHour.projectId != -1) {
              NotFound("This project does not exist")
            }
            else {
              // insert into database and return a 200
              var logMap:Map[String, Double] = Map()
              logMap += (incompleteOfficeHour.comment -> incompleteOfficeHour.hours)
              val df = new SimpleDateFormat("MM/dd/yyyy")
              val officeHour = UserOfficeHour(username, incompleteOfficeHour.date,
                                              incompleteOfficeHour.projectId, logMap)

              val existingOfficeHour = UserOfficeHour.get(username, officeHour.date, officeHour.projectId)

              if (existingOfficeHour.isDefined){
                UserOfficeHour.edit(officeHour)
              } else {
                UserOfficeHour.add(officeHour)
              }

              if (incompleteOfficeHour.markAsUpdate && incompleteOfficeHour.projectId != -1){
                val completeUpdate = ProjectUpdate.create(incompleteOfficeHour.comment, username, incompleteOfficeHour.projectId, Seq[(String, TemporaryFile)]());
                val project = Project.get(incompleteOfficeHour.projectId);
                Future {
                  project.notifyFollowersAndMembersExcluding(username, incompleteOfficeHour.comment);
                }

                updatesCreatedCounter.inc();

                ActivityMaster.logSubmitUpdate(completeUpdate.author, completeUpdate.projectId, completeUpdate.timeSubmitted, completeUpdate.content)

                Future {
                  ActivityMaster.startRankingActivity();
                }
              }

              ActivityMaster.logOfficeHour(officeHour.username, officeHour.projectId, officeHour.date, officeHour.log.head._2)
              
              Ok("Success")
            }

          }
        )
      }
    }
  }}
}
