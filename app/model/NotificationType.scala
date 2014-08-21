package model

object NotificationType extends Enumeration {
	type NotificationType = Value

	val UPDATE, REQUEST, MESSAGE, ADDED_TO_PROJECT, NA = Value;

	def fromString(str : String) : NotificationType = {
		str.toLowerCase() match {
			case "request" => return NotificationType.REQUEST
			case "update" => return NotificationType.UPDATE
			case "message" => return NotificationType.MESSAGE
			case "added to project" => return NotificationType.ADDED_TO_PROJECT
			case _ => return NotificationType.NA
		}
	}

	def toStr(notificationType : NotificationType) : String = {
		notificationType match {
			case NotificationType.REQUEST => "request"
			case NotificationType.UPDATE => "update"
			case NotificationType.MESSAGE => "message"
			case NotificationType.ADDED_TO_PROJECT => "added to project"
			case NotificationType.NA => null
		}
	}
}

