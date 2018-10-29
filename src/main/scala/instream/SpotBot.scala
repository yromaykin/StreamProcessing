package instream

import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.sql.streaming.Trigger
import org.apache.spark.sql.types._
import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.functions.regexp_extract

/**
  * Created by yromaykin on 29/10/2018.
  */
object SpotBot {


  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder
      .master("local")
      .appName("Fraud Detector")
      .config("spark.driver.memory", "2g")
      .getOrCreate()


    val df = spark
      //      .readStream
      .read
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("subscribe", "ad-events")
      .option("key.deserializer", classOf[StringDeserializer].toString)
      .option("value.deserializer", classOf[StringDeserializer].toString)
      .option("failOnDataLoss", value = false)
      .load()

    df.printSchema()


    import spark.implicits._

    val eventSchema =  new StructType()
      .add("unix_time", LongType, nullable = false)
      .add("category_id", IntegerType, nullable = false)
      .add("ip", StringType, nullable = false)
      .add("type", StringType, nullable = false)

    val groomed = df.limit(10)
      .select(translate($"value".cast(StringType), "\\", "").as("value"))
      .select(regexp_extract($"value".cast(StringType), "(\\{.*\\})", 1).as("json"))

    val events = groomed
      .select(from_json($"json".cast(StringType), schema = eventSchema).as("struct"))
    //      .select("struct.*")
    //      .map(r => Event(r.getLong(0), r.getInt(1), r.getString(2), r.getString(3)))

    events.show(false)

  }

}
