package com.paulasmuth.parallel_cf

import scala.actors.Actor
import scala.actors.Actor._

class PreferenceSet(base: ParallelCF, _set_id: Int, max_size: Int){

  var lst : List[Int] = List[Int]()
  var out = Set[(Int, Int)]()

  var sellers = Set[Int]()

  def set_id() : Int = _set_id
  def dump() : List[(Int, Int)] = out.toList

  def prepare() = {
    prepare_dump() 
    lst = null
  }

  def truncate() = ()

  def append(item_id: Int) = this.synchronized {
    append_item(item_id)
  }

  private def prepare_dump() =
    (lst foreach (
      (iid: Int) => (lst - iid) foreach ((oid: Int) =>
        if (iid > oid)
          out += ((iid, oid))
        else
          out += ((oid, iid))
    )))

  private def append_item(item_id: Int) =
    lst = ((item_id :: lst) take(max_size))

}
