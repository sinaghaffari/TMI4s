package io.fetus.tmi4s.models.irc

import io.fetus.tmi4s.models.irc.Message.{Capability, Nick, Pass}

case class Authenticate(nick: Nick, pass: Pass, caps: Set[Capability])
