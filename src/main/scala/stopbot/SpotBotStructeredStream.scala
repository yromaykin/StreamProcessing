package stopbot

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import com.datastax.spark.connector.cql.CassandraConnector
import com.fasterxml.jackson.databind.deser.std.StringDeserializer
import model.{AggregateEvent, Event, EventDb}
import org.apache.ignite.IgniteCache
import org.apache.ignite.spark.IgniteContext
import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming.OutputMode
import org.apache.spark.sql.types.{IntegerType, StringType, StructType, TimestampType}
import org.apache.spark.sql.{Dataset, SparkSession}

object SpotBotStructeredStream {

  val IGNITE_CONFIG = "ignite-client-config.xml"


  def main(args: Array[String]) {

    val spark = SparkSession.builder
      .master("local[5]")
      .appName("Fraud Detector")
      .config("spark.driver.memory", "2g")
      .getOrCreate()

    spark.sparkContext.setLogLevel("WARN")

    val igniteContext = new IgniteContext(spark.sparkContext, "ignite-client-config.xml")

    val connector = CassandraConnector(spark.sparkContext.getConf)
    val namespace = "spotbot"
    val table = "bots"

    connector.withSessionDo { session =>
      session.execute(s"drop KEYSPACE IF EXISTS $namespace")
      session.execute(s"CREATE KEYSPACE IF NOT EXISTS $namespace " +
        s"WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor': 1 }")
      session.execute(s"CREATE TABLE IF NOT EXISTS $namespace.$table " +
        s"(ip_address text, category_id text, unix_time text, event_type text, is_bot text, " +
        s" PRIMARY KEY (ip_address, category_id, unix_time, event_type))")
      session.close()
    }


    val df = spark
      //        .read
      .readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "127.0.0.1:9092")
      .option("subscribe", "ad-events")
      .option("key.deserializer", classOf[StringDeserializer].toString)
      .option("value.deserializer", classOf[StringDeserializer].toString)
      .option("failOnDataLoss", value = false)
      .option("startingOffsets", "earliest")
      .load()
    //      .text("Data/")


    import spark.implicits._

    val eventSchema = new StructType()
      .add("unix_time", TimestampType, nullable = false)
      .add("category_id", IntegerType, nullable = false)
      .add("ip", StringType, nullable = false)
      .add("type", StringType, nullable = false)


    val groomedJson = df
      .select(translate($"value".cast(StringType), "\\", "").as("value"))
      .select(regexp_extract($"value".cast(StringType), "(\\{.*\\})", 1).as("json"))

    val events = groomedJson
      .select(from_json($"json".cast(StringType), schema = eventSchema).as("struct"))
      .na.drop()
      .select($"struct.*")
      .toDF("unixTime", "categoryId", "ipAddress", "eventType")
      .as[Event]

    val groupedByIp = events
      .withWatermark("unixTime", "1 minute") //10 minutes
      .groupBy(window($"unixTime", "10 minutes", "5 minutes"), $"ipAddress")

    //Enormous event rate, e.g. more than 1000 request in 10 minutes*.
    val enormousAmountDF = groupedByIp
      .agg(
        count($"unixTime").as("amount"),
        (count(when($"eventType" === "click", $"unixTime"))
          / count(when($"eventType" === "view", $"unixTime"))).as("rate"),
        size(collect_set($"categoryId")).as("categories"),
        collect_list(struct($"*")).as("events")
      )
      .withColumn("window", $"window.end")
      .withColumn("isBot", $"amount" > 10 || $"rate" > 3 || $"categories" > 5)
      .as[AggregateEvent]
      .flatMap(aggregatedEvent => {
        aggregatedEvent.events.map(e => new EventDb(e, aggregatedEvent))
      })
      .as[EventDb]

    enormousAmountDF
      .writeStream
      .outputMode(OutputMode.Update()) //TODO append
      .foreachBatch((batchDf: Dataset[EventDb], batchId: Long) => {
      batchDf.persist()
      batchDf
        .filter(_.is_bot) //only bots are going to cache
        .foreach(eventDb => {
        val ignite = igniteContext.ignite()
        val cache: IgniteCache[String, LocalDateTime] = ignite.getOrCreateCache("bots")
        val result = cache.putIfAbsent(eventDb.ip_address,
          LocalDateTime.parse(eventDb.unix_time,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
        if (result)
          println(eventDb + " added to cache")
      })
      batchDf
        .write
        .format("org.apache.spark.sql.cassandra")
        .mode("append")
        .options(Map("table" -> "bots", "keyspace" -> "spotbot"))
        .save()
      batchDf.unpersist()
    })
      .start()
      .awaitTermination()
  }
}
