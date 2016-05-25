package actors.workers

import actors.ActorMessageTypes._
import akka.actor._

import enums.ProjectActivityStatus
import enums.ProjectActivityStatus._

import java.util.Date

import model._

import org.apache.lucene.document.Document
import org.apache.lucene.document._
import org.joda.time._

import play.api.Logger

import scala.collection.immutable.ListMap

import utils.SMTPCommunicator

class ActivityWarner extends Actor with Worker[User, Option[play.twirl.api.Html]] {
	def receive = {
		case ActorWork(u : User) => sender ! work(u)
	}

	def shouldWarn(p : Project) : Boolean = {
		val now = new Date()
		return ((Days daysIn(new Interval(p.lastWarning.getOrElse(new Date(0)).getTime, now.getTime))).getDays >= 7 &&
				(Days daysIn(new Interval(p.lastActivity.getTime, now.getTime))).getDays > constants.ServerSettings.ActivityStatus.Warm.getDays() &&
				(p.state == ProjectState.IN_PROGRESS || p.state == ProjectState.IN_PROGRESS_NEEDS_HELP))
	}

	def work(u : User) : ActorResult[User, Option[play.twirl.api.Html]] = {
		val endangeredProjects = u.primaryContactProjects
			.map(Project.get(_ : Int))
			.filter((x : Project) => shouldWarn(x) && x.activityStatus != ProjectActivityStatus.Frozen)
			.groupBy (_.activityStatus)

		val projectsToFreeze = u.projects
			.map(Project.get(_))
			.filter ((x : Project) => x.activityStatus == ProjectActivityStatus.Frozen && x.state != ProjectState.CLOSED && x.state != ProjectState.IDEA && x.state != ProjectState.COMPLETED)

		if (projectsToFreeze.length > 0) {
			projectsToFreeze map (Project.freezeWithNotification _)
		}

		endangeredProjects.values.flatten.foreach((x : Project) => Project.updateLastWarning(x.id))

		if (endangeredProjects.size > 0) {

			val title = (endangeredProjects keySet) reduce((a,b) => (a : ProjectActivityStatus, b : ProjectActivityStatus) match {
				case _ if a == ProjectActivityStatus.Freezing || b == ProjectActivityStatus.Freezing => ProjectActivityStatus.Freezing
				case _ => ProjectActivityStatus.Cold
			}) match {
				case ProjectActivityStatus.Freezing => s"${u.firstName}, your projects are freezing!"
				case ProjectActivityStatus.Cold => s"${u.firstName}, your projects are getting cold!"
			}

			val warningEmail = views.html.email.emailProjectWarning(u, title, endangeredProjects)
			(u.username :: u.followers.toList).toSet map((username : String) => SMTPCommunicator sendEmail(username, (title split(" ")) map(_.capitalize) mkString(" "), warningEmail.toString))
			ActorResult(u, Some(warningEmail))
		}
		else {
			ActorResult(u, None)
		}

	}
}