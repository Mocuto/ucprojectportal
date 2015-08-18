package utils

import java.text.SimpleDateFormat
import java.util.Date

import model._

import org.joda.time._

import play.api.libs.json._
import play.api.Logger

import scala.collection.JavaConversions._

object Conversions {
  //private val formatter = DateTimeFormat.forPattern("yyyy-mm-dd'T'HH:mm:ssZ");
	private val formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ"); 
	private val displayedFormatter = new SimpleDateFormat("MMMM dd, yyyy - h:mm a");

	def stripUUID(filename : String) : String = return filename.substring(Math.max(0, filename.indexOf("--") + 2));

	implicit def dateToStr(date : Date) : String = return date.getTime().toString //return formatter.format(date);

	implicit def dateToDisplayedStr(date : Date) : String = return if (date != null) displayedFormatter.format(date) else "?";

	def getTimeAgo(date : Date) : String = {
		val interval = new Interval(date getTime, (new Date()) getTime)

		if( ((Weeks weeksIn interval) getWeeks) > 1) {
			return ( ((Weeks weeksIn interval) getWeeks) toString) + " weeks ago"
		}
		else if( ((Days daysIn interval) getDays) > 1) {
			return ( ((Days daysIn interval) getDays) toString) + " days ago"
		}
		else if (((Hours hoursIn interval) getHours) > 1) {
			return ( ((Hours hoursIn interval) getHours) toString) + " hours ago"
		}
		else if ( ((Minutes minutesIn interval) getMinutes) > 1) {
			return ( ((Minutes minutesIn interval) getMinutes) toString) + " minutes ago"
		}
		else {
			"just now"
		}
	}


	implicit def strToDate(str : String) : Date =  new Date(str.toLong) //return formatter.parse(str);

	implicit def mapToStr(map : Map[String, String]) : String = { 

		return "{" + map.map(item => {
			val key = item._1;
			val value = item._2.replace("'", "''")
			s"'$key' : '$value'"
		}).mkString(",") + "}"; 
	}

	implicit def eppnToUsername(eppn : String) : String = eppn.substring(0, eppn.indexOf("@") match {
		case -1 => eppn.length
		case length : Int => length
	});

	implicit def funToRunnable(fun: => Unit) = new Runnable() { def run() = fun }

	implicit val projectWrites = new Writes[Project] {
  		def writes(p: Project) = Json.obj(
	    "id" -> p.id,
	    "name" -> p.name,
	    "description" -> p.description,
	    "timeStarted" -> p.timeStarted,
	    "timeFinished" -> p.timeFinished,
	    "categories" -> p.categories,
	    "primaryContact" -> p.primaryContact,
	    "teamMembers" -> p.teamMembers,
	    "state" -> p.state,
	    "stateMessage" -> p.stateMessage
	  )
	}

	def sumList(xs: List[Int]): Int = {

	  def inner(xs: List[Int], accum: Int): Int = {
	    xs match {
	      case x :: tail => inner(tail, accum + x)
	      case Nil => accum
	    }
	  }
	  inner(xs, 0)
	}
}