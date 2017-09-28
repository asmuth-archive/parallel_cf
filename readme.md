parallel_cf
===========

parallel_cf computes a list of <itemA, itemB, similarity> tuples from a list of
<user, item> interactions using the [Jaccard similarity coefficient](http://en.wikipedia.org/wiki/Jaccard_index).

#### Use cases:

+ __"Users that bought this product also bought..."__ from `user_id--bought-->product_id` pairs
+ __"Users that viewed this video also viewed..."__ from `user_id--viewed-->video_id` pairs
+ __"Users that like this venue also like..."__ from `user_id--likes-->venue_id` pairs


How it works
------------

Speaking more formally, parallel_cf implements item-based collaborative filtering
for implicit, binary ratings [1]. It takes as input data a list of so-called
"preference sets"; a preference set is a set of items that a user has implicitly
rated, for example by visiting, buying or clicking all of them. Here are a few
examples of what preference sets could look like:

```
# Example: Products clicked by user
[user23] product5 produt42 product17 ...
[user42] product8 produt16 product5 ...
...
```

From these preference sets we compute a two-dimensional array, the "co-concurrency matrix".
This array stores, for each unique combination of two items, the number of times
the respective combination of items has been observed in a preference set. We also
pre-compute another array storing the total number of times we've seen an item
as an optimization.

Using this index, we can compute the jaccard similarity between any two items
by simply doing a lookup into our pre-computed arrays (the set intersection and
union in jaccard can be transformed to arithmetic on counts retrieved from the
array).

The final step of the algorithm is to simply find the K nearest neighbors of each
item using the accelerated jaccard lookup.

Here is what the output looks like

```
# similar products based on co-concurrent buys
product5 => product17 (0.78), product8 (0.43), product42 (0.31)
product17 => product5 (0.36), product8 (0.21), product42 (0.18)

# similar videos based on co-concurrent views
video19 => video3 (0.93), video6 (0.56), video42 (0.34)
video42 => video19 (0.32), video3 (0.21), video6 (0.08)
```

#### Partitioning & Parallelization

The key to this implementation is that the item-item co-concurrency matrix is not
actually fully materialized in memory. Storing the full co-concurrency matrix
would require `O(n^2)` memory as the number of unique items in the dataset grows.

To still process large-ish datasets, we use a hash function to map the input item
ids into a number of buckets. We then scan the input data multiple times, once for
each bucket. On each pass, we build a sparse representation of the subset of
columns of the co-concurrency matrix that correspond to the item ids matching
the current bucket.

Another benefit of partitioning the co-concurrency matrix is that it allows us
to compute each of the individual partitions in parallel without requiring
inter-thread communication, hence the name parallel_cf.

_This code processes a real-world data set containing 16.2 million interactions
in around 8 minutes (6 batches, ~8GB ram, 24 cores)._


Example
-----

```scala
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
```

You can run this example with:

    sbt run


Sources / References
--------------------

[1] Miranda C. and Alipio J. (2008). Incremental collaborative ﬁltering for binary ratings (LIAAD - INESC Porto, University of Porto)

[2] George Karypis (2000) Evaluation of Item-Based Top-N Recommendation Algorithms (University of Minnesota, Department of Computer Science / Army HPC Research Center)

[3] Shiwei Z., Junjie W. Hui X. and Guoping X. (2011) Scaling up top-K cosine similarity search (Data & Knowledge Engineering 70)


License
-------

    Copyright (c) 2011 Paul Asmuth

    Permission is hereby granted, free of charge, to any person obtaining a copy of
    this software and associated documentation files (the "Software"), to deal in the
    Software without restriction, including without limitation the rights to use,
    copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the
    Software, and to permit persons to whom the Software is furnished to do so,
    subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
    INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
    PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
    HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
    OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
    SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

