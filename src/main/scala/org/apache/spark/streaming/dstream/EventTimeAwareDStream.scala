package org.apache.spark.streaming.dstream

import model.EventSchema
import org.apache.spark.rdd.{EmptyRDD, RDD}
import org.apache.spark.streaming.{Duration, Time}

import scala.reflect.ClassTag

class EventTimeAwareDStream[T: ClassTag](parent: DStream[T],
                                         preservePartitioning: Boolean,
                                         window: Duration
                                        ) extends DStream[T](parent.ssc) {

  //don't wont to break any thing that why not using generatedRDDs map
  //  @transient
  //  val mineGeneratedRDDs = new HashMap[Time, EventSchema]()
  //  var minTime: Time = Time(0)
  var hugeRDD: RDD[EventSchema] = null

  var time: Time = null

  var groupByTime: Map[Time, Array[EventSchema]] = null

  override def dependencies: List[DStream[_]] = List(parent)

  override def slideDuration: Duration = window

  override def compute(validTime: Time): Option[RDD[T]] = {

    if (hugeRDD == null) {
      hugeRDD = parent.getOrCompute(validTime)
        .filter(rdd => !rdd.isEmpty())
        .map(rdd =>
          rdd.map(_.asInstanceOf[EventSchema])
            .sortBy(_.unix_time))
        .get
      groupByTime = hugeRDD.collect().groupBy(v => Time(v.unix_time * 1000))
      time = groupByTime.keys.min
    }

    if (groupByTime != null) {
      val events = groupByTime.keys
        .filter(keyTime => keyTime.greater(time) && keyTime.less(time + window))
        .map(time => groupByTime.get(time))
        .flatMap(evs => evs.orNull.toList).toSeq
      time += window
      Some(ssc.sc.makeRDD(events, 5)).asInstanceOf[Option[RDD[T]]]
    } else {
      Some(new EmptyRDD[T](this.ssc.sc))
    }
  }

}
