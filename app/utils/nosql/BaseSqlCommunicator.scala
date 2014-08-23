package utils.nosql

import com.datastax.driver.core.{Cluster, ResultSet, ResultSetFuture, Row}
import com.datastax.driver.core.exceptions._

abstract class BaseSqlCommunicator {
	def defaultOnException(e : java.lang.RuntimeException,  executeString : String)

	def execute(executeString : String, onException : (java.lang.RuntimeException, String) => Unit = defaultOnException, doesDebug : Boolean = true) : Option[ResultSet]
	def executeAsync(executeString : String, onException : (java.lang.RuntimeException, String) => Unit = defaultOnException, doesDebug : Boolean = true) : Option[ResultSetFuture]
}