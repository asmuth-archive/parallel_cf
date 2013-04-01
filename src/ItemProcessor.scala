// parallel_cf
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.parallel_cf

import scala.collection.JavaConverters._
import scala.collection.mutable.HashMap
import scala.collection.immutable.ListMap
import scala.actors.Actor
import scala.actors.Actor._
import java.util.concurrent.atomic._

class ItemInfo(_pid: Int){
  var total_count : AtomicInteger = new AtomicInteger(0)
  def pid : Int = _pid
}

class ItemProcessor(base: ParallelCF, iid: Int){

  var neighbors = ListMap[Int, Double]()
  val known_neighbors = HashMap[Int, Int]().withDefaultValue(0)

  var num_neighbors : Int = 0

  def add_neighbor(oid: Int) : Unit = this.synchronized {
    known_neighbors += ((oid, known_neighbors(oid)+1))
  }

  def prepare(me: ItemInfo) : Unit = {
    val max_neighbors = base.max_neighbors
    val neighbors_map = HashMap[Int, Double]()

    if (base.min_cc_count > 0 && me.total_count.intValue < base.min_cc_count)
      return

    known_neighbors.foreach((k: (Int, Int)) => {
      val sim = k._2.toDouble / (me.total_count.intValue + total_count(k._1) - k._2).toDouble
      neighbors_map += ((k._1, sim))
    })

    neighbors = (ListMap(neighbors_map.toList.sortBy{_._2}.reverse.take(max_neighbors):_*))
    num_neighbors = neighbors.size
  }

  def total_count(oid: Int) : Int =
    if (base.items contains oid) base.items(oid).total_count.intValue else 0


  def dump() : (Int, ListMap[Int, Double]) =
    ((iid, neighbors))

}
