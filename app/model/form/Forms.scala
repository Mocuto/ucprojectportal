package model.form

import java.util.Date

object Forms {
	case class UserForm(firstName : String, lastName : String, preferredPronouns : String, position : String, officeHourRequirement : Double);

	object OfficeHour {
	  def apply(date: Date, projectId: Int, hours: Double, comment: String, markAsUpdate: Boolean) : OfficeHour = {
	    new OfficeHour("", date, projectId, hours, comment, markAsUpdate)
	  }

	  def unapplyIncomplete(officeHour: OfficeHour): Option[(Date, Int, Double, String, Boolean)] = Some(officeHour.date, officeHour.projectId, officeHour.hours, officeHour.comment, officeHour.markAsUpdate)
	}

	case class OfficeHour(
	  username: String,
	  date: Date,
	  projectId: Int,
	  hours: Double,
	  comment: String,
	  markAsUpdate: Boolean
	)
}