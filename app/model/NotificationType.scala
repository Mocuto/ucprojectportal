package model

object NotificationType extends Enumeration {
	type NotificationType = Value

	val UPDATE, REQUEST, MESSAGE, NA = Value;

	def fromString(str : String) : NotificationType = {
		str match {
			case "request" => return NotificationType.REQUEST
			case "update" => return NotificationType.UPDATE
			case "message" => return NotificationType.MESSAGE
			case _ => return NotificationType.NA
		}
	}

	def toStr(notificationType : NotificationType) : String = {
		notificationType match {
			case NotificationType.REQUEST => "request"
			case NotificationType.UPDATE => "update"
			case NotificationType.MESSAGE => "message"
			case NotificationType.NA => null
		}
	}
}

