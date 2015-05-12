package utils.nosql

import com.datastax.driver.core.{Cluster, ResultSet, ResultSetFuture, Row}
import com.datastax.driver.core.exceptions._

import java.util.Date

import model._

import play.api.Logger

import scala.collection.JavaConversions._

import utils.{Conversions, PasswordHasher}

trait ProjectSqlCommunicator extends BaseSqlCommunicator {

	private[nosql] val PROJECTS : String
	private[nosql] val PROJECTS_KEY : String
	private[nosql] val PROJECT_INSERT_FIELDS : String
	private[nosql] val PROJECT_UPDATE_FIELDS : String

	private[nosql] val USERS : String

	private[nosql] val logger : Logger

	def getProjects : Seq[Project] = {
		val columnName = "name";
    	val executeString = s"select * from $PROJECTS"
      val result = executeAsync(executeString);
      result match {
        case None => return List[Project]();
        case Some(r: ResultSetFuture) => return r.getUninterruptibly().all().map(x => Project.fromRow(x));
      }
  }

  def getProjectsForUsername(username : String, projectColumnName : String = "projects") : Seq[Project] = {
    val nameColumnName = "name";
    var executeString = s"select $projectColumnName from $USERS where username='$username'";

    val projectIds = executeAsync(executeString) match {
      case None => List[java.lang.Integer]();
      case Some(r : ResultSetFuture) => r.getUninterruptibly().one() match {
        case null => List[java.lang.Integer]();
        case row : Row => row.getSet(s"$projectColumnName", classOf[java.lang.Integer]).toList
      }
    }

    return projectIds.map(id => {
        executeString = s"select * from $PROJECTS where $PROJECTS_KEY=$id";
        executeAsync(executeString) match {
          case None => {
            logger.error(s"Undefined project: $id queried for user: $username ");
            Project.undefined
          }
          case Some(r : ResultSetFuture) => {
            val row = r.getUninterruptibly().one();
            Project.fromRow(row);
          }
        }
    })

  }

	def getPrimaryProjectsForUsername(username : String) : Seq [Project] = {
	    val projectColumnName = "primary_contact_projects";
	    return getProjectsForUsername(username, projectColumnName);
	}

	def getProject(projectId : Int) : Project = {
	    val executeString = s"select * from $PROJECTS where $PROJECTS_KEY=$projectId";
	    executeAsync(executeString) match {
	      case None => return Project.undefined
	      case Some(r : ResultSetFuture) => {
	        val row = r.getUninterruptibly().one();
	        return Project.fromRow(row);
	      }
	}}
	def getNumberOfProjects() : Int = {
    	val executeString = s"select count(*) from $PROJECTS";

    	return execute(executeString).get.one().getLong("COUNT").toInt;
  	}

	def getNextProjectId() : Int = {
	    var id = getNumberOfProjects();

	    while(Project.get(id) != Project.undefined) {
	      id += 1;
	    }
	    return id;
	}

	def addProject(project : Project, primaryContact : String, categories : Seq[String], state : String, teamMembers : Seq[String]) : Project = {
		var id = getNextProjectId();

		val categoriesStr = categories.map(category => s"'${category.replace("'", "''")}'").mkString(",");

		val cleanState = state.replace("'", "''");

		val teamMembersStr = teamMembers.map(member => s"'$member'").mkString(",")

		var executeString = s"insert into $PROJECTS($PROJECT_INSERT_FIELDS) VALUES($id, '${project.description.replace("'", "''")}', { $teamMembersStr}, '${project.name.replace("'", "''")}', '$primaryContact', '$cleanState', '', { $categoriesStr } , dateOf(now()))";
		execute(executeString);

		executeString = s"update $USERS set primary_contact_projects = primary_contact_projects + { $id } where username='$primaryContact'";
		execute(executeString);

		executeString = s"update $USERS set projects = projects + { $id } where username='$primaryContact'";
		execute(executeString);

		return Project(id, project.name, project.description);
	}

	def addUserToProject(user : User, project : Project) {
		val userString = s"update $USERS set projects = projects + {${project.id}} where username = '${user.username}'";
		execute(userString);

		val projectString = s"update $PROJECTS set team_members = team_members + {'${user.username}'} where id = ${project.id}";
		execute(projectString);
	}

	def updateProject(project : Project) {
		if(project.id == -1) {
		  return;
		}
		val categoriesStr = project.categories.map(category => s"'${category.replace("'", "''")}'").mkString(",");
		var stateMessage =  project.stateMessage match {
		  case null => null
		  case _  => project.stateMessage.replace("'", "''");
		}

		val timestamp = if(project.timeFinished == null) null else s"'${utils.Conversions.dateToStr(project.timeFinished)}'";

		val executeString = s"insert into $PROJECTS($PROJECT_UPDATE_FIELDS) VALUES(${project.id}, '${project.name}' , '${project.description.replace("'", "''")}', '${project.state}', { $categoriesStr }, '$stateMessage', '${project.primaryContact}', $timestamp)";

		execute(executeString);
	}

	def changePrimaryContactForProject(oldUser : User, newUser : User, project : Project) {
		val oldUserString = s"update $USERS set primary_contact_projects = primary_contact_projects - {${project.id}} where username= '${oldUser.username}'";
		val newUserString = s"update $USERS set primary_contact_projects = primary_contact_projects + {${project.id}} where username= '${newUser.username}'";
		val projectString = s"update $PROJECTS set primary_contact = '${newUser.username}' where id = ${project.id}";

		if(oldUser.isDefined) {
		  execute(oldUserString);
		}

		if(newUser.isDefined == true) {
		  execute(newUserString);
		  execute(projectString);
		}
		else {
		  execute(s"update $PROJECTS set primary_contact = '' where id = ${project.id}")
		}
	}

	def removeUserFromProject(user : User, project : Project) {
		val userString = s"update $USERS set projects = projects - {${project.id}} where username = '${user.username}'";
		execute(userString);

		val projectString = s"update $PROJECTS set team_members = team_members - {'${user.username}'} where id = ${project.id}";
		execute(projectString);
	}

	def removeProject(project : Project) {
		val executeString = s"delete from $PROJECTS where id=${project.id}";
		execute(executeString);
	}
}