parallel_cf
===========

parallel_cf implements item-based collaborative filtering for binary ratings. It processes item<->item similarities from user->item interactions using the [Jaccard similarity coefficient](http://en.wikipedia.org/wiki/Jaccard_index).

Apart from being fully multi-threaded, the key to this implementation is that the item-item co-concurrency matrix is not pre-calculated. This would be a waste of memory since it grows `O(n^2)` and is very sparse (it contains mostly zeros). Instead, parallel_cf builds this matrix on-the-fly while processing the recommendations. Since this is done in multiple independent batches, only `1 / number_of_batches` of the item<->item tuples have to be unrolled in each batch, so the memory complexity decays logarithmically as the number of batches grows.

_This code processes a real-world data set containing 16.2 million interactions in around 8 minutes (6 batches, ~8GB ram, 24 cores)._


Example
-----

```scala
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
```


Collaborative Filtering
-----------------------

Use cases:

+ __"Users that bought this product also bought..."__ from `user_id--bought-->product_id` pairs
+ __"Users that viewed this video also viewed..."__ from `user_id--viewed-->video_id` pairs
+ __"Users that like this venue also like..."__ from `user_id--likes-->venue_id` pairs

Your input data (the so called interaction-sets) should look like this:

```
# FORMAT A: user bought products (select buyerid, productid from sales group_by buyerid)
[user23] product5 produt42 product17
[user42] product8 produt16 product5

# FORMAT B: user watched video (this can be transformed to the upper representation with a map/reduce)
user3 -> video3
user6 -> video19
user3 -> video6
user1 -> video42
```

The output data will look like this:

```
# similar products based on co-concurrent buys
product5 => product17 (0.78), product8 (0.43), product42 (0.31)
product17 => product5 (0.36), product8 (0.21), product42 (0.18)

# similar videos based on co-concurrent views
video19 => video3 (0.93), video6 (0.56), video42 (0.34)
video42 => video19 (0.32), video3 (0.21), video6 (0.08)
```






License
-------

Copyright (c) 2011 Paul Asmuth

Permission is hereby granted, free of charge, to the DaWanda GmbH
(Windscheidstr. 18, 10627 Berlin) and subsidiaries to use, copy and 
modify copies of the Software, subject to the following conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

