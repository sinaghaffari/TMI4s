package io.fetus.tmi4s.models.irc

import io.fetus.tmi4s.models.irc.Message.SendableMessage

trait MessageContainer[+T <: Message] {
  val msg: T
}

object MessageContainer {
  case class FromTwitch[+T <: Message](msg: T) extends MessageContainer[T]
  case class ToTwitch[+T <: SendableMessage](msg: T) extends MessageContainer[T]
}
