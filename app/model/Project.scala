package model

import com.datastax.driver.core.Row

import com.github.nscala_time._
import com.github.nscala_time.time._
import com.github.nscala_time.time.Imports._
import com.websudos.phantom.Implicits._

import enums._
import enums.ProjectActivityStatus
import enums.ProjectActivityStatus._

import java.util.Date

import org.joda.time.{Days, Interval, Weeks}

import play.api.libs.json._
import play.api.mvc._

import scala.collection.JavaConversions._
import scala.collection.mutable.MutableList
import scala.concurrent._
import scala.util.Random

import utils._
import utils.Conversions._
import utils.nosql.CassieCommunicator

protected sealed class ProjectTable extends CassandraTable[ProjectTable, Project] {
	object id extends IntColumn(this) with PartitionKey[Int]
	object categories extends SetColumn[ProjectTable, Project, String](this)
	object description extends StringColumn(this)
	object followers extends SetColumn[ProjectTable, Project, String](this)
	object last_activity extends DateColumn(this)
	object last_warning extends OptionalDateColumn(this)
	object likes extends SetColumn[ProjectTable, Project, String](this)
	object office_hours_logged extends DoubleColumn(this)
	object name extends StringColumn(this)
	object primary_contact extends StringColumn(this)
	object state extends StringColumn(this)
	object state_message extends StringColumn(this)
	object team_members extends SetColumn[ProjectTable, Project, String](this)
	object time_finished extends OptionalDateColumn(this)
	object time_started extends DateColumn(this)

	override def fromRow(r : Row) : Project = r match {
			case null => Project.undefined
			case _ => {
				val primaryContactStr = r.getString("primary_contact")
				return Project(
					name= name(r),
		 			id = id(r), 
		 			description= description(r), 
		 			primaryContact = primary_contact(r),
		 			categories = categories(r).toList,
		 			teamMembers = team_members(r).toList,
		 			state = state(r),
		 			stateMessage = state_message(r),
		 			timeStarted = time_started(r),
		 			timeFinished = time_finished(r),
		 			lastActivity = last_activity(r),
		 			lastWarning = last_warning(r),
		 			likes = likes(r).toList,
		 			followers = followers(r).toList,
		 			officeHoursLogged = office_hours_logged(r),
					isDefined = true
				);
			}
		}
}

object ProjectTable extends ProjectTable {
	override val tableName = "projects";
	implicit val session = CassieCommunicator.session

	def add(
		name: String,
		description : String,
		primaryContact : String,
		categories : Seq[String],
		state : String,
		stateMessage : String,
		teamMembers : Seq[String]) : Project = {
		val id = (allUninterruptibly.reduce((a,b) => if(a.id > b.id) a else b)).id + 1;
		val date = new Date();
		val timeFinished = if(state == ProjectState.COMPLETED || state == ProjectState.CLOSED) Some(date) else None;

		val project = Project(
			id = id,
			name = name,
			description = description,
			primaryContact = primaryContact,
			categories = categories,
			state = state,
			stateMessage = stateMessage,
			teamMembers = teamMembers,
			timeStarted = date,
			timeFinished = timeFinished,
			lastActivity = date,
			officeHoursLogged = 0
		)

		scala.concurrent.Await.result(
			insert
				.value(_.id, id)
				.value(_.name, name)
				.value(_.description, description)
				.value(_.primary_contact, primaryContact)
				.value(_.categories, categories.toSet)
				.value(_.state, state)
				.value(_.state_message, stateMessage)
				.value(_.team_members, teamMembers.toSet)
				.value(_.time_started, date)
				.value(_.time_finished, timeFinished)
				.value(_.last_activity, date)
				.value(_.office_hours_logged, 0.0)
				.future(),
				constants.Cassandra.defaultTimeout)

		project
	}

	def edit(
		id : Int,
		name : String,
		description : String,
		state : String,
		stateMessage : String,
		categories : Seq[String],
		primaryContact : String,
		timeFinished : Option[Date]) : Unit = {
		
		update.where(_.id eqs id)
			.modify(_.name setTo name)
			.and(_.description setTo description)
			.and(_.state setTo state)
			.and(_.state_message setTo stateMessage)
			.and(_.categories setTo categories.toSet)
			.and(_.primary_contact setTo primaryContact)
			.and(_.time_finished setTo timeFinished)
			.future();
	}

	def get(id : Int) : Future[Option[Project]] = select.where(_.id eqs id).one();
	def getUninterruptibly (id: Int) : Option[Project] = scala.concurrent.Await.result(get(id), constants.Cassandra.defaultTimeout)

	def all : Future[Seq[Project]] = select.fetch();
	def allUninterruptibly : Seq[Project] = scala.concurrent.Await.result(all, constants.Cassandra.defaultTimeout)

	def addLike(id : Int, username : String) : Unit = update.where(_.id eqs id).modify(_.likes add username).future();
	def removeLike(id : Int, username : String) : Unit = update.where(_.id eqs id).modify(_.likes remove username).future();

	def updateLastActivity(id : Int, date : Date) : Unit = update.where(_.id eqs id).modify(_.last_activity setTo date).future();
	def updateLastWarning(id : Int) : Unit = update.where(_.id eqs id).modify(_.last_warning setTo Some(new Date())).future();

	def addFollower(id : Int, username : String) : Unit = {
		update.where(_.id eqs id).modify(_.followers add username).future();
		User.addProjectToFollow(username, id);
	}

	def removeFollower(id : Int, username : String) : Unit = {
		update.where(_.id eqs id).modify(_.followers remove username).future();
		User.removeProjectToFollow(username, id);
	}

	def addOfficeHours(id : Int, amount : Double) : Unit = {
		println(s"Project.addOfficeHours $id $amount")
		get(id).map(_ match {
			case Some(project) if project.isDefined => update.where(_.id eqs id).modify(_.office_hours_logged setTo (project.officeHoursLogged + amount)).future()
			case x => println(x)//NOOP
		})
	}
}

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

		val newProject = ProjectTable.add(name, description, primaryContact, categories, state, stateMessage, primaryContact :: teamMembers.toList)

		Project.changePrimaryContact(newProject.id, User.undefined, User.get(primaryContact))

		for (username <- teamMembers) {
			if(username != primaryContact) {
				Project.addUser(newProject.id, User.get(username))
			}
		}

		return newProject
	}

	def edit(
		id : Int,
		name : String,
		description : String,
		state : String,
		stateMessage : String,
		categories : Seq[String],
		primaryContact : String,
		timeFinished : Option[Date]) : Unit = ProjectTable.edit(id, name, description, state, stateMessage, categories, primaryContact, timeFinished)
	

	def all : Seq[Project] = ProjectTable.allUninterruptibly//CassieCommunicator.getProjects;
	
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

	def get(username : String) : Seq[Project] = User.get(username).projects.map(Project.get(_ : Int))

	def get(id : Int) : Project = {
		//return CassieCommunicator.getProject(id);
		return ProjectTable.getUninterruptibly(id).getOrElse({ Project.undefined })
	}

	def getPrimaryForUsername(username : String) : Seq [Project] = User.get(username).primaryContactProjects.map(Project.get(_ : Int))

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
			freeze(project)
		}
	}

	def freeze(project : Project) {
		Project.update(Project(
				id = project.id,
				name = project.name,
				description = project.description,
				categories = project.categories,
				state = ProjectState.CLOSED,
				stateMessage = project.stateMessage,
				teamMembers = project.teamMembers,
				primaryContact = project.primaryContact,
				lastActivity = project.lastActivity,
				lastWarning = project.lastWarning,
				likes = project.likes,
				timeFinished = Some(new Date())
		))
	}

	def freezeWithNotification(project : Project) {
		freeze(project)

		project.involvedMembers(User.get(project.primaryContact)).toSet foreach((x : String) => Notification.createProjectFrozen(User.get(x), project))
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

		User.addToProject(newUser.username, id);

		return Project.get(id);
	}

	//def getUpdates(id : Int) : Seq[ProjectUpdate] = CassieCommunicator.getUpdatesForProject(id);
	def getUpdates(id : Int) : Seq[ProjectUpdate] = ProjectUpdateTable.getUninterruptibly(id)

	def getUpdatesAsync(id : Int) : Future[Seq[ProjectUpdate]] = ProjectUpdateTable.get(id)

	def addLike(id : Int, username : String) : Unit = ProjectTable.addLike(id, username)

	def removeLike(id : Int, username : String) : Unit = ProjectTable.removeLike(id, username)

	def addFollower(id : Int, follower : String) : Unit = ProjectTable.addFollower(id, follower)

	def removeFollower(id : Int, follower : String) : Unit = ProjectTable.removeFollower(id, follower)

	def updateLastActivity(id : Int, date : Date) : Unit = ProjectTable.updateLastActivity(id, date)

	def updateLastWarning(id : Int) : Unit = ProjectTable.updateLastWarning(id)

	def addOfficeHours(id : Int, amount : Double) = ProjectTable.addOfficeHours(id, amount)

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
		 			lastActivity = row.getDate("last_activity"),
		 			lastWarning = if(row.isNull("last_warning")) None else Some(row.getDate("last_warning")),
		 			timeFinished = if(row.isNull("time_finished")) None else Some(row.getDate("time_finished"))
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
		timeFinished : Option[Date] = None,
		lastActivity : Date = new Date(),
		lastWarning : Option[Date] = Some(new Date()),
		categories : Seq[String] = List[String](), 
		tags : Seq[String] = List[String](),
	 	primaryContact : String = "", 
	 	teamMembers : Seq[String] = List[String](), 
	 	state : String = "", 
	 	stateMessage : String = "",
	 	likes : Seq[String] = List[String](),
	 	followers : Seq[String] = List[String](),
	 	officeHoursLogged : Double = 0,
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

	def notifyFollowersAndMembersExcluding(excludingUsername : String, updateContent : String) : Unit = {
		val updater = User.get(excludingUsername)
		involvedMembers(updater) foreach(username => {
			if(excludingUsername != username)
			{
				Notification.createUpdate(User.get(username), updater, this, updateContent)
			}
		});
	}

	def involvedMembers(updater : User) : Set[String] = (teamMembers ++ followers ++ updater.followers).toSet[String]

	def isNew : Boolean = Weeks.weeksBetween(new DateTime(timeStarted), DateTime.now).getWeeks <= 1;

	def removeUser(user : User) : Project = Project.removeUser(id, user);

	def activityStatus : ProjectActivityStatus = (Days daysIn(new Interval(lastActivity.getTime, (new Date()).getTime))).getDays match {
		case x if x <= constants.ServerSettings.ActivityStatus.Hot.getDays && state != ProjectState.CLOSED => ProjectActivityStatus.Hot
		case x if x <= constants.ServerSettings.ActivityStatus.Warm.getDays && state != ProjectState.CLOSED => ProjectActivityStatus.Warm
		case x if x <= constants.ServerSettings.ActivityStatus.Cold.getDays && state != ProjectState.CLOSED => ProjectActivityStatus.Cold
		case x if x <= constants.ServerSettings.ActivityStatus.Freezing.getDays && state != ProjectState.CLOSED => ProjectActivityStatus.Freezing
		case _ => ProjectActivityStatus.Frozen
	}
}