import scala.actors.Actor
import scala.collection.immutable.ListMap

import com.paulasmuth.parallel_cf._

object ExampleRecommender extends Actor {

  def main(args: Array[String]) : Unit = {

    // start self, results are sent as messages
    this.start

    // create a new cf processor
    val proc = new ParallelCF(this)

    // read a csv file with headers, one line per order, columns: (user_id, item_id)
    (new CSVReader[Unit]((line: Map[Symbol, Int]) => {
      proc.import_rating(line('item_id), line('user_id))
    })).read("ratings.csv")

    // start the processing
    proc.process()

  }

  def act = {
    Actor.loop { react {

      // print each result to stdout
      case (item_id: Int, neighbors: ListMap[Int, Double]) =>
        println("item_id", item_id, "neighbors", neighbors)

    }}
  }

}
