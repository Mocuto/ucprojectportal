package utils.nosql

import com.datastax.driver.core.{Cluster, ResultSet, ResultSetFuture, Row}
import com.datastax.driver.core.exceptions._

import java.util.Date

import model._

import scala.collection.JavaConversions._

import utils.{Conversions, PasswordHasher}

trait FeedbackSqlCommunicator extends BaseSqlCommunicator {
	private[nosql] val FEEDBACK : String
	private[nosql] val FEEDBACK_INSERT_FIELDS : String

	def getFeedback : Seq[Feedback] = {
		val executeString = s"select * from $FEEDBACK"
		executeAsync(executeString) match {
			case None => List[Feedback]()
			case Some(r : ResultSetFuture) => {
				val rows = r.getUninterruptibly().all();

				return rows.map(row => Feedback.fromRow(row));
			}
		}
	}

	def addFeedback(author : String, content : String, feedbackType : String, timeSubmitted : Date) : Feedback = {
		val timestamp = Conversions.dateToStr(timeSubmitted);
		val executeString = s"insert into $FEEDBACK($FEEDBACK_INSERT_FIELDS) values ('$author', '$content', '$feedbackType', '$timestamp')";
		execute(executeString)

		return Feedback(author, content, feedbackType, timeSubmitted);
	}

}