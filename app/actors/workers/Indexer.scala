import utils.indexers.ProjectIndexer

case class Work[A](items : Seq[A])
case class Result[A, B](value : Seq[B])

class Indexer extends Actor with ProjectIndexer {
	def receieve = {
		case Work(items) => {
			
		}
	}
}