package utils

import java.text.SimpleDateFormat

import java.util.Date

import scala.collection.JavaConversions._

object Conversions {
  //private val formatter = DateTimeFormat.forPattern("yyyy-mm-dd'T'HH:mm:ssZ");
	private val formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ"); 
	private val displayedFormatter = new SimpleDateFormat("MMMM dd, yyyy - h:mm a");

	def stripUUID(filename : String) : String = return filename.substring(Math.max(0, filename.indexOf("--") + 2));

	implicit def dateToStr(date : Date) : String = return formatter.format(date);

	implicit def dateToDisplayedStr(date : Date) : String = return if (date != null) displayedFormatter.format(date) else "?";

	implicit def strToDate(str : String) : Date = return formatter.parse(str);

	implicit def mapToStr(map : Map[String, String]) : String = { 
		val key = item._1;
		val value = item._2.replace("'", "''");
		return "{" + map.map(item => s"'$key' : '$value'").mkString(",") + "}"; 
	}
}