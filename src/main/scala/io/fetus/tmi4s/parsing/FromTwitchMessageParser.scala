package io.fetus.tmi4s.parsing

import GeneralParser.name
import io.fetus.tmi4s.models.irc.Message
import io.fetus.tmi4s.models.irc.Message._

/**
  * Created by Sina on 2017-02-11.
  */
object FromTwitchMessageParser {
  import fastparse.all._
  import GeneralParser._
  import TagParser._

  val join: P[Join] = P(":" ~ name ~ "!" ~ fakeName ~ "@" ~ fakeName ~ ".tmi.twitch.tv JOIN " ~ channel).map(t => Join(t._1, t._2))
  val part: P[Part] = P(":" ~ name ~ "!" ~ fakeName ~ "@" ~ fakeName ~ ".tmi.twitch.tv PART " ~ channel).map(t => Part(t._1, t._2))
  val ping: P[Ping.type] = P("PING :tmi.twitch.tv").map(t => Ping)
  val pong: P[Pong.type] = P(":tmi.twitch.tv PONG tmi.twitch.tv :tmi.twitch.tv").map(t => Pong)
  val privmsg: P[PrivMsg] = P(tags.? ~ ":" ~ name ~ "!" ~ fakeName ~ "@" ~ fakeName ~ ".tmi.twitch.tv PRIVMSG " ~ channel ~ " :" ~ messageBody).map(t => PrivMsg(t._1, t._2, t._3, t._4))
  val caps: P[Capability] = P(":tmi.twitch.tv CAP * ACK :twitch.tv/" ~ name).map {
    case "membership" => MembershipCap
    case "commands" => CommandsCap
    case "tags" => TagsCap
  }
  val addOp: P[AddOp] = P(":jtv MODE " ~ channel ~ " +o " ~ name).map(t => AddOp(t._2, t._1))
  val removeOp: P[AddOp] = P(":jtv MODE " ~ channel ~ " -o " ~ name).map(t => AddOp(t._2, t._1))
  val notice: P[Notice] = P(tags.? ~ ":tmi.twitch.tv NOTICE " ~ channel ~ " :" ~ messageBody).map(Notice.tupled)
  val userState: P[UserState] = P(tags.? ~ ":tmi.twitch.tv USERSTATE " ~ channel).map(UserState.tupled)
  val globalUserState: P[GlobalUserState] = P(tags.? ~ ":tmi.twitch.tv GLOBALUSERSTATE").map(GlobalUserState.apply)
  val roomState: P[RoomState] = P(tags.? ~ ":tmi.twitch.tv ROOMSTATE " ~ channel).map(RoomState.tupled)
  val userNotice: P[UserNotice] = P(tags.? ~ ":tmi.twitch.tv USERNOTICE " ~ channel ~ (" :" ~ messageBody).?).map(UserNotice.tupled)
  val clearChat: P[ClearChat] = P(tags.? ~ ":tmi.twitch.tv CLEARCHAT " ~ channel ~ (" :" ~ name).?).map(ClearChat.tupled)
  val hostTarget: P[HostTarget] = P(":tmi.twitch.tv HOSTTARGET " ~ channel ~ " :" ~ ("-".!.map(_ => None) | name.map(Some.apply)) ~ " " ~ ("-".!.map(_ => 0) | integer)).map(HostTarget.tupled)
  val parse001: P[`001`] = P(":tmi.twitch.tv 001 " ~ name ~ " :Welcome, GLHF!").map(`001`.apply)
  val parse002: P[`002`] = P(":tmi.twitch.tv 002 " ~ name ~ " :Your host is tmi.twitch.tv").map(`002`.apply)
  val parse003: P[`003`] = P(":tmi.twitch.tv 003 " ~ name ~ " :This server is rather new").map(`003`.apply)
  val parse004: P[`004`] = P(":tmi.twitch.tv 004 " ~ name ~ " :-").map(`004`.apply)
  val parse375: P[`375`] = P(":tmi.twitch.tv 375 " ~ name ~ " :-").map(`375`.apply)
  val parse372: P[`372`] = P(":tmi.twitch.tv 372 " ~ name ~ " :You are in a maze of twisty passages, all alike.").map(`372`.apply)
  val parse376: P[`376`] = P(":tmi.twitch.tv 376 " ~ name ~ " :>").map(`376`.apply)
  val parse353: P[`353`] = P(":" ~ name ~ ".tmi.twitch.tv 353 " ~ fakeName ~ " = " ~ channel ~ " :" ~ name.rep(sep=" ")).map(`353`.tupled)
  val parse366: P[`366`] = P(":" ~ name ~ ".tmi.twitch.tv 366 " ~ fakeName ~ " " ~ channel ~ " :End of /NAMES list").map(`366`.tupled)
  val message: P[Message] = P(join |
    part |
    ping |
    pong |
    privmsg |
    caps |
    addOp |
    removeOp |
    notice |
    userState |
    globalUserState |
    roomState |
    userNotice |
    clearChat |
    hostTarget |
    parse001 |
    parse002 |
    parse003 |
    parse004 |
    parse375 |
    parse372 |
    parse376 |
    parse353 |
    parse366)
}
