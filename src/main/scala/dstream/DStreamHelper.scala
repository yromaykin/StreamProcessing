package dstream

import org.apache.spark.sql.catalyst.plans.logical.Window
import org.apache.spark.streaming.Duration
import org.apache.spark.streaming.dstream.{EventAwareTimeRDD, EventTimeAwareDStream}

import scala.reflect.ClassTag

object DStreamHelper {

  implicit class DStream[T: ClassTag](stream: org.apache.spark.streaming.dstream.DStream[T]) {

    def convertToEventTimeAwareStream(window: Duration): EventTimeAwareDStream[T] = {
      new EventTimeAwareDStream[T](stream, false, window)
    }
  }

}
