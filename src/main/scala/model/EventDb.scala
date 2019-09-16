package model

case class EventDb (unix_time: String,
                    category_id: String,
                    ip_address: String,
                    event_type: String,
                    is_bot: Boolean) {

  def this(event: Event, aggregateEvent: AggregateEvent){
    this(event.unixTime, event.categoryId, event.ipAddress, event.eventType, aggregateEvent.isBot.getOrElse(false))
  }
}