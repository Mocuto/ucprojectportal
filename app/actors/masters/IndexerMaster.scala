package actors.masters

import actors.ActorMessageTypes._
import actors.workers.Indexer
import akka.actor._
import akka.routing._

import java.nio.file.{Paths, Files}

import model.Project

import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document._
import org.apache.lucene.index.{IndexWriter, IndexWriterConfig, Term}
import org.apache.lucene.util.Version
import org.apache.lucene.search._
import org.apache.lucene.store._

import play.api.Logger

object IndexerMaster {
	val indexesDir = Paths.get("indexes");
	val writeLock = Paths.get("indexes", "write.lock")


	Files.deleteIfExists(writeLock)

	if (Files.exists(Paths.get("indexes")) == false) {
		Files.createDirectory(indexesDir);
	}

	val analyzer = new StandardAnalyzer()
	val directory = new NIOFSDirectory(indexesDir)

	var writer = new IndexWriter(directory, new IndexWriterConfig(analyzer))
	var isWriterClosed = false;

	def openWriter() : Unit = {
		if(isWriterClosed == true) {
			writer = new IndexWriter(directory, new IndexWriterConfig(analyzer));
			isWriterClosed = false;
		}
	}

	def closeWriter() : Unit = {
		writer.close();
		Files.deleteIfExists(writeLock)
		isWriterClosed = true;
	}
}

class IndexerMaster extends Actor {

	//TODO: Replace "indexes" with strings found in configuration file.
	val writer = IndexerMaster.writer;

	val capActorSize = 5;

	val routees = Vector.fill(capActorSize) {
		val r = context.actorOf(Props[Indexer])
		context watch r
		ActorRefRoutee(r)
	}

	val router = {
		Router(RoundRobinRoutingLogic(), routees)
	}

	def receive = {


		case w : ActorWork[Project] => {println(w); router.route(w, context self); }

		case ws : Seq[ActorWork[Project]] => ws.foreach(receive(_))

		case ActorResult(p : Project, od : Option[Document]) => {
			Logger.info(s"Document received for project: ${p.id} - ${p.name}");
			od match {
				case Some(d : Document) => {
					d.getField("project-id") match {
						case x if x != null => write(p, d)
						case _ => //NOOP 
					}
				}
				case None => //NOOP
			}
			if(routees.length > capActorSize)
			{
				sender ! ActorTerminate
			}
		}

		case ActorTerminated(a) => {
			println("Terminating router");
			router.removeRoutee(a);
			val r = context.actorOf(Props[Indexer])
			context watch r
			router.addRoutee(r);
		}
	}


	def write(p : Project, d : Document) : Unit = {
		IndexerMaster.openWriter();
		val query = NumericRangeQuery.newIntRange(constants.Indexing.PROJECT_ID, p.id, p.id, true, true);
		writer.deleteDocuments(query)
		writer.addDocument(d)
		writer.commit();
	}

	override def postStop(): Unit = {
		IndexerMaster.closeWriter();
	}
}