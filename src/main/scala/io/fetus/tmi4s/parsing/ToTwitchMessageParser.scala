package io.fetus.tmi4s.parsing

import io.fetus.tmi4s.models.irc.Message
import io.fetus.tmi4s.models.irc.Message._

/**
  * Created by Sina on 2017-02-11.
  */
object ToTwitchMessageParser {
  import fastparse.all._
  import GeneralParser._
  val joinmsg: P[Join] = P("JOIN " ~ channel).map(Join.apply)
  val partmsg: P[Part] = P("PART " ~ channel).map(Part.apply)
  val ping: P[Ping.type] = P("PING :tmi.twitch.tv").map(t => Ping)
  val pong: P[Pong.type] = P("PONG :tmi.twitch.tv").map(t => Pong)
  val privmsg: P[PrivMsg] = P("PRIVMSG " ~ channel ~ " :" ~ messageBody).map(t => PrivMsg(t._1, t._2))
  val nick: P[Nick] = P("NICK " ~ name).map(Nick.apply)
  val pass: P[Pass] = P("PASS oauth:" ~ AnyChar.rep.!).map(s => Message.Pass(s))
  val caps: P[Capability] = P("CAP REQ :twitch.tv/" ~ name).map {
    case "membership" => MembershipCap
    case "commands" => CommandsCap
    case "tags" => TagsCap
  }
  val message: P[SendableMessage] = P(
    joinmsg |
    partmsg |
    ping |
    pong |
    privmsg |
    caps |
    nick |
    pass
  )
}
