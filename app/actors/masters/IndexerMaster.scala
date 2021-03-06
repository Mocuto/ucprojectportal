package actors.masters

import actors.ActorMessageTypes._
import actors.workers.Indexer
import akka.actor._
import akka.pattern.ask
import akka.routing._
import akka.util.Timeout

import constants.Indexing._

import java.nio.file.{Files, Paths}

import model.Project

import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document._
import org.apache.lucene.index.{IndexWriter, IndexWriterConfig, Term}
import org.apache.lucene.util.Version
import org.apache.lucene.search._
import org.apache.lucene.store._

import play.api.Logger

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class IndexWriterHandlerException(msg : String) extends RuntimeException

trait IndexWriterHandler {

	private var writer : Option[IndexWriter] = None;

	private val writeLock = Paths.get(constants.ServerSettings.IndexingDirectory, "write.lock")

	protected def constructWriter() : Unit = { writer = writer match {
			case None => {
				val indexesDir = Paths.get(constants.ServerSettings.IndexingDirectory);

				deleteWriteLock();

				if (Files.exists(Paths.get(constants.ServerSettings.IndexingDirectory)) == false) {
					Files.createDirectory(indexesDir)
				}

				val analyzer = new StandardAnalyzer()
				val directory = new NIOFSDirectory(indexesDir)


				val result = Some(new IndexWriter(directory, new IndexWriterConfig(analyzer)))
				
				Logger.info("Writer created");
				
				result
			}
			case x : Some[IndexWriter] => x
		}
		
	}

	protected def destroyWriter() : Unit = writer match {
		case Some(x : IndexWriter) => {
			x.close()
			deleteWriteLock();
			writer = None;
			Logger.info("Writer destroyed")
		}
		case _ => Logger.info("Writer already destroyed")
	}

	//TODO: Make this function for general use
	protected def write(p : Project, d : Document) : Unit = {
		constructWriter();

		writer match {
			case Some(x : IndexWriter) => {
				val query = NumericRangeQuery.newIntRange(constants.Indexing.ProjectId, p.id, p.id, true, true);

				x.deleteDocuments(query)
				x.addDocument(d);
				x.commit();
			}
		
			case None => new IndexWriterHandlerException("Attempted to write but could not create IndexWriter instance")
		}
	}

	private def deleteWriteLock() : Unit = Files.deleteIfExists(writeLock)
}

class IndexerMaster extends Actor with WorkRouter with IndexWriterHandler {

	def workerProps = Props[Indexer]

	private var workCounter = 0;

	override protected def onWorkRouted(w : ActorWork[_]) : Unit = workCounter += 1;

	override protected def onResultHandled() : Unit = workCounter -= 1;

	def receive = {

		case ActorWork(p : Project) => routeWork(ActorWork(p))

		case ws : Seq[_] => ws.foreach(receive(_))

		case ActorResult(p : Project, Some(d : Document)) if d.getField("project-id") != null => handleResult { write(p, d) }

		case ActorResult(p : Project, _) => handleResult { Logger.info(s"Tried to index project, had erroneous result: $p") }

		case ActorTerminated(a) => onActorTerminated(a)

		case IndexWriterWorkStatus => sender ! workCounter

		case KillIndexWriter => destroyWriter();
	}

	override def postStop() = destroyWriter();

}

//TODO: Generalize this
object IndexerMaster extends Master with actors.Scheduler {

	import play.api.libs.concurrent.Akka
	import play.api.Play.current

	def actorName = "indexer-master"

	def masterProps = Props[IndexerMaster]

	implicit val timeout = Timeout(5 seconds)

	def index(p : Project) = actor ! ActorWork(p);

	def start() : Unit = {
		Project.all.foreach (this index _)
		checkOn(5 minutes, actorName) {
			for (workCounter <- (actor ? IndexWriterWorkStatus) if workCounter == 0) actor ! KillIndexWriter
		}
	}
}
