package model

case class WindowEventTrigger[T] private(eventsInWindow: Seq[T],
                                         triggerNow: Boolean,
                                         private val lastTriggeredEvent: Option[T],
                                         private val triggerLevel: Int) {
  def this(item: T, triggerLevel: Int) = this(Seq(item), false, None, triggerLevel)

  def add(incoming: WindowEventTrigger[T]): WindowEventTrigger[T] = {
    val combined = eventsInWindow ++ incoming.eventsInWindow
    val shouldTrigger = lastTriggeredEvent.isEmpty && combined.length >= triggerLevel
    val triggeredEvent = if (shouldTrigger) combined.seq.drop(triggerLevel - 1).headOption else lastTriggeredEvent
    new WindowEventTrigger(combined, shouldTrigger, triggeredEvent, triggerLevel)
  }

  def remove(outgoing: WindowEventTrigger[T]): WindowEventTrigger[T] = {
    val reduced = eventsInWindow.filterNot(y => outgoing.eventsInWindow.contains(y))
    val triggeredEvent = if (lastTriggeredEvent.isDefined && outgoing.eventsInWindow.contains(lastTriggeredEvent.get)) None else lastTriggeredEvent
    new WindowEventTrigger(reduced, false, triggeredEvent, triggerLevel)
  }
}
