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

	def undefined(implicit username : String) : A;
}

object UserPrivileges {
	case class View (username : String, projects : Boolean, users : Boolean, accountability : Boolean, moderator : Boolean, admin : Boolean) extends UserPrivileges
	case class Create (username : String, projects : Boolean, updatesTheirProjects : Boolean, updatesAllProjects : Boolean, users : Boolean) extends UserPrivileges
	case class Edit (username : String, joinProjects : Boolean, projectsOwn : Boolean, projectsAll : Boolean, userPermissions : Boolean) extends UserPrivileges
	case class Follow (username : String, usersSome : Set[String], usersAll : Boolean, projectsAll : Boolean) extends UserPrivileges
	case class Delete (username : String, updatesOwn : Boolean, updatesAll : Boolean, users : Boolean, projects : Boolean) extends UserPrivileges

	case class PrivilegeSet(view : View, create : Create, edit : Edit, follow : Follow, delete : Delete)

	def get(implicit username : String) : PrivilegeSet = PrivilegeSet(
		UserPrivilegesView.getUninterruptibly(username).getOrElse {   UserPrivilegesView.undefined },
		UserPrivilegesCreate.getUninterruptibly(username).getOrElse { UserPrivilegesCreate.undefined },
		UserPrivilegesEdit.getUninterruptibly(username).getOrElse {   UserPrivilegesEdit.undefined },
		UserPrivilegesFollow.getUninterruptibly(username).getOrElse { UserPrivilegesFollow.undefined },
		UserPrivilegesDelete.getUninterruptibly(username).getOrElse { UserPrivilegesDelete.undefined }
	)

	def innovator(username : String) = PrivilegeSet(
		View(username, true, true, false, false, false),
		Create(username, true, true, false, false),
		Edit(username, true, true, false, false),
		Follow(username, Set[String](), false, false),
		Delete(username, true, false, false, false)
	)


	def isInnovator(username : String) = get(username) == innovator(username);
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

    def undefined(implicit username : String) = UserPrivileges.View(username, false, false, false, false, false)

	def accountability(username : String) = UserPrivileges.View(username, false, false, true, false, false)
    def moderator(username : String) = UserPrivileges.View(username, false, false, false, true, false)
    def admin(username : String) = UserPrivileges.View(username, false, false, false, false, true)

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

	def undefined(implicit username : String) = UserPrivileges.Create(username, false, false, false, false)
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

	def undefined(implicit username : String) = UserPrivileges.Edit(username, false, false, false, false)
}

sealed class UserPrivilegesFollow extends UserPrivilegesTable[UserPrivileges.Follow] {
	object follow_projects_all extends BooleanColumn(this)
	object follow_users_all extends BooleanColumn(this)
	object follow_users_some extends SetColumn[UserPrivilegesTable[UserPrivileges.Follow], UserPrivileges.Follow, String](this)

	override def fromRow(r : Row) = UserPrivileges.Follow(username(r), follow_users_some(r), follow_users_all(r), follow_projects_all(r));
}

object UserPrivilegesFollow extends UserPrivilegesFollow with UserPrivilegesModel[UserPrivileges.Follow, UserPrivilegesFollow] {
	val T = this;
	override val tableName = "user_privileges_follow"

	def undefined(implicit username : String) = UserPrivileges.Follow(username, Set[String](), false, false);
}

sealed class UserPrivilegesDelete extends UserPrivilegesTable[UserPrivileges.Delete] {
	object delete_projects extends BooleanColumn(this)
	object delete_updates_all extends BooleanColumn(this)
	object delete_updates_self extends BooleanColumn(this)
	object delete_users extends BooleanColumn(this)

	override def fromRow(r: Row) = UserPrivileges.Delete(username(r), delete_updates_self(r), delete_updates_all(r), delete_users(r), delete_projects(r))
}

object UserPrivilegesDelete extends UserPrivilegesDelete with UserPrivilegesModel[UserPrivileges.Delete, UserPrivilegesDelete] {
	val T = this;
	override val tableName = "user_privileges_delete"

	def undefined(implicit username: String) = UserPrivileges.Delete(username, false, false, false, false);
}