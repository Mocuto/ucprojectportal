package model

import com.datastax.driver.core.Row

import java.io.File
import java.nio.file.{Paths, Files}
import java.util.Date

import play.api.mvc.MultipartFormData._
import play.api.libs.Files._

import scala.collection.JavaConversions._

import utils._
import utils.nosql.CassieCommunicator

object ProjectFile {
	def undefined : ProjectFile = { 
		return ProjectFile(
			isDefined = false, 
			filename = "",
			originalName = "", 
			author = "", 
			projectId = -1
		)
	 }

 	def get(project : Project) : Seq[ProjectFile] = return CassieCommunicator.getFilesForProject(project)

 	def get(projectId : Int, timeSubmitted : Date, filename : String) : ProjectFile = return CassieCommunicator.getFile(projectId, timeSubmitted, filename)

	def saveFile (temporaryFile : (String, TemporaryFile), author : String, projectId : Int, timeSubmitted : Date) : ProjectFile = {

		val uuid = java.util.UUID.randomUUID.toString;

		val originalName = temporaryFile._1
		val filename = uuid + "--" + temporaryFile._1

		if(Files.exists(Paths.get(constants.Directories.Root, constants.Directories.Uploads)) == false) {
			val uploadsDir = new File(Paths.get(constants.Directories.Root, constants.Directories.Uploads).toString);
			uploadsDir.mkdir();
		} 

		//val file = new File(s"uploads/$filename");
		val file = new File(Paths.get(constants.Directories.Root, constants.Directories.Uploads, filename).toString)

	    temporaryFile._2.moveTo(file, true);

	    return createProjectFile(filename, originalName, author, projectId, timeSubmitted);
	}

	def createProjectFile(filename : String, originalName : String, author : String, projectId : Int, timeSubmitted : Date) : ProjectFile = {
		val projectFile = ProjectFile(filename, originalName, author, projectId, timeSubmitted);
		return CassieCommunicator.addFile(projectFile);
	}

	def delete(file : ProjectFile) {
		CassieCommunicator.removeFile(file)
	}

	def delete(projectId : Int, timeSubmitted : Date, filename : String) {
		ProjectFile.delete(ProjectFile.get(projectId, timeSubmitted, filename))
	}

	implicit def fromRow(row : Row) : ProjectFile = {
		row match {
	  		case null => return ProjectFile.undefined
	  		case row : Row => {
	  			return ProjectFile(
	  				row.getString("filename"),
	  				row.getString("original_filename"),
	  				row.getString("author"),
	  				row.getInt("project_id"), 
	  				row.getDate("time_submitted")
				);
	  		}
  		}

	}
}

case class ProjectFile(filename : String, originalName : String,  author : String, projectId : Int, timeSubmitted : Date = new Date(), isDefined : Boolean = true)