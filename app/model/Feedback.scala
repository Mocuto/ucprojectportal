package model

import com.datastax.driver.core.Row
import com.github.nscala_time.time.Imports._

import java.util.Date

import play.api.libs.json._
import play.api.mvc._

import scala.collection.JavaConversions._
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

import utils._
import utils.nosql.CassieCommunicator

object Feedback {

	def undefined = Feedback("", isDefined = false)

	def all : Seq[Feedback] = CassieCommunicator.getFeedback

	def create(author : String, content : String, feedbackType : String) : Feedback = {
		val timeSubmitted = new Date();

		return CassieCommunicator.addFeedback(author, content, feedbackType, timeSubmitted);
	}

	def fromRow(row : Row) : Feedback = {
		row match {
			case null => Feedback.undefined
			case (row : Row) => Feedback(
				row.getString("author"),
				row.getString("content"),
				row.getString("type"),
				row.getDate("time_submitted")
			)
		}
	}
}

case class Feedback(author : String, content : String = "" , feedbackType : String= "" , timeSubmitted : Date = new Date(), isDefined : Boolean = true)