package controllers

import com.typesafe.plugin._

import java.util.Date

import model._

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.libs.json._

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

import utils._

object Application extends Controller {

implicit val projectUpdateForm = Form(
	mapping(
		"content" -> nonEmptyText,
		"project_id" -> number,
		"date" -> ignored(new Date())
	) (ProjectUpdate.applyIncomplete)(ProjectUpdate.unapplyIncomplete)
)

implicit val loginForm : Form[(String, String)] = Form(
	tuple(
		"username" -> nonEmptyText,
		"password" -> nonEmptyText
	).verifying("Incorrect username or password", fields => fields match {
		case (username, password) => { User.authenticate(username, password).isDefined}
	})
)

implicit val projectForm = Form(
	mapping(
		"name" -> nonEmptyText,
		"description" -> nonEmptyText,
		"categories" -> list(nonEmptyText),
		"team-members" -> list(nonEmptyText)
	) (Project.apply)(Project.unapplyIncomplete).verifying("Insert invalid project categories message here", fields => fields match {
		case project => { println("project.categories.length: " + project.categories.length);  project.categories.length > 0}
	})
)

implicit val newUserForm = Form(
	tuple(
		"username" -> nonEmptyText,
		"first_name" -> nonEmptyText,
		"last_name" -> nonEmptyText,
		"password" -> nonEmptyText
	)
)

def checkAuthenticated (request : Request[play.api.mvc.AnyContent])(success : (String) => Result ) : Result = { 
	request.session.get("authenticated").map {
		authenticatedUser => success(authenticatedUser)
	}.getOrElse {
		Redirect(routes.Application.login(request.path));
	}
}

def javascriptRoutes = Action { implicit request =>
	import routes.javascript._
	Ok(
		Routes.javascriptRouter("jsRoutes")
		(
		 routes.javascript.Application.submitUpdate,
		 routes.javascript.Application.editProject,
		 routes.javascript.Application.leaveProject,
		 routes.javascript.Application.requestJoin,
		 routes.javascript.Application.acceptRequest,
		 routes.javascript.Application.ignoreRequest,
		 routes.javascript.Application.resetUnreadNotifications,
		 routes.javascript.Application.getUnreadNotificationCount,
		 routes.javascript.Application.ignoreNotification
		 )
	).as("text/javascript")
}

def index = Action { implicit request => {
/*	request.session.get("authenticated").map {
		authenticatedUser => {
			authenticatedUser match {
				case null => Redirect(routes.Application.login)
				case _ => {
					val user = User.get(authenticatedUser);
					Ok(views.html.index(user))
				}
			}

		}
	}.getOrElse {
		Redirect(routes.Application.login);
	}
}*/
	val authenticatedUser = request.session.get("authenticated").get;
	val user = User.get(authenticatedUser);
	Ok(views.html.index(user));

}}


def filter(filterStr : String) = Action { implicit request => {
	checkAuthenticated(request) {
		authenticatedUsername : String => {
			val authenticatedUser = User.get(authenticatedUsername);
			Ok(views.html.filter(authenticatedUser, filterStr))
		}
	}
}}

def project(id : Int) = Action { request => {
	checkAuthenticated(request) {
		authenticatedUser : String => { 
			val project = Project.get(id);
			if(project.isDefined == false) {
				NotFound(views.html.notFound("This project does not exist"));
			}
			else {
				val updates = CassieCommunicator.getStatusesForProject(id);
				val user = User.get(authenticatedUser);
				user.projects.foreach{project : Project => println(project)}
				val isPrimaryContact = user.primaryProjects.map { _.id }.exists(_ == id);
				Ok(views.html.project(project, updates, authenticatedUser, isPrimaryContact))
			}
		}
	}
  }
}

def newProject = Action {request => {

	checkAuthenticated(request) {
		authenticatedUser : String => Ok(views.html.newProject(User.get(authenticatedUser)));
	}
}
}

def user(username : String) = Action { implicit request => {
	request.session.get("authenticated").map {
		authenticatedUser => {
			val user = User.get(username)
			if (user.isDefined == false) {
				NotFound(views.html.notFound("This user does not exist"));
			} else {
				val loggedInUser = User.get(authenticatedUser);
				Ok(views.html.user(user, loggedInUser)(username == authenticatedUser));
			}
		}
	}.getOrElse {
		Redirect(routes.Application.login(request.path))
	}
}
}

def login(path : String) = Action {
	Ok(views.html.login(path)(loginForm));
}

def login : Action[play.api.mvc.AnyContent] = login("");

def uploads(filename : String) = Action {
	val file : java.io.File = new java.io.File(s"uploads/$filename");
	if (file.exists()) {
	  	Ok.sendFile(
		    content = file,
		    fileName = _ => utils.Conversions.stripUUID(filename)
  		)
	}
	else {
		BadRequest(s"$filename does not exist")
	}

}

def submitUpdate = Action { implicit request =>
	checkAuthenticated(request) {
		authenticatedUser : String => {
			projectUpdateForm.bindFromRequest.fold(
			  formWithErrors => {
			    // binding failure, you retrieve the form containing errors:
			    //BadRequest(views.html.user(formWithErrors))
			    var errorMessage : String = "";
			    formWithErrors.errors map { error  => {
			    		errorMessage = s"$errorMessage ${error.key}";
			    	}
			    }
				BadRequest(errorMessage);
			  },
			  update => {
			    /* binding success, you get the actual value. */
			    val project = Project.get(update.projectId);

			    if(project.teamMembers.contains(authenticatedUser) == false) {
			    	Status(462)("You are not a member of this project");
			    }
			    else if(project.isDefined == false) {
			    	Status(404)("This project does not exist")
			    }
			    else {
				    val multipartFormData = request.body.asMultipartFormData.get
				    val files = multipartFormData.files.map(filepart => (filepart.filename, filepart.ref));

				    val completeUpdate = ProjectUpdate.create(update.content, authenticatedUser, update.projectId, files = files);

				    Future {
				    	project.notifyMembersExcluding(authenticatedUser, completeUpdate.content);
					}

				    val response = JsObject(
				    	Seq(
			    			"html" -> JsString(views.html.updateView(completeUpdate).toString)
				    	)
				    )

				    Ok(response);
			    }
			  }
			)
		}
	}
}

def submitProject = Action { implicit request =>
	checkAuthenticated(request) {
		authenticatedUsername : String => {
			projectForm.bindFromRequest.fold(
				formWithErrors => {
					BadRequest(views.html.newProject(User.get(authenticatedUsername))(formWithErrors))
				},
				incompleteProject => {
					println(s"cate length ${incompleteProject.categories.length}");
					var completeProject = Project.create(incompleteProject.name, incompleteProject.description, authenticatedUsername,
						incompleteProject.categories, incompleteProject.teamMembers);
					//TODO get project with id, redirect to that project's page
					Redirect(routes.Application.project(completeProject.id));
				}
			)
		}
	}

}

def editProject(id : Int) = Action { implicit request =>
	checkAuthenticated(request) {
		authenticatedUsername : String => {
			val project = Project.get(id);

			if(project.primaryContact != authenticatedUsername) 
			{
				Status(401)("You are not authorized to edit this project");
			}
			else if(request.body.asMultipartFormData == None) 
			{
				Status(400)("No data found");
			}
			else if(project.isDefined == false) {
				Status(400)("This project does not exist")
			}
			else 
			{
				val dataParts = request.body.asMultipartFormData.get.dataParts

				val state = dataParts.getOrElse("state", List(project.state))(0)

				val isFinished = (state == ProjectState.COMPLETED || state == ProjectState.CLOSED);

				val updatedProject = Project(
					id = project.id,
					name = project.name,
					description = dataParts.getOrElse("description", List(project.description))(0),
					categories = dataParts.getOrElse("categories", project.categories),
					state = state,
					stateMessage = dataParts.getOrElse("state-message", List(project.stateMessage))(0),
					teamMembers = dataParts.getOrElse("team-members", project.teamMembers),
					primaryContact = dataParts.getOrElse("primary-contact", List(project.primaryContact))(0),
					timeFinished = if(isFinished) new Date() else null
				)

				updatedProject.update();

				println(dataParts)

				for(username <- updatedProject.teamMembers ++ project.teamMembers) {
					val user = User.get(username);
					if(project.teamMembers.contains(username) == false) {
						user.addToProject(updatedProject);
					}
					else if(updatedProject.teamMembers.contains(username) == false && username != updatedProject.primaryContact) {
						user.removeFromProject(updatedProject);
					}
				}

				if(updatedProject.primaryContact != project.primaryContact) {
					updatedProject.changePrimaryContact(User.get(project.primaryContact), User.get(updatedProject.primaryContact))
				}

				val response = JsObject( 
					Seq(
						"response" -> JsString("project edited")
					)
				)

				Ok(response);
			}
		}
	}
}

def leaveProject(id : Int) = Action { implicit request =>
	checkAuthenticated(request) {
		authenticatedUsername : String => {
			val project = Project.get(id);

			if(project.teamMembers.contains(authenticatedUsername) == false) {
				Status(404)("You are not on this project team")
			}
			else if(project.isDefined == false) {
				Status(400)("This project does not exist")
			}
			else {
				val authenticatedUser = User.get(authenticatedUsername)

				if(project.primaryContact == authenticatedUsername) {
					if(project.teamMembers.length == 1) {
						project.changePrimaryContact(authenticatedUser, User.undefined);
					}
					else {
						var otherUser : User = null;
						project.teamMembers.toStream.takeWhile(_ => otherUser == null).foreach({ member =>
							if(member != authenticatedUsername) {
								otherUser = User.get(member);
							}
						})
						project.changePrimaryContact(authenticatedUser, otherUser)
					}
				}
				authenticatedUser.removeFromProject(project);
				
				val response = JsObject( 
					Seq(
						"response" -> JsString("left project")
					)
				)

				Ok(response);
			}
		}
	}

}

def tryLogin(path : String) = Action { implicit request =>
	loginForm.bindFromRequest.fold(
		formWithErrors => {
			BadRequest(views.html.login()(formWithErrors))
		},
		loginData => {
			println(loginData._1)
			if(path == "") {
				Redirect(routes.Application.index).withSession(
					"authenticated" -> loginData._1			)
			}
			else {
				Redirect(path).withSession(
					"authenticated" -> loginData._1			)
			}

		}
	)
}

def requestJoin(projectId : Int) = Action { implicit request =>
	checkAuthenticated(request) {
		authenticatedUsername : String => {
			val project = Project.get(projectId);
			val receiver = User.get(project.primaryContact);
			val authenticatedUser = User.get(authenticatedUsername);

			project match{
				case project if project.isDefined == false => { //Check if the project with that id does not exist
					Status(404)(s"project with id = $projectId does not exist")
				}
				case project if authenticatedUser.projects.contains(project) => { //Check if they're already in that project
					Status(460)(s"already member of project with id = $projectId")
				}
				case _ => {
					val success = Notification.createRequest(receiver, authenticatedUser, project);

					if (success == false) {
						Status(461)(s"project request already sent")
					}
					else {
						val response = JsObject(
							Seq(
								"response" -> JsString("your request has been sent")
							)
						)

						Ok(response);
					}


				}
			}
		}
	}
}

def acceptRequest(projectId : Int, requester : String)  = Action{ implicit request =>
	checkAuthenticated(request) {
		authenticatedUsername : String => {
			val projectRequest = ProjectRequest.get(projectId, authenticatedUsername, requester);
			
			if (projectRequest.isDefined == false) {
				Status(404)("request does not exist");
			}
			else {
				val project = Project.get(projectId);
				if(project.isDefined == false) {
					NotFound("project does not exist");
				}
				else {
					projectRequest.accept();

					val response = JsObject(
						Seq(
							"response" -> JsString("request accepted")
						)
					)

					val user = User.get(requester);

					Ok(response)				
				}


			}
		}
	}
}

def ignoreRequest(projectId : Int, requester : String)  = Action{ implicit request =>
	checkAuthenticated(request) {
		authenticatedUsername : String => {
			val projectRequest = ProjectRequest.get(projectId, authenticatedUsername, requester);
			
			if (projectRequest.isDefined == false) {
				Status(404)("request does not exist");
			}
			else {
				projectRequest.ignore();

				val response = JsObject(
					Seq(
						"response" -> JsString("request ignored")
					)
				)

				Ok(response)
			}
		}
	}
}
def resetUnreadNotifications() = Action { implicit request =>
	checkAuthenticated(request) {
		authenticatedUsername : String => {
			val authenticatedUser = User.get(authenticatedUsername);
			Notification.resetUnreadForUser(authenticatedUser);

			val response = JsObject(
				Seq(
					"response" -> JsString("notifications have been reset")
				)
			)

			Ok(response);
		}
	}
}

def getUnreadNotificationCount = Action { implicit request =>
	checkAuthenticated(request) {
		authenticatedUsername : String => {
			val authenticatedUser = User.get(authenticatedUsername);

			val response = JsObject(
				Seq(
					"count" -> JsNumber(authenticatedUser.unreadNotifications),
					"html" -> JsString(views.html.notificationsListView(authenticatedUser).toString)
				)
			)

			Ok(response);
		}
	}
}

def ignoreNotification (timeCreated : String) = Action { implicit request =>
	println(s"timeCreated $timeCreated")
	checkAuthenticated(request) {
		authenticatedUsername : String => {
			val notification = Notification(authenticatedUsername, utils.Conversions.strToDate(timeCreated));
			notification.delete();
			val response = JsObject(
				Seq(
					"response" -> JsString("notification has been ignored")
				)
			)
			Ok(response);
		}
	}
}

def admin = Action { implicit request =>
	checkAuthenticated(request) {
		authenticatedUsername : String => {
			val authenticatedUser = User.get(authenticatedUsername);
			val isAdmin = UserGroup.isUserInGroup(authenticatedUser, "admin");

			if(isAdmin == false) {
				NotFound(views.html.notFound("this page does not exist"));
			}
			else {
				Ok(views.html.admin(authenticatedUser));
				//Ok(views.html.emailMessage("This is a test message too!"))
			}
		}
	}
}

def createUser = Action { implicit request => 
	checkAuthenticated(request) {
		authenticatedUsername : String => {
			val authenticatedUser = User.get(authenticatedUsername);
			val isAdmin = UserGroup.isUserInGroup(authenticatedUser, "admin");

			if(isAdmin == false) {
				NotFound(views.html.notFound("this page does not exist"));
			}
			else {
				newUserForm.bindFromRequest.fold(
					formWithErrors => {
						//TODO: Put in error message here
						BadRequest("")

					}, newUserData => {
						newUserData match {
							case (username : String, firstName : String, lastName : String, password : String) => {
								val newUser = User(
									username = username,
									firstName = firstName,
									lastName = lastName,
									password = password
								)

								User.create(newUser);

								Redirect(routes.Application.admin);
							}
							case _ => {
								//TODO: Put in error message here
								BadRequest("")
							}
						}
					}
				)
			}
		}
	}
}

def signout = Action { implicit request =>
	Redirect(routes.Application.login("")).withSession(
		request.session - "authenticated"
	)

}

}