package model

import com.datastax.driver.core.Row

import com.github.nscala_time._
import com.github.nscala_time.time._
import com.github.nscala_time.time.Imports._

import java.util.Date

import org.joda.time.{Weeks, Years}

import play.api.libs.json._
import play.api.mvc._

import scala.collection.JavaConversions._
import scala.collection.mutable.MutableList
import scala.concurrent._
import scala.util.Random

import utils._
import utils.Conversions._
import utils.nosql.CassieCommunicator

object Project {

	def apply (name : String) : Project = new Project(name);
	def apply (
		name : String,
		description : String,
		categories : Seq[String],
		state : String,
		stateMessage : String,
		teamMembers : Seq[String]) = new Project(name, description, categories = categories, state = state, stateMessage = stateMessage, teamMembers = teamMembers);

	def unapplyIncomplete(project : Project) : Option[(String, String, List[String], String, String, List[String])] = Some(
		project.name,
		project.description,
		project.categories.toList,
		project.state,
		project.stateMessage,
		project.teamMembers.toList);

	def undefined : Project = {
		return Project(
			isDefined = false,
			id = -1,
			name = "",
			description = "",
			categories = List[String]("null")
		)
	}

	def create (name: String, description : String, primaryContact : String, categories : Seq[String], state : String, stateMessage : String, teamMembers : Seq[String]) : Project = {

		val project = Project(name, description, categories, state, stateMessage, primaryContact :: teamMembers.toList);

		val newProject = CassieCommunicator.addProject(project, primaryContact, categories, state, stateMessage, primaryContact :: teamMembers.toList);

		for (username <- teamMembers) {
			if(username != primaryContact) {
				Project.addUser(newProject.id, User.get(username))
			}
		}

		return newProject
	}

	def all : Seq[Project] = CassieCommunicator.getProjects;
	def allSorted : Seq[Project] = {
		val projects = Project.all;

		val projectCount = projects.length;
		val randomizedProjects = MutableList.fill(projectCount) { Project.undefined }

		val rankCount = 2 //Options are either not frozen/closed or frozen/closed, thus there are only two ranks

		//In the future we may want to partially apply the tabulate
		//And supply the index function separately
		val filteredProjectIndexes = List.fill(2) { new MutableList[Int] }
		for (i <- 0 until projectCount) {
			val p = projects(i)
			if (p.state == ProjectState.CLOSED || p.state == ProjectState.COMPLETED) {
				filteredProjectIndexes(1) += i;
			}
			else {
				filteredProjectIndexes(0) += i;
			}
		}

		var start = 0;

		(0 until rankCount).foreach( x => {
			val currentList = filteredProjectIndexes(x);
			(0 until filteredProjectIndexes(x).length).foreach( i => {
				val randomIndex =
				 	if (i == 0) {
						0
					}
					else {
							Math.abs(Random.nextInt) % i
					}

				if(i != randomIndex) {
					randomizedProjects(start + i) = randomizedProjects(start + randomIndex);
				}

				randomizedProjects(start + randomIndex) = projects(currentList(i))
			})
			start += currentList.length;
		})

		return randomizedProjects;

	}

	def get(username : String) : Seq[Project] = {
		return CassieCommunicator.getProjectsForUsername(username);
	}

	def get(id : Int) : Project = {
		return CassieCommunicator.getProject(id);
	}

	def getPrimaryForUsername(username : String) : Seq [Project] = {
		return CassieCommunicator.getPrimaryProjectsForUsername(username);
	}

	def update(project : Project) {
		if(!project.isDefined) {
			return
		}
		CassieCommunicator.updateProject(project);
	}


	def delete(id : Int) {
		Project.getUpdates(id).foreach(update => update.delete())
		val project = Project.get(id);

		project.teamMembers.foreach(username => project.removeUser(User.get(username)));
		
		CassieCommunicator.removeProject(Project.get(id));
	}

	def close(project : Project) {
		if(project.isNew && Project.getUpdates(project.id).length == 0) {
			delete(project.id);
		}
		else {
			Project.update(Project(
					id = project.id,
					name = project.name,
					description = project.description,
					categories = project.categories,
					state = ProjectState.CLOSED,
					stateMessage = project.stateMessage,
					teamMembers = project.teamMembers,
					primaryContact = null,
					timeFinished = project.timeFinished
			))
		}
	}

	def allTags : Seq[String] = return CassieCommunicator.getTagsWithType("project").getOrElse { return List[String]() };


	def addUser(id : Int, user : User) : Project = {
		val project = Project.get(id);

		if(!project.isDefined) {
			return Project.undefined
		}

		CassieCommunicator.addUserToProject(user, project);
		Notification.createAddedToProject(user, project);
		return Project.get(id);
	}

	def removeUser(id : Int, user : User) : Project = {
		val project = get(id);

		if(!project.isDefined) {
			return Project.undefined
		}
		
		CassieCommunicator.removeUserFromProject(user, project);

		if(project.primaryContact == user.username) {
			Project.removePrimaryContact(id)
		}

		return Project.get(id);
	}

	def removePrimaryContact(id : Int) : Project = {
		val project = Project.get(id);

		if(!project.isDefined) {
			return Project.undefined
		}

		println(s"Num of members = ${project.teamMembers.length}")

		if(project.teamMembers.length <= 1) {
			Project.changePrimaryContact(id, User.get(project.primaryContact), User.undefined);
			Project.close(project);
			return Project.get(id);
		}
		else {
			var otherUser : User = User.undefined;
			project.teamMembers.toStream.takeWhile(_ => otherUser == null).foreach({ member =>
				if(member != project.primaryContact) {
					otherUser = User.get(member);
				}
			})
			return Project.changePrimaryContact(id, User.get(project.primaryContact), otherUser)
		}
	}

	def changePrimaryContact(id : Int, oldUser : User, newUser : User) : Project = {
		val oldProject = Project.get(id);

		if(!oldProject.isDefined) {
			return Project.undefined
		}

		CassieCommunicator.changePrimaryContactForProject(oldUser, newUser, oldProject);
		ProjectRequest.swapOwner(id, oldUser.username, newUser.username)

		return Project.get(id);
	}

	//def getUpdates(id : Int) : Seq[ProjectUpdate] = CassieCommunicator.getUpdatesForProject(id);
	def getUpdates(id : Int) : Seq[ProjectUpdate] = ProjectUpdateTable.getUninterruptibly(id)

	def getUpdatesAsync(id : Int) : Future[Seq[ProjectUpdate]] = ProjectUpdateTable.get(id)

	implicit def fromRow (row : Row) : Project =  { 
		row match {
			case null => Project.undefined
			case row : Row => {
				val primaryContactStr = row.getString("primary_contact")
				return Project(
					name= row.getString("name"),
		 			id = row.getInt("id"), 
		 			description= row.getString("description"), 
		 			primaryContact = if (primaryContactStr == null) "" else primaryContactStr,
		 			categories = row.getSet("categories", classOf[String]).toList,
		 			teamMembers = row.getSet("team_members", classOf[String]).toList,
		 			state = row.getString("state"),
		 			stateMessage = row.getString("state_message"),
		 			timeStarted = row.getDate("time_started"),
		 			timeFinished = row.getDate("time_finished")
				);
			}
		}
	 }
}

case class Project (	
		id : Int,
		name: String, 
		description : String,
		timeStarted : Date = new Date(), 
		timeFinished : Date = null,
		categories : Seq[String] = List[String](), 
		tags : Seq[String] = List[String](),
	 	primaryContact : String = "", 
	 	teamMembers : Seq[String] = List[String](), 
	 	state : String = "", 
	 	stateMessage : String = "",
	 	isDefined : Boolean = true) {

	def this (
			name : String, 
			description : String, 
			categories : Seq[String], 
			state : String,
			stateMessage : String,
			teamMembers : Seq[String]) = {

 		this(-1, name, description, categories=categories, state = state, teamMembers=teamMembers)
	}

	def this (name : String) = this(
			name, 
			description = "", 
			categories = List[String](), 
			state = "",
			stateMessage = "",
			teamMembers = List[String]())

	def notifyMembersExcluding(excludingUsername : String, updateContent : String) : Unit = {
		teamMembers foreach(username => {
			if(excludingUsername != username)
			{
				Notification.createUpdate(User.get(username), User.get(excludingUsername), this, updateContent)
			}
		});
	}

	def isNew : Boolean = Weeks.weeksBetween(new DateTime(timeStarted), DateTime.now).getWeeks <= 1;

	def removeUser(user : User) : Project = Project.removeUser(id, user);
}