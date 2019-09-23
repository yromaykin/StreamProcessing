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

  var time: Time = null

  var groupByTime: Map[Time, Array[EventSchema]] = Map[Time, Array[EventSchema]]()

  override def dependencies: List[DStream[_]] = List(parent)

  override def slideDuration: Duration = window

  override def compute(validTime: Time): Option[RDD[T]] = {


    val hugeRDD = parent.getOrCompute(validTime)
      .filter(rdd => !rdd.isEmpty())
      .map(rdd =>
        rdd.map(_.asInstanceOf[EventSchema])
          .sortBy(_.unix_time))
      .orNull
//      .map(rdd => groupByTime += rdd.collect().groupBy(v => Time(v.unix_time * 1000)))
    if(hugeRDD != null) {
      groupByTime = hugeRDD.collect().groupBy(v => Time(v.unix_time * 1000))
    }
    time = groupByTime.keys.min

    println("groupBySize: " + groupByTime.size)
    if (groupByTime != null) {
      val eventPartitions = groupByTime.partition(entry => entry._1.greaterEq(time) && entry._1.lessEq(time + window))
      groupByTime = eventPartitions._2
      time += window
      val events = eventPartitions._1.values.flatMap(_.toStream).toSeq
      println("Time:" + time)
      Some(ssc.sc.makeRDD(events, 5)).asInstanceOf[Option[RDD[T]]]
    } else {
      Some(new EmptyRDD[T](this.ssc.sc))
    }
  }

}
