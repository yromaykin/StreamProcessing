package org.apache.spark.streaming.dstream

import model.EventSchema
import org.apache.spark.rdd.{EmptyRDD, RDD}
import org.apache.spark.streaming.{Duration, Seconds, Time}

import scala.collection.mutable.ListBuffer
import scala.reflect.ClassTag

case class SerInt(window: Int)

class EventTimeAwareDStream[T: ClassTag](parent: DStream[T],
                                         preservePartitioning: Boolean,
                                         val window: Int
                                        ) extends DStream[T](parent.ssc) {

  val rdds: ListBuffer[RDD[EventSchema]] = ListBuffer()

  override def dependencies: List[DStream[_]] = List(parent)

  override def slideDuration: Duration = Seconds(10)

  override def compute(validTime: Time): Option[RDD[T]] = {

    val v = SerInt(window)
    val rdd = parent.getOrCompute(validTime)
      .map(rdd => rdd.map(_.asInstanceOf[EventSchema]))

    if (rdd.isDefined) {
      rdds.append(rdd.get)
    }

    if (rdds.isEmpty) {
      Some(new EmptyRDD[T](this.ssc.sc))
    }

    val rdd1 = rdd
      .map(rdd => rdd.union(rdds.head))
      .map(rdd => rdd.filter(event => Time(event.unix_time * 1000).greaterEq(validTime) && Time(event.unix_time * 1000).lessEq(validTime + Seconds(v.window))))
    rdds.remove(0)
    rdd1.asInstanceOf[Option[RDD[T]]]
  }

}
