import scala.collection.immutable.ListMap

import com.paulasmuth.parallel_cf._

object ExampleRecommender {

  def main(args: Array[String]) : Unit = {

    val callback = (item_id: Int, neighbors: ListMap[Int, Double]) =>
        println("item_id", item_id, "neighbors", neighbors)

    // create a new cf processor
    val proc = new ParallelCF(callback)

    // read a csv file with headers, one line per order, columns: (user_id, item_id)
    (new CSVReader[Unit]((line: Map[Symbol, Int]) => {
      proc.import_rating(line('item_id), line('user_id))
    })).read("ratings.csv")

    // start the processing
    proc.process()

  }

}
