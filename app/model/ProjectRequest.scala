package model

import com.datastax.driver.core.Row
import com.github.nscala_time.time.Imports._

import java.util.Date

import play.api.libs.json._
import play.api.mvc._

import scala.collection.JavaConversions._

object ProjectRequest {

	def undefined : ProjectRequest = return ProjectRequest(-1, "", "", isDefined = false);

	def get(projectId : Int, owner : String) : Seq [ProjectRequest] = {
		if (owner.length == 0) {
			return List[ProjectRequest]();
		}
		return CassieCommunicator.getRequest(projectId, owner);
	}

	def get(projectId : Int, owner : String, requester : String) : ProjectRequest = {
		if (owner.length == 0) {
			return ProjectRequest.undefined
		}
		return CassieCommunicator.getRequest(projectId, owner, requester);
	}

	def create(projectId : Int, owner : String, requester : String, timeCreated : Date) : ProjectRequest = {
		val request = ProjectRequest(projectId, owner, requester, timeCreated);
		CassieCommunicator.addRequest(request);
	}

	def delete(request : ProjectRequest) {
		CassieCommunicator.removeRequest(request);
	}

	def swapOwner(projectId : Int, oldOwner : String, newOwner : String) {
		val requests = ProjectRequest.get(projectId, oldOwner).foreach(x => {
			x.ignore();
			Notification.createRequest(User.get(newOwner), User.get(x.requester), Project.get(x.projectId));
		})
	}

	implicit def fromRow(row : Row) : ProjectRequest = {
		row match {
			case null => ProjectRequest.undefined
			case row : Row => return ProjectRequest(
				row.getInt("project_id"),
				row.getString("owner"),
				row.getString("requester"),
				row.getDate("time_created"),
				true
			) 
		}
	}
}

case class ProjectRequest(projectId : Int, owner : String,  requester : String, timeCreated : Date = new Date(), isDefined : Boolean = true) {
	def accept() {
		val user = User.get(requester);
		val project = Project.get(projectId);

		if(project.isDefined) {
			user.addToProject(project);
		}
		
		this.delete();
		Notification(owner, timeCreated).delete();
	}

	def ignore() {
		this.delete();
		Notification(owner, timeCreated).delete();
		//TODO: Remove notification
	}

	def delete() {
		CassieCommunicator.removeRequest(this);
	}
}