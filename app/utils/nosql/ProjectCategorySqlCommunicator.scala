package utils.nosql

import com.datastax.driver.core.{Cluster, ResultSet, ResultSetFuture, Row}
import com.datastax.driver.core.exceptions._

import java.util.Date

import model._

import scala.collection.JavaConversions._

import utils.{Conversions, PasswordHasher}


trait ProjectCategorySqlCommunicator extends BaseSqlCommunicator {

	private[nosql] val CATEGORIES : String

	def getCategories : Seq[ProjectCategory] = {
		val executeString = s"select * from $CATEGORIES";
		executeAsync(executeString) match {
		  case None => return List[ProjectCategory]()
		  case Some(r : ResultSetFuture) => {
		    val rows = r.getUninterruptibly().all();
		    return rows.map(row => ProjectCategory.fromRow(row));
		  }
		}

	}

	def getCategory(name : String) : ProjectCategory = {
		val executeString = s"select * from $CATEGORIES where name = '$name'"
		executeAsync(executeString) match {
		  case None => return ProjectCategory.undefined
		  case Some(r : ResultSetFuture) => {
		    val row = r.getUninterruptibly().one();
		    return ProjectCategory.fromRow(row);
		  }
		}
	}

}