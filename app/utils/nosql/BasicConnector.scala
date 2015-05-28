package utils.nosql

import com.datastax.driver.core.Session
import com.websudos.phantom.connectors.{KeySpace, SimpleCassandraConnector}

trait BasicConnector extends SimpleCassandraConnector {
  implicit val keySpace = KeySpace("projectsg")
}
