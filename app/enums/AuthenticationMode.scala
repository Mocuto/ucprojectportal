package enums

import scala.Enumeration

object AuthenticationMode extends Enumeration {
	val Login, Shibboleth, N_A = Value

	def fromString(str : String) = str.toLowerCase match {
		case "shibboleth" => Shibboleth
		case _ => Login
	}

}