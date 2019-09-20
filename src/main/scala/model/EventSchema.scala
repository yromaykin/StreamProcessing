package model

case class EventSchema(unix_time: Long, category_id: Int, ip: String, `type`: String) {
  override def toString(): String = "(" + unix_time + ", " + category_id + ", " + ip + ")";
}
