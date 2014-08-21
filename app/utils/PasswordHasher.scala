package utils

import com.roundeights.hasher.Implicits._
import scala.language.postfixOps

object PasswordHasher {
	def hash(password : String, salt : String) : String = return password.bcrypt.hex

	def check(password : String, hashedPassword : String) : Boolean = return password.bcrypt hash= hashedPassword
}