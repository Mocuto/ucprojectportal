package utils.nosql

import com.datastax.driver.core.{Cluster, ResultSet, ResultSetFuture, Row}
import com.datastax.driver.core.exceptions._

import java.util.Date

import model._

import scala.collection.JavaConversions._

import utils.{Conversions, PasswordHasher}

trait ProjectRequestSqlCommunicator extends BaseSqlCommunicator {

	private[nosql] val REQUESTS : String
	private[nosql] val REQUESTS_INSERT_FIELDS : String

	def getRequest (projectId : Int, owner : String, requester : String) : ProjectRequest = {
		val executeString = s"select * from $REQUESTS where project_id = $projectId and owner = '$owner' and requester = '$requester'";
		executeAsync(executeString) match {
		  case None => return ProjectRequest.undefined
		  case Some(r : ResultSetFuture) => {
		    val row = r.getUninterruptibly().one();
		    return ProjectRequest.fromRow(row);
		  }
		}
	}

	def getRequests (projectId : Int, owner : String) : Seq[ProjectRequest] = {
		val executeString = s"select * from $REQUESTS where project_id = $projectId and owner = '$owner'";
		executeAsync(executeString) match {
		  case None => return List[ProjectRequest]()
		  case Some(r : ResultSetFuture) => {
		    val rows = r.getUninterruptibly().all();

		    return rows.map(row => ProjectRequest.fromRow(row));
		  }
		}
	}

	def addRequest(projectRequest : ProjectRequest) : ProjectRequest = {
		val timestamp = utils.Conversions.dateToStr(projectRequest.timeCreated);
		val executeString = s"insert into $REQUESTS ($REQUESTS_INSERT_FIELDS) values (${projectRequest.projectId} ,'${projectRequest.owner}', '${projectRequest.requester}', '$timestamp')";

		execute(executeString);

		return projectRequest;
	}

	def removeRequest(projectRequest : ProjectRequest) {
		if(projectRequest.isDefined == false) {
		  return;
		}

		val executeString = s"delete from $REQUESTS where project_id = ${projectRequest.projectId} and owner = '${projectRequest.owner}' and requester = '${projectRequest.requester}'";

		execute(executeString);
	}
}