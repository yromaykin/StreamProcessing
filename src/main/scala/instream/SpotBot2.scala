package instream

import instream.SpotBot.Event
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.catalyst.expressions.Minute
import org.apache.spark.sql.functions.{from_json, regexp_extract, translate}
import org.apache.spark.sql.types._
import org.apache.spark.streaming.{Minutes, Seconds, StreamingContext}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming.Trigger

/**
  * Created by yromaykin on 30/10/2018.
  */
object SpotBot2 {

  case class Event(unixTime: Long, categoryId: Int, ipAddress: String, eventType: String) {
    def isClick: Boolean = eventType == "click"

    def isView: Boolean = eventType == "view"
  }

  def main(args: Array[String]): Unit = {

    val sparkSession = SparkSession.builder
      .master("local")
      .appName("Fraud Detector")
      .config("spark.driver.memory", "2g")
      .getOrCreate()


    val df = sparkSession
      .readStream
      .format("kafka")
      .option("startingOffsets", "earliest")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("subscribe", "ad-events")
      .option("key.deserializer", classOf[StringDeserializer].toString)
      .option("value.deserializer", classOf[StringDeserializer].toString)
      .option("failOnDataLoss", value = false)
      .load()

    val eventSchema = new StructType()
      .add("unix_time", LongType, nullable = false)
      .add("category_id", IntegerType, nullable = false)
      .add("ip", StringType, nullable = false)
      .add("type", StringType, nullable = false)

    import sparkSession.implicits._

//        val windowedCounts = df.groupBy(
//          window($"unixTime", "10 minutes", "5 minutes"),
//          $"word"
//        ).count().show()

    //groomed
    val groomed = df
      .select(translate($"value".cast(StringType), "\\", "").as("value"))
      .select(regexp_extract($"value".cast(StringType), "(\\{.*\\})", 1).as("json"))

    //events
    val events = groomed
      .select(from_json($"json".cast(StringType), schema = eventSchema).as("struct"))
      .select("struct.*")
      .map(r => Event(r.getLong(0), r.getInt(1), r.getString(2), r.getString(3)))

    val grouped = events.groupBy($"ipAddress", window($"unixTime".cast("timestamp"), "20 minutes", "1 minutes")).count()
//    val grouped = events.groupBy($"ipAddress").count()


    val query = grouped.writeStream
      .format("console")
      .outputMode("complete")
//      .trigger(Trigger.ProcessingTime("1 minutes"))
      .start()


    query.awaitTermination()

  }

}
