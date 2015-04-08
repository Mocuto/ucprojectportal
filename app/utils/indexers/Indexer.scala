package utils.indexers

trait Indexer[A] {
	def index (item : A) : Document
}