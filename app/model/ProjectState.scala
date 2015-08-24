package model

import com.datastax.driver.core.Row
import com.github.nscala_time.time.Imports._

import java.util.Date

import play.api.libs.json._
import play.api.mvc._

import scala.collection.JavaConversions._

import utils._
import utils.nosql.CassieCommunicator

object ProjectState {

	def undefined : ProjectState = ProjectState("", false);

	def all : Seq[ProjectState] = {
		return CassieCommunicator.getStates;
	}

	implicit def fromRow(row : Row) : ProjectState = {
		row match {
			case null => return ProjectState.undefined
			case row : Row => return ProjectState(
				name = row.getString("name"),
				true
			)
		}
	}

	final val COMPLETED = "completed"
	final val IDEA = "idea"
	final val IN_PROGRESS = "in progress"
	final val IN_PROGRESS_NEEDS_HELP = "in progress (needs help)"
	final val CLOSED = "frozen"

	def DEFAULT = IDEA
}

case class ProjectState(name : String, isDefined : Boolean = true)