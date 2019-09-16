package model

case class Event(unixTime: String,
                 categoryId: String,
                 ipAddress: String,
                 eventType: String) {
}
