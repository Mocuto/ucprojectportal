package utils.nosql

import com.datastax.driver.core.{Cluster, ResultSet, ResultSetFuture, Row}
import com.datastax.driver.core.exceptions._

import java.util.Date

import model._

import scala.collection.JavaConversions._

import utils.{Conversions, PasswordHasher}

import play.api.Logger
import play.Play

object CassieCommunicator extends BaseSqlCommunicator with NotificationSqlCommunicator with ProjectCategorySqlCommunicator
    with ProjectFileSqlCommunicator with ProjectRequestSqlCommunicator with ProjectSqlCommunicator
    with ProjectUpdateSqlCommunicator with UserSqlCommunicator {

  private val serverIp = Play.application.configuration.getString("cassandra.host");
  private val keyspace = Play.application.configuration.getString("cassandra.keyspace")

	private[nosql] val USERS = "users"
	private[nosql] val USER_INSERT_FIELDS = "username"

	private[nosql] val USER_GROUPS = "user_groups"

  private[nosql] val USER_ACTIVATION = "user_activation"
  private[nosql] val USER_ACTIVATION_INSERT_FIELDS = "username, code"

  private[nosql] val USER_AUTHENTICATION = "user_authentication"
  private[nosql] val USER_AUTHENTICATION_INSERT_FIELDS = "username, password";

	private[nosql] val PROJECTS = "projects";
	private[nosql] val PROJECTS_KEY = "id"
	private[nosql] val PROJECT_INSERT_FIELDS = "id, description, team_members, name, primary_contact, state, categories, time_started"
	private[nosql] val PROJECT_UPDATE_FIELDS = "id, name, description, state, categories, state_message, primary_contact, time_finished"

	private[nosql] val PROJECT_UPDATES = "project_updates";
	private[nosql] val PROJECT_UPDATES_INSERT_FIELDS = "project_id, author, content, time_submitted, files"

	private[nosql] val FILES = "files";
	private[nosql] val FILES_INSERT_FIELDS = "project_id, time_submitted, filename, original_filename, author"

	private[nosql] val CATEGORIES = "categories";

	private[nosql] val STATES = "states";

	private[nosql] val NOTIFICATIONS = "notifications"
	private[nosql] val NOTIFICATIONS_INSERT_FIELDS = "username, time_created, content, type";

	private[nosql] val REQUESTS = "requests"
	private[nosql] val REQUESTS_INSERT_FIELDS = "project_id, owner, requester, time_created";

	private[nosql] val TAGS = "tags";

	private[nosql] val cluster = Cluster.builder().addContactPoint(serverIp).build();
	private[nosql] val session = cluster.connect(keyspace);

	private[nosql] val logger = Logger(this.getClass())

  //TODO: Add exception catching and error handling system

  def defaultOnException(e : java.lang.RuntimeException,  executeString : String) {
    logger.error(s"Exception with $executeString", e);
  }

  def execute(executeString : String, onException : (java.lang.RuntimeException, String) => Unit = defaultOnException, doesDebug : Boolean = true) : Option[ResultSet] = {
    
    if(doesDebug){
      logger.debug(executeString);
    }

    try {
      val statement = session.prepare(executeString);
      return Some(session.execute(statement.bind()));
    }
    catch {
      case e: java.lang.RuntimeException => {
        onException(e, executeString);
        return None;
      }
    }
  }

  def executeAsync(executeString : String, onException : (java.lang.RuntimeException, String) => Unit = defaultOnException, doesDebug : Boolean = true) : Option[ResultSetFuture] = {

    if(doesDebug) {
     logger.debug(executeString);     
    }

    try {
      val statement = session.prepare(executeString);
      return Some(session.executeAsync(statement.bind()));
    }
    catch {
      case e: java.lang.RuntimeException => {
        onException(e, executeString);
        return None;
      }
    }
  }

  def getStates : Seq[ProjectState] = {
    val executeString = s"select * from $STATES";
    executeAsync(executeString) match {
      case None => return List[ProjectState]()
      case Some(r : ResultSetFuture) => {
        val rows = r.getUninterruptibly.all();

        return rows.map(row => ProjectState.fromRow(row));        
      }
    }
  }

  def getTagsWithType (tagType : String) : Option[Seq[String]] = {
    val executeString = s"select * from $TAGS where type='$tagType'";
    executeAsync(executeString) match {
      case None => return None;
      case Some(r : ResultSetFuture) => {
        val row = r.getUninterruptibly().one();
        row match {
          case null => None;
          case _ => return Some(row.getSet("value", classOf[String]).toList)
        }  
      }
    }
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
      execute(executeString);
    }
  }

}