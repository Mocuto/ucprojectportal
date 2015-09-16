package utils

import actors.masters.IndexerMaster

import java.nio.file.{Paths, Files}

import model.Project

import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index.{DirectoryReader, IndexReader}
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser
import org.apache.lucene.search._
import org.apache.lucene.store._

object ProjectSearcher {

	val indexesDir = Paths.get(constants.ServerSettings.IndexingDirectory);

	val directory = new NIOFSDirectory(indexesDir)

	val reader = DirectoryReader.open(directory)

	val searcher = new IndexSearcher(reader);

	def search(queryString : String) : Seq[Project] = {

		if(model.Project.all.length == 0) {
			return List[Project]();
		}

		val fields = List(
			"project-id", 
			"project-name", 
			"project-description", 
			"project-categories", 
			"project-primary-contact", 
			"project-team-members", 
			"project-state",
			"project-state-message")

		val flags = List.fill(fields.length) { BooleanClause.Occur.SHOULD }
		val query = MultiFieldQueryParser.parse(queryString, fields.toArray, flags.toArray, new StandardAnalyzer());
		val numberOfHits = 50;
		(searcher.search(query, numberOfHits) scoreDocs) sortBy(-_.score) map(scoreDoc => {

			val doc = searcher.doc(scoreDoc.doc);
			val id = doc.get("project-id").toInt;

			Project.get(id)
		})
	}
}