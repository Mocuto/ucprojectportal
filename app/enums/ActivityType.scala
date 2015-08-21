package enums

import scala.Enumeration

object ActivityType extends Enumeration {
	type ActivityType = Value

	val ViewUser = Value
	val ViewProject, SubmitProject, RequestJoin, JoinProject, LeaveProject, EditProject, LikeProject, UnlikeProject, FollowProject, UnfollowProject = Value
	val AcceptRequest, IgnoreRequest = Value
	val SubmitUpdate, EditUpdate, DeleteUpdate, LikeUpdate, UnlikeUpdate = Value
	val FollowUser, UnfollowUser = Value

	val Toggleables = List(LikeProject, UnlikeProject, FollowProject, UnfollowProject, LikeUpdate, UnlikeUpdate, FollowUser, UnfollowUser)

	def fromString(str : String) : ActivityType = str.toLowerCase match {
		case "view-user" => ViewUser
		case "view-project" => ViewProject
		case "submit-project" => SubmitProject
		case "request-join" => RequestJoin
		case "join-project" => JoinProject
		case "leave-project" => LeaveProject
		case "edit-project" => EditProject
		case "like-project" => LikeProject
		case "unlike-project" => UnlikeProject
		case "follow-project" => FollowProject
		case "unfollow-project" => UnfollowProject
		case "accept-request" => AcceptRequest
		case "ignore-request" => IgnoreRequest
		case "submit-update" => SubmitUpdate
		case "edit-update" => EditUpdate
		case "delete-update" => DeleteUpdate
		case "like-update" => LikeUpdate
		case "unlike-update" => UnlikeUpdate
		case "follow-user" => FollowUser
		case "unfollow-user" => UnfollowUser
	}

	def toString(a : ActivityType) : String = a match {
		case ViewUser => "view-user"
		case ViewProject => "view-project"
		case SubmitProject => "submit-project"
		case RequestJoin => "request-join" 
		case JoinProject => "join-project"
		case LeaveProject => "leave-project"
		case EditProject =>"edit-project"
		case LikeProject => "like-project"
		case UnlikeProject => "unlike-project"
		case FollowProject => "follow-project"
		case UnfollowProject => "unfollow-project"
		case AcceptRequest => "accept-request"
		case IgnoreRequest => "ignore-request"
		case SubmitUpdate => "submit-update"
		case EditUpdate => "edit-update"
		case DeleteUpdate => "delete-update"
		case LikeUpdate	=> "like-update"
		case UnlikeUpdate => "unlike-update"
		case FollowUser => "follow-user"
		case UnfollowUser => "unfollow-user"
	}

	def invert(a : ActivityType) : ActivityType = a match {
		case LikeUpdate => UnlikeUpdate
		case UnlikeUpdate => LikeUpdate
		case LikeProject => UnlikeProject
		case UnlikeProject => LikeProject
		case FollowProject => UnfollowProject
		case UnfollowProject => FollowProject
		case FollowUser => UnfollowUser
		case UnfollowUser => FollowUser
		case _ => a
	}
}