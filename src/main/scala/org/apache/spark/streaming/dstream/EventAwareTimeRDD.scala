package org.apache.spark.streaming.dstream

import java.time.{LocalDateTime, ZoneOffset}

import model.EventSchema
import org.apache.spark.rdd.{EmptyRDD, RDD}
import org.apache.spark.streaming.Time
import org.apache.spark.{Partition, TaskContext}


class EventAwareTimeRDD(val time: Time, val it: Iterable[EventSchema], val prev: RDD[EventSchema])
  extends RDD[EventSchema](prev) {

  override def compute(split: Partition, context: TaskContext): Iterator[EventSchema] = {
    it.iterator
  }

  override protected def getPartitions: Array[Partition] = firstParent[EventSchema].partitions
}
