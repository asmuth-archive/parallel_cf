// parallel_cf
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.parallel_cf

import scala.collection.mutable.HashMap 
import scala.collection.mutable.SynchronizedMap 
import scala.actors.Actor
import scala.actors.Actor._
import scala.actors.Futures._
import java.util.concurrent._
import scala.collection.mutable.HashSet
import scala.collection.mutable.SynchronizedSet

import java.util.concurrent.atomic._

class ParallelCF(callback: (Int, scala.collection.immutable.ListMap[Int,Double]) => Unit){

  var num_batches   = 6
  var num_threads   = 64
  var pset_max_size = 50
  var max_neighbors = 50

  var debug = true

  var item_processors  = HashMap[Int, ItemProcessor]()
  var preference_sets    = new HashMap[Int, PreferenceSet] with SynchronizedMap[Int, PreferenceSet]
  var items              = new HashMap[Int, ItemInfo]

  def process() = {
    debugln("-- preparing indexes")
    prepare_psets()

    (0 to num_batches - 1)
      .foreach(n => process_cc_matrix(num_batches, n))

    debugln("-- processing finished")
  }

  def import_rating(item_id: Int, user_id: Int) = {
    if (items contains item_id unary_!)
      items += ((item_id, new ItemInfo(item_id)))

    items(item_id).total_count.incrementAndGet

    if (preference_sets contains user_id unary_!){
      val pset =  new PreferenceSet(this, user_id, pset_max_size)
      preference_sets += ((user_id, pset))
    }

    preference_sets(user_id).append(item_id)
  }

  private def prepare_psets() = {
    debugln(">> preparing: preference sets")

    val runner = Executors.newFixedThreadPool(num_threads)

    preference_sets.foreach((k: (Int, PreferenceSet)) => {
      runner.execute(new Runnable {
        def run() = { k._2.prepare() }
      })
    })

    runner.shutdown
    runner.awaitTermination(Math.MAX_LONG, TimeUnit.SECONDS)
  }

  private def process_cc_matrix(total: Integer, current: Integer) = {
    debugln("-- ccmatrix pass " + (current+1).toString + " of " + total.toString)

    val item_ids = items
      .map(prod => prod._1)
      .filter(pid => (pid % total) == current)


    debugln(">> preparing " + item_ids.size.toString + " recommendations")

    item_ids.foreach { pid => 
      item_processors += ((pid, new ItemProcessor(this, pid)))
    }


    debugln(">> collecting all neighbors")

    val runner = Executors.newFixedThreadPool(num_threads)

    preference_sets.foreach(pset => {
      runner.execute(new Runnable {
        def run() = {
          pset._2.dump.foreach{ k => 
            if ((k._1 % total) == current)
              item_processors(k._1).add_neighbor(k._2)
            if ((k._2 % total) == current)
              item_processors(k._2).add_neighbor(k._1)
          }
        }
      })
    })

    runner.shutdown
    runner.awaitTermination(Math.MAX_LONG, TimeUnit.SECONDS)

    process_precs() 
  }

  private def process_precs() = {
    debugln(">> processing item distancees")

    val runner = Executors.newFixedThreadPool(num_threads)

    item_processors.foreach(prec => {
      runner.execute(new Runnable {
        def run() = {
          prec._2.prepare(items(prec._1))

          if(prec._2.num_neighbors != 0)
            callback tupled prec._2.dump
        }
      })
    })

    runner.shutdown
    runner.awaitTermination(Math.MAX_LONG, TimeUnit.SECONDS)

    item_processors.clear()
  }

  private def debugln(str: String) =
    if (debug) println(str)

}
