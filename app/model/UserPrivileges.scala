package model

import com.websudos.phantom.Implicits._
import com.datastax.driver.core.{Cluster, ResultSet, ResultSetFuture, Row}

import scala.concurrent._
import scala.concurrent.duration._

import utils.nosql.CassieCommunicator

trait UserPrivileges[A <: UserPrivileges[A]] {
	def isEmpty : Boolean;

	def union (that : A) : A;

	def | (that : A) = this union that

	def intersect (that : A) : A;

	def & (that : A) = this intersect that;

	def not : A;

	def unary_! = not;
}

trait UserPrivilegesTable[A <: UserPrivileges[A]] {
	val username : StringColumn[_, _] with PartitionKey[String]
}

trait UserPrivilegesModel[A <: UserPrivileges[A]] {
	self : CassandraTable[_ <: UserPrivilegesTable[A], A] =>

	
    implicit val session = CassieCommunicator.session

	def undefined(implicit username : String) : A

	def default(implicit username : String) : A

	def all = select.fetch()
	def allUninterruptibly = scala.concurrent.Await.result(all, constants.Cassandra.defaultTimeout)

	def get (username : String) : scala.concurrent.Future[Option[A]] = select.where (_.username eqs username).one()
	def getUninterruptibly (username: String) : Option[A] = scala.concurrent.Await.result(get(username), constants.Cassandra.defaultTimeout)

	def add (item : A) : scala.concurrent.Future[ResultSet]

	def replace (username : String, item : A) : scala.concurrent.Future[ResultSet]

	def create (username : String) : scala.concurrent.Future[ResultSet] = add (default (username))
}

object UserPrivileges {
	case class View (
		username : String,
		projects : Boolean = false,
		users : Boolean = false,
		accountability : Boolean = false,
		moderator : Boolean = false,
		admin : Boolean = false) extends UserPrivileges[View] {
		def isEmpty = !(projects || users || accountability || moderator || admin)

		def union (that : View) : View = View(
			s"$username,${that.username}",
			projects | that.projects,
			users | that.users,
			accountability | that.accountability,
			moderator | that.moderator,
			admin | that.admin
		)

		def intersect (that : View) : View = View(
			s"$username,${that.username}",
			projects & that.projects,
			users & that.users,
			accountability & that.accountability,
			moderator & that.moderator,
			admin & that.admin
		)

		def not = View(
			username,
			!projects,
			!users,
			!accountability,
			!moderator,
			!admin
		)
	}

	case class Create (
		username : String,
		projects : Boolean = false,
		updatesTheirProjects : Boolean = false,
		updatesAllProjects : Boolean = false,
		users : Boolean = false) extends UserPrivileges[Create] {
		def isEmpty = !(projects || updatesTheirProjects || updatesAllProjects || users)

		def union (that : Create) : Create = Create(
			s"$username,${that.username}",
			projects | that.projects,
			updatesTheirProjects | that.updatesTheirProjects,
			updatesAllProjects | that.updatesAllProjects,
			users | that.users
		)

		def intersect (that : Create) : Create = Create(
			s"$username,${that.username}",
			projects & that.projects,
			updatesTheirProjects & that.updatesTheirProjects,
			updatesAllProjects & that.updatesAllProjects,
			users & that.users
		)

		def not = Create(
			username,
			!projects,
			!updatesTheirProjects,
			!updatesAllProjects,
			!users
		)
	}

	case class Edit (
		username : String,
		joinProjects : Boolean = false,
		projectsOwn : Boolean = false,
		projectsAll : Boolean = false,
		updatesOwn : Boolean = false,
		updatesAll : Boolean = false,
		userPermissions : Boolean = false) extends UserPrivileges[Edit] {
		def isEmpty = !(joinProjects || projectsOwn || projectsAll || updatesOwn || updatesAll || userPermissions)

		def matches (that : Edit) = (
			joinProjects == that.joinProjects && 
			projectsOwn == that.projectsOwn &&
			projectsAll == that.projectsAll &&
			updatesOwn == that.updatesOwn &&
			updatesAll == that.updatesAll &&
			userPermissions == that.userPermissions)

		def union (that : Edit) = Edit(
			s"$username,${that.username}",
			joinProjects | that.joinProjects,
			projectsOwn | that.projectsOwn,
			projectsAll | that.projectsAll,
			updatesOwn | that.updatesOwn,
			updatesAll | that.updatesAll,
			userPermissions | that.userPermissions
		)

		def intersect (that : Edit) = Edit(
			s"$username,${that.username}",
			joinProjects & that.joinProjects,
			projectsOwn & that.projectsOwn,
			projectsAll & that.projectsAll,
			updatesOwn & that.updatesOwn,
			updatesAll & that.updatesAll,
			userPermissions & that.userPermissions)

		def >= (that : Edit) = !(
			(joinProjects == false && that.joinProjects == true) || 
			(projectsOwn == false && that.projectsOwn == true) ||
			(projectsAll == false && that.projectsAll == true) ||
			(updatesOwn == false && that.updatesOwn == true) ||
			(updatesAll == false && that.updatesAll == true) ||
			(userPermissions == false && that.userPermissions == true))

		def > (that : Edit) = this >= that && (this matches that) == false

		def not = Edit(
			username,
			!joinProjects,
			!projectsOwn,
			!projectsAll,
			!updatesOwn,
			!updatesAll,
			!userPermissions
		)
	}

	case class Follow (
		username : String,
		usersAll : Boolean = false,
		projectsAll : Boolean = false) extends UserPrivileges[Follow] {
		def isEmpty = !(usersAll || projectsAll)

		def union (that : Follow) : Follow = Follow(
			s"$username,${that.username}",
			usersAll | that.usersAll,
			projectsAll | that.projectsAll
		)

		def intersect (that : Follow) : Follow = Follow(
			s"$username,${that.username}",
			usersAll & that.usersAll,
			projectsAll & that.projectsAll
		)

		def not = Follow(
			username,
			!usersAll,
			!projectsAll
		)
	}

	case class Delete (
		username : String,
		updatesOwn : Boolean = false,
		updatesAll : Boolean = false,
		users : Boolean = false,
		projects : Boolean = false) extends UserPrivileges[Delete] {
		def isEmpty = !(updatesOwn || updatesAll || users || projects)

		def union(that : Delete) : Delete = Delete(
			s"$username,${that.username}",
			updatesOwn | that.updatesOwn,
			updatesAll | that.updatesAll,
			users | that.users,
			projects | that.projects
		)

		def intersect(that : Delete) : Delete = Delete(
			s"$username,${that.username}",
			updatesOwn & that.updatesOwn,
			updatesAll & that.updatesAll,
			users & that.users,
			projects & that.projects
		)

		def not = Delete (
			username,
			!updatesOwn,
			!updatesAll,
			!users,
			!projects
		)
	}

	case class PrivilegeSet(view : View, create : Create, edit : Edit, follow : Follow, delete : Delete)

	def get(implicit username : String) : PrivilegeSet = PrivilegeSet(
		UserPrivilegesView.getUninterruptibly(username).getOrElse {   UserPrivilegesView.undefined },
		UserPrivilegesCreate.getUninterruptibly(username).getOrElse { UserPrivilegesCreate.undefined },
		UserPrivilegesEdit.getUninterruptibly(username).getOrElse {   UserPrivilegesEdit.undefined },
		UserPrivilegesFollow.getUninterruptibly(username).getOrElse { UserPrivilegesFollow.undefined },
		UserPrivilegesDelete.getUninterruptibly(username).getOrElse { UserPrivilegesDelete.undefined }
	)

	def create(username : String) : Unit = {
		UserPrivilegesView.create(username);
		UserPrivilegesCreate.create(username);
		UserPrivilegesEdit.create(username);
		UserPrivilegesFollow.create(username);
		UserPrivilegesDelete.create(username);
	}

	def innovator(username : String) = PrivilegeSet(
		View(username, true, true, false, false, false),
		Create(username, true, true, false, false),
		Edit(username, true, true, false, false),
		Follow(username, false, false),
		Delete(username, true, false, false, false)
	)


	def isInnovator(username : String) = get(username) == innovator(username);
}

sealed class UserPrivilegesView extends CassandraTable[UserPrivilegesView, UserPrivileges.View] with UserPrivilegesTable[UserPrivileges.View]
{
	object username extends StringColumn(this) with PartitionKey[String]
	object view_accountability extends BooleanColumn(this)
	object view_admin extends BooleanColumn(this)
	object view_moderator extends BooleanColumn(this)
	object view_projects extends BooleanColumn(this)
	object view_users extends BooleanColumn(this)

	override def fromRow(r : Row) = UserPrivileges.View(username(r), view_projects(r), view_users(r), view_accountability(r), view_moderator(r), view_admin(r));
}

object UserPrivilegesView extends UserPrivilegesView with UserPrivilegesModel[UserPrivileges.View] {
	val T = this;
    override val tableName = "user_privileges_view"

    def undefined(implicit username : String) = UserPrivileges.View(username, false, false, false, false, false)

    def default(implicit username : String) = UserPrivileges.View(username, true, true, false, false, false)

	def accountability(username : String) = UserPrivileges.View(username, false, false, true, false, false)
    def moderator(username : String) = UserPrivileges.View(username, false, false, false, true, false)
    def admin(username : String) = UserPrivileges.View(username, false, false, false, false, true)

    def add(item : UserPrivileges.View) = {
    	insert.value(_.username, item.username)
    		.value(_.view_accountability, item.accountability)
    		.value(_.view_admin, item.admin)
    		.value(_.view_moderator, item.moderator)
    		.value(_.view_projects, item.projects)
    		.value(_.view_users, item.users)
    		.future();
    }

    def replace(username : String, item : UserPrivileges.View) = {
    	insert.value(_.username, username)
    		.value(_.view_accountability, item.accountability)
    		.value(_.view_admin, item.admin)
    		.value(_.view_moderator, item.moderator)
    		.value(_.view_projects, item.projects)
    		.value(_.view_users, item.users)
    		.future();
    }

}

sealed class UserPrivilegesCreate extends CassandraTable[UserPrivilegesCreate, UserPrivileges.Create] with UserPrivilegesTable[UserPrivileges.Create]  {
	object username extends StringColumn(this) with PartitionKey[String]
	object create_project extends BooleanColumn(this)
	object create_users extends BooleanColumn(this)
	object update_projects_all extends BooleanColumn(this)
	object update_projects_self extends BooleanColumn(this)

	override def fromRow(r : Row) = UserPrivileges.Create(username(r), create_project(r), update_projects_self(r), update_projects_all(r), create_users(r))
}

object UserPrivilegesCreate extends UserPrivilegesCreate with UserPrivilegesModel[UserPrivileges.Create] {
	val T = this;
	override val tableName = "user_privileges_create"

	def undefined(implicit username : String) = UserPrivileges.Create(username, false, false, false, false)
	def default(implicit username : String) = UserPrivileges.Create(username, false, false, false, false)

    def add(item : UserPrivileges.Create) = {
    	insert.value(_.username, item.username)
    		.value(_.create_project, item.projects)
    		.value(_.create_users, item.users)
    		.value(_.update_projects_all, item.updatesAllProjects)
    		.value(_.update_projects_self, item.updatesTheirProjects)
    		.future();
    }

    def replace(username : String, item : UserPrivileges.Create) = {
    	insert.value(_.username, username)
    		.value(_.create_project, item.projects)
    		.value(_.create_users, item.users)
    		.value(_.update_projects_all, item.updatesAllProjects)
    		.value(_.update_projects_self, item.updatesTheirProjects)
    		.future();
    }
}

sealed class UserPrivilegesEdit extends CassandraTable[UserPrivilegesEdit, UserPrivileges.Edit] with UserPrivilegesTable[UserPrivileges.Edit] {
	object username extends StringColumn(this) with PartitionKey[String]
	object edit_projects_all extends BooleanColumn(this)
	object edit_projects_self extends BooleanColumn(this)
	object edit_updates_all extends BooleanColumn(this)
	object edit_updates_self extends BooleanColumn(this)
	object edit_user_permissions extends BooleanColumn(this)
	object join_projects extends BooleanColumn(this);

	override def fromRow(r : Row) = UserPrivileges.Edit(
		username(r),
		join_projects(r),
		edit_projects_self(r),
		edit_projects_all(r),
		edit_updates_self(r),
		edit_updates_all(r),
		edit_user_permissions(r));
}

object UserPrivilegesEdit extends UserPrivilegesEdit with UserPrivilegesModel[UserPrivileges.Edit] {
	val T = this;
	override val tableName = "user_privileges_edit"

	def undefined(implicit username : String) = UserPrivileges.Edit(username, false, false, false, false)

	def default(implicit username : String) = UserPrivileges.Edit(username, false, false, false, false)

    def add(item : UserPrivileges.Edit) = {
    	insert.value(_.username, item.username)
    		.value(_.edit_projects_all, item.projectsAll)
    		.value(_.edit_projects_self, item.projectsOwn)
    		.value(_.edit_updates_all, item.updatesAll)
    		.value(_.edit_updates_self, item.updatesOwn)
    		.value(_.edit_user_permissions, item.userPermissions)
    		.value(_.join_projects, item.joinProjects)
    		.future();
    }

    def replace(username : String, item : UserPrivileges.Edit) = {
    	println("replace")
    	println(item)
    	insert.value(_.username, username)
    		.value(_.edit_projects_all, item.projectsAll)
    		.value(_.edit_projects_self, item.projectsOwn)
    		.value(_.edit_updates_all, item.updatesAll)
    		.value(_.edit_updates_self, item.updatesOwn)
    		.value(_.edit_user_permissions, item.userPermissions)
    		.value(_.join_projects, item.joinProjects)
    		.future();
    }

    def whoMatch(term : UserPrivileges.Edit) : Future[Seq[UserPrivileges.Edit]] = select.fetch().map(_.filter(_ matches term))

    def whoMatchOrExceed(term : UserPrivileges.Edit) : Future[Seq[UserPrivileges.Edit]] = select.fetch().map(_.filter(_ >= term))
}

sealed class UserPrivilegesFollow extends CassandraTable[UserPrivilegesFollow, UserPrivileges.Follow] with UserPrivilegesTable[UserPrivileges.Follow]   {
	object username extends StringColumn(this) with PartitionKey[String]
	object follow_projects_all extends BooleanColumn(this)
	object follow_users_all extends BooleanColumn(this)

	override def fromRow(r : Row) = UserPrivileges.Follow(username(r), follow_users_all(r), follow_projects_all(r));
}

object UserPrivilegesFollow extends UserPrivilegesFollow with UserPrivilegesModel[UserPrivileges.Follow] {
	val T = this;
	override val tableName = "user_privileges_follow"

	def undefined(implicit username : String) = UserPrivileges.Follow(username, false, false);

	def default(implicit username : String) = UserPrivileges.Follow(username, false, true);

    def add(item : UserPrivileges.Follow) = {
    	insert.value(_.username, item.username)
    		.value(_.follow_projects_all, item.projectsAll)
    		.value(_.follow_users_all, item.usersAll)
    		.future();
    }

    def replace(username : String, item : UserPrivileges.Follow) = {
    	insert.value(_.username, username)
    		.value(_.follow_projects_all, item.projectsAll)
    		.value(_.follow_users_all, item.usersAll)
    		.future();
    }
}

sealed class UserPrivilegesDelete extends CassandraTable[UserPrivilegesDelete, UserPrivileges.Delete] with UserPrivilegesTable[UserPrivileges.Delete]  {
	object username extends StringColumn(this) with PartitionKey[String]
	object delete_projects extends BooleanColumn(this)
	object delete_updates_all extends BooleanColumn(this)
	object delete_updates_self extends BooleanColumn(this)
	object delete_users extends BooleanColumn(this)

	override def fromRow(r: Row) = UserPrivileges.Delete(username(r), delete_updates_self(r), delete_updates_all(r), delete_users(r), delete_projects(r))
}

object UserPrivilegesDelete extends UserPrivilegesDelete with UserPrivilegesModel[UserPrivileges.Delete] {
	val T = this;
	override val tableName = "user_privileges_delete"

	def undefined(implicit username: String) = UserPrivileges.Delete(username, false, false, false, false);

	def default(implicit username: String) = UserPrivileges.Delete(username, false, false, false, false);

    def add(item : UserPrivileges.Delete) = {
    	insert.value(_.username, item.username)
    		.value(_.delete_projects, item.projects)
    		.value(_.delete_updates_all, item.updatesAll)
    		.value(_.delete_updates_self, item.updatesOwn)
    		.value(_.delete_users, item.users)
    		.future();
    }

    def replace(username : String, item : UserPrivileges.Delete) = {
    	insert.value(_.username, username)
    		.value(_.delete_projects, item.projects)
    		.value(_.delete_updates_all, item.updatesAll)
    		.value(_.delete_updates_self, item.updatesOwn)
    		.value(_.delete_users, item.users)
    		.future();
    }
}