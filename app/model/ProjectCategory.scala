package model;

import com.datastax.driver.core.Row

object ProjectCategory {
	def all : Seq[ProjectCategory] = return CassieCommunicator.getCategories;

	def undefined : ProjectCategory = return ProjectCategory("", isDefined = false);

	def fromRow(row : Row) : ProjectCategory = {
		row match {
			case null => ProjectCategory.undefined
			case row : Row => {
				return ProjectCategory(
					row.getString("name"),
					row.getBool("show_on_landing"),
					isDefined = true
				)
			}
		}
	}
}

case class ProjectCategory(name : String, showOnLanding : Boolean = false, isDefined : Boolean = true);