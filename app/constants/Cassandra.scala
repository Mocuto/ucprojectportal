package constants

import com.datastax.driver.core.{Cluster, ResultSet, ResultSetFuture, Row}
import scala.concurrent.duration._

object Cassandra {
	type FutureResultSet = scala.concurrent.Future[ResultSet]

	val defaultTimeout = Duration.Inf;
}