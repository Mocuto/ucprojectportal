package model

import com.datastax.driver.core.Cluster
import com.datastax.driver.core.Row

import java.util.Date

import scala.collection.JavaConversions._

import utils.Conversions

//TODO: Prepare all statements

object CassieCommunicator {
	private val serverIp = "localhost"
	private val keyspace = "projectsg"

	private val USERS = "users"
  private val USER_INSERT_FIELDS = "username, first_name, last_name, password"

  private val USER_GROUPS = "user_groups"

	private val PROJECTS = "projects";
	private val PROJECTS_KEY = "id"
	private val PROJECT_INSERT_FIELDS = "id, description, team_members, name, primary_contact, state, categories, time_started"
  private val PROJECT_UPDATE_FIELDS = "id, description, state, categories, state_message, primary_contact"

	private val PROJECT_UPDATES = "project_updates";
	private val PROJECT_UPDATES_INSERT_FIELDS = "project_id, author, content, time_submitted, files"

	private val FILES = "files";
	private val FILES_INSERT_FIELDS = "project_id, time_submitted, filename, original_filename, author"

  private val CATEGORIES = "categories";

  private val STATES = "states";

  private val NOTIFICATIONS = "notifications"
  private val NOTIFICATIONS_INSERT_FIELDS = "username, time_created, content, type";

  private val REQUESTS = "requests"
  private val REQUESTS_INSERT_FIELDS = "project_id, owner, requester, time_created";

  private val TAGS = "tags";

	private val cluster = Cluster.builder().addContactPoint(serverIp).build();
	private val session = cluster.connect(keyspace);

  //TODO: Add exception catching and error handling system

  /*                  SETTERS                 */
  def setUserUnreadNotifications(user : User, num : Int) {
    val executeString = s"update $USERS set unread_notifications = $num where username = '${user.username}'"
    session.execute(executeString);
  }

  /*                  GETTERS                 */

	def getProjects : Seq[Project] = {
    	val columnName = "name";
    	val executeString = s"select * from $PROJECTS"

      val rows = session.executeAsync(executeString).getUninterruptibly().all();

    	return rows.map(row => Project.fromRow(row));
  }

  def getProjectsForUsername(username : String, projectColumnName : String = "projects") : Seq[Project] = {
    val nameColumnName = "name";
    var executeString = s"select $projectColumnName from $USERS where username='$username'";
    val projectIds : List[java.lang.Integer] = session.executeAsync(executeString).getUninterruptibly().one().getSet(s"$projectColumnName", classOf[java.lang.Integer]).toList;
    println(projectIds);
    println (projectColumnName);
    
    return projectIds.map(id => {
        executeString = s"select * from $PROJECTS where $PROJECTS_KEY=$id";

        val row = session.executeAsync(executeString).getUninterruptibly().one();

        Project.fromRow(row);
    })
  }

  def getPrimaryProjectsForUsername(username : String) : Seq [Project] = {
    val projectColumnName = "primary_contact_projects";
    return getProjectsForUsername(username, projectColumnName);
  }

	def getProject(projectId : Int) : Project = {
    val executeString = s"select * from $PROJECTS where $PROJECTS_KEY=$projectId";

    val row = session.executeAsync(executeString).getUninterruptibly().one();
    return Project.fromRow(row);
	}

	def getLatestStatusForProject(projectId: Int) : ProjectUpdate = {
		val executeString = s"select * from $PROJECT_UPDATES WHERE project_id=$projectId";

    val row = session.execute(executeString).one();

		return ProjectUpdate.fromRow(row);
	}

	def getStatusesForProject(projectId: Int) : Seq[ProjectUpdate] = {
  	val executeString = s"select * from $PROJECT_UPDATES WHERE project_id=$projectId";

    val rows = session.execute(executeString).all();
    rows.map( { row =>
            //ProjectUpdate(content = row.getString("content"), timeSubmitted = new LocalDate(row.getDate("time_submitted")))
            ProjectUpdate.fromRow(row);
	     }).sortWith((a, b) => a.timeSubmitted.after(b.timeSubmitted));
	}

	def getNumberOfStatusesForProject(projectId: Int) : Long = {
		val executeString = s"select count(*) from $PROJECT_UPDATES"

		return session.execute(executeString).one().getLong("COUNT");
	}

  def getNumberOfProjects() : Int = {
    val executeString = s"select count(*) from $PROJECTS";

    return session.execute(executeString).one().getLong("COUNT").toInt;
  }

  def getFilesForProject(project : Project) : Seq[ProjectFile] = {
    val executeString = s"select * from $FILES where project_id = ${project.id}";

    val rows = session.executeAsync(executeString).getUninterruptibly().all();

    return rows.map(row => ProjectFile.fromRow(row)).sortWith((a, b) => a.timeSubmitted.after(b.timeSubmitted))
  }

  def getUserWithUsernameAndPassword(username : String, password : String) : User = {
    val executeString = s"select * from $USERS where username='$username'";
    val row = session.executeAsync(executeString).getUninterruptibly().one();
    
    //TODO: Password hashing and salting here

    val user = User.fromRow(row);
    if (user.password.equals(password) == false) {
      println(s"wrong pass $password ${user.password}");
      return User.undefined;
    }
    return user;
  }

  def getUserWithUsername(username : String) : User = {
    val executeString = s"select * from $USERS where username='$username'";
    val result = session.executeAsync(executeString).getUninterruptibly().one();

    User.fromRow(result);
  }

  def getUsers : Seq[User] = {
    val executeString = s"select * from $USERS";
    val rows = session.executeAsync(executeString).getUninterruptibly().all();

    return rows.map(row => User.fromRow(row));
  }

  def getUserGroup(name : String) : UserGroup = {
    val executeString = s"select * from $USER_GROUPS where name = '$name'"
    val row = session.executeAsync(executeString).getUninterruptibly().one();

    return UserGroup.fromRow(row);
  }

  def getCategories : Seq[ProjectCategory] = {
    val executeString = s"select * from $CATEGORIES";
    val rows = session.executeAsync(executeString).getUninterruptibly().all();

    return rows.map(row => ProjectCategory.fromRow(row));
  }

  def getStates : Seq[ProjectState] = {
    val executeString = s"select * from $STATES";
    val rows = session.executeAsync(executeString).getUninterruptibly.all();

    return rows.map(row => ProjectState.fromRow(row));
  }

  def getTagsWithType (tagType : String) : Option[Seq[String]] = {
    val executeString = s"select * from $TAGS where type='$tagType'";
    val result = session.executeAsync(executeString).getUninterruptibly().one();

    if(result == null) {
      return None
    }

    return Some(result.getSet("value", classOf[String]).toList);
  }

  def getNotificationsForUser (user : User) : Seq[Notification] = {
    val executeString = s"select * from $NOTIFICATIONS where username = '${user.username}' order by time_created desc"
    val rows = session.executeAsync(executeString).getUninterruptibly().all();

    return rows.map(row => Notification.fromRow(row));
  }

  def getRequest (projectId : Int, owner : String, requester : String) : ProjectRequest = {
    val executeString = s"select * from $REQUESTS where project_id = $projectId and owner = '$owner' and requester = '$requester'";

    val row = session.executeAsync(executeString).getUninterruptibly().one();

    return ProjectRequest.fromRow(row);
  }

  def getRequest (projectId : Int, owner : String) : Seq[ProjectRequest] = {
    val executeString = s"select * from $REQUESTS where project_id = $projectId and owner = '$owner'";

    val rows = session.executeAsync(executeString).getUninterruptibly().all();

    return rows.map(row => ProjectRequest.fromRow(row));
  }

	def addUpdateForProject(update: ProjectUpdate) : ProjectUpdate = {
    val timestamp = Conversions.dateToStr(update.timeSubmitted)
    val content = update.content.replace("'", "''")
    val files = s"[" + update.files.map(file => s"'$file'").mkString(",") + s"]"
		val values = s"${update.projectId} ,'${update.author}', '$content', '$timestamp', $files"

		val executeString = s"insert into $PROJECT_UPDATES($PROJECT_UPDATES_INSERT_FIELDS) values($values)";
		println(executeString);

		session.execute(executeString);

    return update
	}


  def addProject(project : Project, primaryContact : String, categories : Seq[String], teamMembers : Seq[String]) : Project = {
    val id = CassieCommunicator.getNumberOfProjects();

    val categoriesStr = categories.map(category => s"'${category.replace("'", "''")}'").mkString(",");

    val teamMembersStr = teamMembers.map(member => s"'$member'").mkString(",")

    var executeString = s"insert into $PROJECTS($PROJECT_INSERT_FIELDS) VALUES($id, '${project.description.replace("'", "''")}', { $teamMembersStr}, '${project.name.replace("'", "''")}', '$primaryContact', 'in progress', { $categoriesStr } , dateOf(now()))";
    session.execute(executeString);

    executeString = s"update $USERS set primary_contact_projects = primary_contact_projects + { $id } where username='$primaryContact'";
    session.execute(executeString);

    executeString = s"update $USERS set projects = projects + { $id } where username='$primaryContact'";
    session.execute(executeString);
    return Project(id, project.name, project.description);
  }

  def addUserToProject(user : User, project : Project) {
    val userString = s"update $USERS set projects = projects + {${project.id}} where username = '${user.username}'";
    session.execute(userString);

    val projectString = s"update $PROJECTS set team_members = team_members + {'${user.username}'} where id = ${project.id}";
    session.execute(projectString);
  }

  def addUserToGroup(user : User, userGroup : UserGroup) {
    val executeString = s"update $USER_GROUPS set users = users + {'${user.username}'} where name = '${userGroup.name}'"
    session.executeAsync(executeString);
  }

  def addUser(user : User) {
    val executeString = s"insert into $USERS($USER_INSERT_FIELDS) VALUES('${user.username}', '${user.firstName}', '${user.lastName}', '${user.password}')"
    session.executeAsync(executeString);
  }

  def addFile(projectFile : ProjectFile) : ProjectFile = {

    val timestamp = Conversions.dateToStr(projectFile.timeSubmitted);
    val values = s"${projectFile.projectId}, '$timestamp', '${projectFile.filename}', '${projectFile.originalName}', '${projectFile.author}'"
  	val executeString = s"insert into $FILES($FILES_INSERT_FIELDS) VALUES($values)";
  	session.execute(executeString);

    return projectFile
  }

  def addTags(tags : Seq[String], tagType : String) {
    val existingTags = CassieCommunicator.getTagsWithType(tagType);
    if (existingTags.isEmpty) {
      return;
    }
    else {
      val newTagList = (existingTags.get ++ tags).distinct;
      val tagsStr = newTagList.map(_ => s"'_'").mkString(",");
      val executeString = s"update $TAGS set value= {$tagsStr} where type='$tagType'";
      session.execute(executeString);
    }
  }

  def addNotification(notification : Notification) : Notification = {
    val timestamp = utils.Conversions.dateToStr(notification.timeCreated)
    val contentStr = utils.Conversions.mapToStr(notification.content)
    val typeStr = NotificationType.toStr(notification.notificationType);
    val executeString = s"insert into $NOTIFICATIONS ($NOTIFICATIONS_INSERT_FIELDS) values ('${notification.username}', '$timestamp', $contentStr, '$typeStr')";

    println(executeString);
    session.execute(executeString);

    return notification;
  }

  def addRequest(projectRequest : ProjectRequest) : ProjectRequest = {
    val timestamp = utils.Conversions.dateToStr(projectRequest.timeCreated);
    val executeString = s"insert into $REQUESTS ($REQUESTS_INSERT_FIELDS) values (${projectRequest.projectId} ,'${projectRequest.owner}', '${projectRequest.requester}', '$timestamp')";

    session.execute(executeString);

    return projectRequest;
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
    var executeString = s"insert into $PROJECTS($PROJECT_UPDATE_FIELDS) VALUES(${project.id}, '${project.description.replace("'", "''")}', '${project.state}', { $categoriesStr }, '$stateMessage', '${project.primaryContact}')";
    println(executeString)
    session.execute(executeString);
  }

  def removeUserFromProject(user : User, project : Project) {
    val userString = s"update $USERS set projects = projects - {${project.id}} where username = '${user.username}'";
    session.execute(userString);

    val projectString = s"update $PROJECTS set team_members = team_members - {'${user.username}'} where id = ${project.id}";
    session.execute(projectString);
  }

  def changePrimaryContactForProject(oldUser : User, newUser : User, project : Project) {
    val oldUserString = s"update $USERS set primary_contact_projects = primary_contact_projects - {${project.id}} where username= '${oldUser.username}'";
    val newUserString = s"update $USERS set primary_contact_projects = primary_contact_projects + {${project.id}} where username= '${newUser.username}'";
    val projectString = s"update $PROJECTS set primary_contact = '${newUser.username}' where id = ${project.id}";

    session.execute(oldUserString);
    session.execute(newUserString);
    session.execute(projectString);
  }

  def removeNotification(notification : Notification) {
    val timestamp = utils.Conversions.dateToStr(notification.timeCreated);
    val executeString = s"delete from $NOTIFICATIONS where username='${notification.username}' and time_created='$timestamp'"

    println(executeString);
    session.execute(executeString);
  }

  def removeRequest(projectRequest : ProjectRequest) {
    if(projectRequest.isDefined == false) {
      return;
    }

    val executeString = s"delete from $REQUESTS where project_id = ${projectRequest.projectId} and owner = '${projectRequest.owner}' and requester = '${projectRequest.requester}'";

    session.execute(executeString);
  }



}