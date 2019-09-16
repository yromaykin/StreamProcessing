package model.parcer
import java.text._
import java.time.{Instant, LocalDateTime, ZoneId, ZoneOffset}
import java.util._

import scala.util.Try
import spray.json._

object DateFormatParser {
  implicit object DateFormat extends JsonFormat[LocalDateTime] {
    def write(date: LocalDateTime) = JsString(dateToIsoString(date))
    def read(json: JsValue) = json match {
      case JsNumber(rawDate) =>
        parseIsoDateString(rawDate)
          .fold(deserializationError(s"Expected ISO Date format, got $rawDate"))(identity)
      case error => deserializationError(s"Expected JsString, got $error")
    }
  }

  private val localIsoDateFormatter = new ThreadLocal[SimpleDateFormat] {
    override def initialValue() = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
  }

  private def dateToIsoString(date: LocalDateTime) =
    localIsoDateFormatter.get().format(date)

  private def parseIsoDateString(date: BigDecimal): Option[LocalDateTime] =
    Try{ LocalDateTime.ofEpochSecond(date.toLong, 0, ZoneOffset.UTC) }.toOption
}
