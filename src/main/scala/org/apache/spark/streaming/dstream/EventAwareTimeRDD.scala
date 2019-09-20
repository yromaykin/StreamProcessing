package org.apache.spark.streaming.dstream

import java.time.{LocalDateTime, ZoneOffset}

import model.EventSchema
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.Time
import org.apache.spark.{Partition, TaskContext}

import scala.reflect.ClassTag

class EventAwareTimeRDD[T: ClassTag](var prev: RDD[T]) extends RDD[T](prev) {

  override def compute(split: Partition, context: TaskContext): Iterator[T] = {
    prev.compute(split, context)
  }

  override protected def getPartitions: Array[Partition] = firstParent[T].partitions

  def getUnixTime : Time = {
    implicit val localDateTimeOrdering: Ordering[LocalDateTime] = Ordering.by(_.toEpochSecond(ZoneOffset.UTC))

    if(prev.isEmpty()){
      return Time(0)
    }
    prev.first() match {
      case event : EventSchema => Time(prev.sortBy(_.asInstanceOf[EventSchema].unix_time)
        .first().asInstanceOf[EventSchema].unix_time * 1000)//todo should be last
      case _ => Time(0)//throw new UnsupportedOperationException("Stream has unsupported object for extracting event time")
    }
  }
}
