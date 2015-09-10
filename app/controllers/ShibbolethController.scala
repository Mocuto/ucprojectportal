package controllers

import com.codahale.metrics.Counter
import com.kenshoo.play.metrics.MetricsRegistry

import java.util.Date

import model._
import model.UserPrivileges

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.libs.json._

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

import utils._

object ShibbolethController extends Controller with SessionHandler {

	def secure(path : String) = Action { implicit request => 

		if(constants.ServerSettings.AuthenticationMode == enums.AuthenticationMode.Login) {
			Redirect(routes.Application.login(path))
		}

		println(request.headers)

		(request.headers.get("eppn"), request.headers.get("givenName"), request.headers.get("sn"))  match {
			case (Some(eppn), Some(firstName), Some(lastName)) => {
				val username = utils.Conversions.eppnToUsername(eppn);
				println(username);

				User.get(username) match {
					case x : User if x.isDefined == false => {
						//Create the user in the Project Portal's database
						println(x);
						println("Creating user");
						User.createFromShibboleth(username, firstName, lastName);
						Redirect(routes.ActivationController.activateNEW(path)).withSession("authenticated" -> username)
					}
					case user : User => Redirect(routes.Application.index).withSession("authenticated" -> username)
				}
			}
			case _ => NotFound(views.html.messages.notFound("There was some kind of issue retrieving your information from the Central Login System. "))
		}


	}

	def secure : Action[play.api.mvc.AnyContent] = secure("")
}