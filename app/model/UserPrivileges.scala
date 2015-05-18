package model

import com.websudos.phantom.Implicits._

import utils.nosql.CassieCommunicator
import scala.concurrent.duration._

trait UserPrivileges

trait UserPrivilegesTable[A <: UserPrivileges] extends CassandraTable[UserPrivilegesTable[A], A] {
	object username extends StringColumn(this) with PartitionKey[String]
}

trait UserPrivilegesModel[A <: UserPrivileges, B <: UserPrivilegesTable[A]] {
	val T : B;

	val defaultTimeout = Duration.Inf;

    implicit val session = CassieCommunicator.session

	def get (username : String) : scala.concurrent.Future[Option[A]] = T.select.where (_.username eqs username).one()
	def getUninterruptibly (username: String) : Option[A] = scala.concurrent.Await.result(get(username), defaultTimeout)
}

object UserPrivileges {
	case class View (username : String, projects : Boolean, users : Boolean, accountability : Boolean, moderator : Boolean, admin : Boolean) extends UserPrivileges
	case class Create (username : String, projects : Boolean, updatesTheirProjects : Boolean, updatesAllProjects : Boolean, users : Boolean) extends UserPrivileges
	case class Edit (username : String, joinProjects : Boolean, projectsOwn : Boolean, projectsAll : Boolean, userPermissions : Boolean) extends UserPrivileges
	case class Follow (username : String, usersSome : Seq[String], usersAll : Boolean, projectsAll : Boolean) extends UserPrivileges
	case class Delete (username : String, updatesOwn : Boolean, updatesAll : Boolean, users : Boolean) extends UserPrivileges
}

sealed class UserPrivilegesView extends UserPrivilegesTable[UserPrivileges.View] //extends CassandraTable[UserPrivilegesView, UserPrivileges.View] 
{
	//object username extends StringColumn(this) with PartitionKey[String]
	object view_accountability extends BooleanColumn(this)
	object view_admin extends BooleanColumn(this)
	object view_moderator extends BooleanColumn(this)
	object view_projects extends BooleanColumn(this)
	object view_users extends BooleanColumn(this)

	override def fromRow(r : Row) = UserPrivileges.View(username(r), view_projects(r), view_users(r), view_accountability(r), view_moderator(r), view_admin(r));
}

object UserPrivilegesView extends UserPrivilegesView with UserPrivilegesModel[UserPrivileges.View, UserPrivilegesView] {
	val T = this;
    override val tableName = "user_privileges_view"
}

sealed class UserPrivilegesCreate extends UserPrivilegesTable[UserPrivileges.Create] {
	object create_project extends BooleanColumn(this)
	object create_users extends BooleanColumn(this)
	object update_projects_all extends BooleanColumn(this)
	object update_projects_self extends BooleanColumn(this)

	override def fromRow(r : Row) = UserPrivileges.Create(username(r), create_project(r), update_projects_self(r), update_projects_all(r), create_users(r))
}

object UserPrivilegesCreate extends UserPrivilegesCreate with UserPrivilegesModel[UserPrivileges.Create, UserPrivilegesCreate] {
	val T = this;
	override val tableName = "user_privileges_create"
}

sealed class UserPrivilegesEdit extends UserPrivilegesTable[UserPrivileges.Edit] {
	object edit_projects_all extends BooleanColumn(this)
	object edit_projects_self extends BooleanColumn(this)
	object edit_user_permissions extends BooleanColumn(this)
	object join_projects extends BooleanColumn(this);

	override def fromRow(r : Row) = UserPrivileges.Edit(username(r), join_projects(r), edit_projects_self(r), edit_projects_all(r), edit_user_permissions(r));
}

object UserPrivilegesEdit extends UserPrivilegesEdit with UserPrivilegesModel[UserPrivileges.Edit, UserPrivilegesEdit] {
	val T = this;
	override val tableName = "user_privileges_edit"
}