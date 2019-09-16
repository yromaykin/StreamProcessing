package stopbot


import java.time.LocalDateTime

import com.datastax.spark.connector.cql.CassandraConnector
import model.parcer.DateFormatParser
import org.apache.ignite.IgniteCache
import org.apache.ignite.spark.IgniteContext
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.SparkConf
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.{Seconds, StreamingContext}
import spray.json._

object SpotBotDStream {

  val IGNITE_CONFIG = "ignite-client-config.xml"


  def main(args: Array[String]) {

    case class EventSchema(unix_time: LocalDateTime, category_id: Int, ip: String, `type`: String)


    object MyJsonProtocol extends DefaultJsonProtocol {

      import DateFormatParser.DateFormat

      implicit val eventFormat = jsonFormat4(EventSchema)
    }


    val conf = new SparkConf().setAppName("DStreamSpotBot").setMaster("local[5]")
    val streamingContext = new StreamingContext(conf, Seconds(1))

    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> "localhost:9092",
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> "use_a_separate_group_id_for_each_stream",
      "auto.offset.reset" -> "earliest",
      "enable.auto.commit" -> (false: java.lang.Boolean)
    )

    val igniteContext = new IgniteContext(streamingContext.sparkContext, "ignite-client-config.xml")

    val cassandra = CassandraConnector(streamingContext.sparkContext.getConf)
    val namespace = "spotbot"
    val table = "bots"
    val column_ip = "ip_address"
    val column_categoryId = "category_id"
    val column_unixTime = "unix_time"
    val column_eventType = "event_type"
    val column_is_bot = "is_bot"

    cassandra.withSessionDo { session =>
      session.execute(s"drop KEYSPACE IF EXISTS $namespace")
      session.execute(s"CREATE KEYSPACE IF NOT EXISTS $namespace " +
        s"WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor': 1 }")
      session.execute(s"CREATE TABLE IF NOT EXISTS $namespace.$table " +
        s"(ip_address text, category_id text, unix_time text, event_type text, is_bot text, " +
        s" PRIMARY KEY (ip_address, category_id, unix_time, event_type))")
      session.close()
    }

    val topics = Array("ad-events")
    val stream = KafkaUtils.createDirectStream[String, String](
      streamingContext,
      PreferConsistent,
      Subscribe[String, String](topics, kafkaParams)
    )

    import MyJsonProtocol._
    val regexp = """(\{.*\})""".r

    val events = stream.map(rdd => rdd.value())
      .map(json => json.replaceAll("\\\\", ""))
      .map(json => regexp.findFirstIn(json).orNull)
      .filter(json => json != null)
      .map(json => {
        json.parseJson.convertTo[EventSchema]
      })
      //todo not finished
      .window(Seconds(10), Seconds(5))
      .transform(rdd => rdd.groupBy(_.ip))
      .transform(rdd => rdd.map(aggregatedByIpEvent => (aggregatedByIpEvent._2.size > 10, aggregatedByIpEvent._2)))
      .foreachRDD(rdd => {
        rdd
          .map(events => {//not mapping but saving to database
            val session = cassandra.openSession()
            val insert = session.prepare(s"""insert into $namespace.$table
              ($column_ip, $column_categoryId, $column_unixTime, $column_eventType, $column_is_bot )
              values (?, ?, ?, ?, ?)""")
            val isBot = events._1
            events._2.toStream.foreach(event =>
              session.execute(
                insert.bind(event.ip, event.category_id.toString, event.unix_time.toString, event.`type`, isBot.toString)))
            events
          })
          .filter(_._1) //write only bots
          .map(_._2)
          .flatMap(_.toStream)
          .foreach(event => {
            val ignite = igniteContext.ignite()
            val cache: IgniteCache[String, LocalDateTime] = ignite.getOrCreateCache("bots")
            val result = cache.putIfAbsent(event.ip, event.unix_time)
            if (result)
              println(event + " added to cache")
          })

      })


    streamingContext.start()
    streamingContext.awaitTermination()

  }
}
