package model

// TODO: add a list of agreegated event to be able to write it into cassandra
case class AggregateEvent(amount: BigInt,
                          window: String,
                          ipAddress: String,
                          rate: String,
                          categories: String,
                          events: List[Event],
                          isBot: Option[Boolean]) {
}
