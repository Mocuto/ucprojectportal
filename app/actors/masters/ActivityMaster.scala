package actors.masters

import actors.ActorMessageTypes._
import actors.workers.ActivityWarner
import akka.actor._
import akka.pattern.ask
import akka.routing._
import akka.util.Timeout

import com.github.nscala_time.time.Imports._

import enums.ActivityType._
import enums.ActivityType

import java.util.Date

import model._

import org.joda.time.{Days, Weeks, Interval}

import play.api.Logger

import scala.collection.Map
import scala.concurrent._

import utils.{Conversions, SMTPCommunicator}

trait ActivityLogger {

	def logViewUser(viewer : String, viewee : String) : Unit = {
		log(viewer, ActivityType.ViewUser, Map("user" -> viewee))
	}

	def logProjectActivity(username : String, projectId : Int, activityType : ActivityType) : Unit = {
		log(username, activityType, Map("project-id" -> projectId.toString))
	}

	def logAcceptRequest(username : String, projectId : Int, requester : String) : Unit = {
		log(username, ActivityType.AcceptRequest, Map(
			"project-id" -> projectId.toString,
			"requester" -> requester
		))
	}

	def logIgnoreRequest(username : String, projectId : Int, requester : String) : Unit = {
		log(username, ActivityType.IgnoreRequest, Map(
			"project-id" -> projectId.toString,
			"requester" -> requester
		))
	}

	def logSubmitUpdate(username : String, projectId : Int, timeSubmitted : Date, content : String) : Unit = {
		Project.updateLastActivity(projectId, timeSubmitted)
		
		log(username, ActivityType.SubmitUpdate, Map(
			"project-id" -> projectId.toString,
			"time-submitted" -> (Conversions dateToStr timeSubmitted),
			"content" -> content
		))
	}

	def logEditUpdate(username : String, projectId : Int, author : String, timeSubmitted : Date, timeEditted : Date) : Unit = {
		log(username, ActivityType.EditUpdate, Map(
			"author" -> author,
			"project-id" -> projectId.toString,
			"time-submitted" -> (Conversions dateToStr timeSubmitted),
			"time-editted" -> (Conversions dateToStr timeEditted)
		))
	}

	def logDeleteUpdate(username : String, projectId : Int, author : String,  timeSubmitted : Date) : Unit = {
		log(username, ActivityType.DeleteUpdate, Map(
			"author" -> author,
			"project-id" -> projectId.toString,
			"time-submitted" -> (Conversions dateToStr timeSubmitted)
		))
	}

	def logLikeUpdate(username : String, projectId : Int,  author : String, timeSubmitted : Date) : Unit = {
		log(username, ActivityType.LikeUpdate, Map(
			"project-id" -> projectId.toString,
			"author" -> author,
			"time-submitted" -> (Conversions dateToStr timeSubmitted)))
	}

	def logUnlikeUpdate(username : String, projectId : Int,  author : String, timeSubmitted : Date) : Unit = {
		log(username, ActivityType.UnlikeUpdate, Map(
			"project-id" -> projectId.toString,
			"author" -> author,
			"time-submitted" -> (Conversions dateToStr timeSubmitted)))
	}

	def logFollowUser(follower : String, toFollow : String) : Unit = {
		log(follower, ActivityType.FollowUser, Map("to-follow" -> toFollow))
	}

	def logUnfollowUser(follower : String, toFollow : String) : Unit = {
		log(follower, ActivityType.UnfollowUser, Map("to-follow" -> toFollow))
	}

	def log(username : String, activityType : ActivityType, detail : Map[String, String]) : Unit = {
		val activity = UserActivity.add(username, activityType, detail);
		User.updateLastActivity(username, activity.timeSubmitted)
	}
}

trait ActivityEmailHandler {
	def sendProjectLikedEmail(projectId : Int) : Unit = {
		//Schedule an email to be sent 10 minutes in the future
	}
}

class ActivityMaster extends Actor with WorkRouter {

	def workerProps = Props[ActivityWarner]

	def receive = {
		case ActorWork(u : User) => routeWork(ActorWork(u))
		
		case ws : Seq[_] => ws.foreach(receive(_))

		case ActorTerminated(a) => onActorTerminated(a)
	}

	def logLikeProject(username : String) : Unit = {
		//Notify Team Members
	}
}

object ActivityMaster extends Master with actors.Scheduler with ActivityLogger {

	import play.api.libs.concurrent.Akka
	import play.api.Play.current

	def actorName = "activity-master"

	val ProjectWarnings = "project warning daily routine"
	val ProjectDigest = "project digest weekly  routine"
	val ProjectLikedEmail = (u : User, projectId : Int) => s"${u.username} liked project-$projectId"
	val UpdateLikedEmail = (u : User, projectId : Int, author : String, timeSubmittedStr : String) => s"${u.username} liked update-$projectId-$author-$timeSubmittedStr"

	val LikedEmailDelayInHours = 3;

	def masterProps = Props[ActivityMaster]

	def start() : Unit = {
		scheduleWarnings();
		startDigest();
	}

	def scheduleWarnings() : Unit = scheduleAtNext(constants.ServerSettings.ProjectWarningHour, ProjectWarnings) {
		if(isStopped) {
			return
		}
		import scala.concurrent.ExecutionContext.Implicits.global
		val f = Future {
			startWarnings();
		}
		
		f onComplete {
			case _ if !isStopped => {
				Logger.info("scheduling next round of warnings")
				scheduleWarnings();
			}
		}
	}

	def scheduleDigest() : Unit = scheduleAtNext(constants.ServerSettings.ProjectDigestDay, constants.ServerSettings.ProjectDigestHour, ProjectDigest) {
		
		import scala.concurrent.ExecutionContext.Implicits.global
		val f = Future {
			startDigest()
		}

		f onComplete {
			case _ => {
				Logger.info("scheduling next digest")
				scheduleDigest()
			}
		}
	}

	def startDigest() : Unit = {
		//First compile global portion of digest
		val updatesThisWeek = ProjectUpdate.all.filter(u => { 
			((Days.daysBetween(new DateTime(u.timeSubmitted), DateTime.now).getDays) < 7) &&
			u.timeSubmitted == u.timeEditted //Remove updates that are simply edits
		})

		val numberOfUpdates = updatesThisWeek.length;

		//Hot Projects
		val hotProjectsWithCount = updatesThisWeek
			.groupBy(_.projectId) //Group by projects
			.toSeq
			.sortWith((a,b) => a._2.length > b._2.length) //Sort by amount of updates
			.take(constants.ServerSettings.HotProjectsCount) // Take top X projects
			.map({case (projectId : Int, updates : Seq[ProjectUpdate]) => (Project.get(projectId), updates.length)}) //Map to (Project, Count)

		//Hot Contributors
		val hotContributorsWithCount = updatesThisWeek
			.groupBy(_.author) //Group by author
			.toSeq
			.sortWith((a,b) => a._2.length > b._2.length) //Sort by amount of updates
			.take(constants.ServerSettings.HotContributorsCount) // Take top X contributors
			.map({case (username : String, updates : Seq[ProjectUpdate]) => (User.get(username), updates.length)}) //Map to (User, Count)

		val projects = Project.all;

		val numberOfProjects = projects.length;

		//How many projects are in each temperature range
		val projectsForEachTemperature = projects.groupBy(_.activityStatus)

		//New Projects
		val newProjects = projects.filter(_.isNew)

		//Completed Projects
		val completedProjects = projects.filter(p => {
			(p.state == ProjectState.COMPLETED) && 
			((Weeks.weeksBetween(new DateTime(p.timeFinished), DateTime.now).getWeeks) <= 1)})

		//Then compile personlized digest for:
		User.all.filter(_.verified).foreach(u => {
			val userProjectsForEachTemperature = u.projects.map(Project.get(_ : Int)).groupBy(_.activityStatus) //1. The projects they're working on
			val userProjectsFollowingForEachTemperature = u.projectsFollowing.map(Project.get(_ : Int)).groupBy(_.activityStatus) //2. The projects they're following

			import scala.concurrent.ExecutionContext.Implicits.global
			Future { //Send personalized email
				SMTPCommunicator.sendDigestEmail(
					u.username,
					projects.length,
					hotProjectsWithCount,
					hotContributorsWithCount,
					projectsForEachTemperature,
					newProjects,
					completedProjects,
					userProjectsForEachTemperature,
					userProjectsFollowingForEachTemperature)
			}

		})
		
	}

	def startWarnings() : Unit = {
		//Iterate through all users and start checking their project activity
		User.allConfirmed.foreach(u => checkWarningsFor(u))
	}

	def scheduleProjectLikedEmail(u : User, project : Project) : Unit = {
		val name = ProjectLikedEmail(u, project.id)
		if(isScheduled(name) == false) {
			scheduleAt((new DateTime()).plusHours(LikedEmailDelayInHours), name) {
				import scala.concurrent.ExecutionContext.Implicits.global
				Future {
					SMTPCommunicator.sendProjectLikedEmail(project.id)
				}
				
			}
		}
	}

	def scheduleUpdateLikedEmail(u : User, update : ProjectUpdate) : Unit = {
		val name = UpdateLikedEmail(u, update.projectId, update.author, utils.Conversions.dateToStr(update.timeSubmitted))
		if(isScheduled(name) == false) {
			scheduleAt((new DateTime()).plusHours(LikedEmailDelayInHours), name) {
				import scala.concurrent.ExecutionContext.Implicits.global
				Future {
					SMTPCommunicator.sendUpdateLikedEmail(update.projectId, update.author, update.timeSubmitted)
				}
				
			}
		}
		import scala.concurrent.ExecutionContext.Implicits.global
				Future {
					SMTPCommunicator.sendUpdateLikedEmail(update.projectId, update.author, update.timeSubmitted)
				}
	}

	def checkWarningsFor(u : User) = actor ! ActorWork(u)
}