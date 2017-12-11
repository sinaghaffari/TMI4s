package io.fetus.tmi4s.models.irc

import akka.util.ByteString
import io.fetus.tmi4s.models.irc.tags.Tag
import play.api.libs.json._

object Message {
  val pingFormat: Format[Ping.type] = new Format[Ping.type] {
    override def reads(json: JsValue): JsResult[Ping.type] = (json \ "type").validate[String].filterNot(_ != "ping").map(_ => Ping)
    override def writes(o: Ping.type): JsValue = Json.obj("type" -> "pong")
  }
  val pongFormat: Format[Pong.type] = new Format[Pong.type] {
    override def reads(json: JsValue): JsResult[Pong.type] = (json \ "type").validate[String].filterNot(_ != "pong").map(_ => Pong)
    override def writes(o: Pong.type): JsValue = Json.obj("type" -> "pong")
  }
  val joinFormat: Format[Join] = new Format[Join]  {
    override def reads(json: JsValue): JsResult[Join] = for {
      _ <- (json \ "type").validate[String].filterNot(_ != "join")
      name <- (json \ "name").validate[String]
      channel <- (json \ "channel").validate[String]
    } yield Join(name, channel)
    override def writes(o: Join): JsValue = o match {
      case o: Join => Json.obj(
        "type" -> "join",
        "name" -> o.name,
        "channel" -> o.channel
      )
      case _ => JsNull
    }
  }
  val partFormat: Format[Part] = new Format[Part]  {
    override def reads(json: JsValue): JsResult[Part] = for {
      _ <- (json \ "type").validate[String].filterNot(_ != "part")
      name <- (json \ "name").validate[String]
      channel <- (json \ "channel").validate[String]
    } yield Part(name, channel)
    override def writes(o: Part): JsValue = Json.obj(
      "type" -> "part",
      "name" -> o.name,
      "channel" -> o.channel
    )
  }
  val privMsgFormat: Format[PrivMsg] = new Format[PrivMsg] {
    override def reads(json: JsValue): JsResult[PrivMsg] = for {
      _ <- (json \ "type").validate[String].filterNot(_ != "privmsg")
      tags <- (json \ "tags").validateOpt[JsObject].map(_.map(_.fields.flatMap(x => Tag.fromJson(x._1, x._2)).toSet))
      name <- (json \ "name").validate[String].orElse(JsSuccess(""))
      channel <- (json \ "channel").validate[String]
      message <- (json \ "message").validate[String]
    } yield PrivMsg(tags, name, channel, message)

    override def writes(o: PrivMsg): JsValue = Json.obj(
      "type" -> "privmsg",
      "tags" -> o.tags.map(_.map(_.json).reduce(_ ++ _)),
      "name" -> o.name,
      "channel" -> o.channel,
      "message" -> o.message
    )
  }
  val nickFormat: Format[Nick] = new Format[Nick] {
    override def reads(json: JsValue): JsResult[Nick] = for {
      _ <- (json \ "type").validate[String].filterNot(_ != "nick")
      name <- (json \ "name").validate[String]
    } yield Nick(name)

    override def writes(o: Nick): JsValue = Json.obj(
      "type" -> "nick",
      "name" -> o.name
    )
  }
  val passFormat: Format[Pass] = new Format[Pass] {
    override def reads(json: JsValue): JsResult[Pass] = for {
      _ <- (json \ "type").validate[String].filterNot(_ != "nick")
      pass <- (json \ "name").validate[String]
    } yield Pass(pass)

    override def writes(o: Pass): JsValue = Json.obj(
      "type" -> "pass",
      "name" -> o.pass
    )
  }
  val membershipCapFormat: Format[MembershipCap.type] = new Format[MembershipCap.type] {
    override def reads(json: JsValue): JsResult[MembershipCap.type] = for {
      _ <- (json \ "type").validate[String].filterNot(_ != "membershipcapability")
    } yield MembershipCap

    override def writes(o: MembershipCap.type): JsObject = Json.obj(
      "type" -> "membershipcapability"
    )
  }
  val commandsCapFormat: Format[CommandsCap.type] = new Format[CommandsCap.type] {
    override def reads(json: JsValue): JsResult[CommandsCap.type] = for {
      _ <- (json \ "type").validate[String].filterNot(_ != "commandscapability")
    } yield CommandsCap

    override def writes(o: CommandsCap.type): JsObject = Json.obj(
      "type" -> "commandscapability"
    )
  }
  val tagsCapFormat: Format[TagsCap.type] = new Format[TagsCap.type] {
    override def reads(json: JsValue): JsResult[TagsCap.type] = for {
      _ <- (json \ "type").validate[String].filterNot(_ != "tagscapability")
    } yield TagsCap

    override def writes(o: TagsCap.type): JsObject = Json.obj(
      "type" -> "tagscapability"
    )
  }
  val addOpFormats: Format[AddOp] = new Format[AddOp] {
    override def writes(o: AddOp): JsValue = Json.obj(
      "type" -> "addop",
      "name" -> o.name,
      "channel" -> o.channel
    )
    override def reads(json: JsValue): JsResult[AddOp] = for {
      _ <- (json \ "type").validate[String].filterNot(_ != "addop")
      name <- (json \ "name").validate[String]
      channel <- (json \ "channel").validate[String]
    } yield AddOp(name, channel)
  }
  val removeOpFormats: Format[RemoveOp] = new Format[RemoveOp] {
    override def writes(o: RemoveOp): JsValue = Json.obj(
      "type" -> "removeop",
      "name" -> o.name,
      "channel" -> o.channel
    )
    override def reads(json: JsValue): JsResult[RemoveOp] = for {
      _ <- (json \ "type").validate[String].filterNot(_ != "removeop")
      name <- (json \ "name").validate[String]
      channel <- (json \ "channel").validate[String]
    } yield RemoveOp(name, channel)
  }
  val noticeFormat: Format[Notice] = new Format[Notice] {
    override def reads(json: JsValue): JsResult[Notice] = for {
      _ <- (json \ "type").validate[String].filterNot(_ != "notice")
      tags <- (json \ "tags").validateOpt[JsObject].map(_.map(_.fields.flatMap(x => Tag.fromJson(x._1, x._2)).toSet))
      channel <- (json \ "channel").validate[String]
      message <- (json \ "message").validate[String]
    } yield Notice(tags, channel, message)

    override def writes(o: Notice): JsValue = Json.obj(
      "type" -> "notice",
      "tags" -> o.tags.map(_.map(_.json).reduce(_ ++ _)),
      "channel" -> o.channel,
      "message" -> o.message
    )
  }
  val userStateFormat: Format[UserState] = new Format[UserState] {
    override def reads(json: JsValue): JsResult[UserState] = for {
      _ <- (json \ "type").validate[String].filterNot(_ != "userstate")
      tags <- (json \ "tags").validateOpt[JsObject].map(_.map(t => t.fields.flatMap(x => Tag.fromJson(x._1, x._2)).toSet))
      channel <- (json \ "channel").validate[String]
    } yield UserState(tags, channel)

    override def writes(o: UserState): JsValue = Json.obj(
      "type" -> "userstate",
      "tags" -> o.tags.map(_.map(_.json).reduce(_ ++ _)),
      "channel" -> o.channel
    )
  }
  val roomStateFormat: Format[RoomState] = new Format[RoomState] {
    override def reads(json: JsValue): JsResult[RoomState] = for {
      _ <- (json \ "type").validate[String].filterNot(_ != "roomstate")
      tags <- (json \ "tags").validateOpt[JsObject].map(_.map(_.fields.flatMap(x => Tag.fromJson(x._1, x._2)).toSet))
      channel <- (json \ "channel").validate[String]
    } yield RoomState(tags, channel)

    override def writes(o: RoomState): JsValue = Json.obj(
      "type" -> "roomstate",
      "tags" -> o.tags.map(_.map(_.json).reduce(_ ++ _)),
      "channel" -> o.channel
    )
  }
  val userNoticeFormat: Format[UserNotice] = new Format[UserNotice] {
    override def reads(json: JsValue): JsResult[UserNotice] = for {
      _ <- (json \ "type").validate[String].filterNot(_ != "usernotice")
      tags <- (json \ "tags").validateOpt[JsObject].map(_.map(_.fields.flatMap(x => Tag.fromJson(x._1, x._2)).toSet))
      channel <- (json \ "channel").validate[String]
      message <- (json \ "message").validateOpt[String]
    } yield UserNotice(tags, channel, message)

    override def writes(o: UserNotice): JsValue = Json.obj(
      "type" -> "usernotice",
      "tags" -> o.tags.map(_.map(_.json).reduce(_ ++ _)),
      "channel" -> o.channel
    ) ++ o.message.map(t => Json.obj("message" -> t)).getOrElse(Json.obj())
  }
  val clearChatFormat: Format[ClearChat] = new Format[ClearChat] {
    override def reads(json: JsValue): JsResult[ClearChat] = for {
      _ <- (json \ "type").validate[String].filterNot(_ != "clearchat")
      tags <- (json \ "tags").validateOpt[JsObject].map(_.map(_.fields.flatMap(x => Tag.fromJson(x._1, x._2)).toSet))
      channel <- (json \ "channel").validate[String]
      username <- (json \ "username").validateOpt[String]
    } yield ClearChat(tags, channel, username)

    override def writes(o: ClearChat): JsValue = Json.obj(
      "type" -> "clearchat",
      "tags" -> o.tags.map(_.map(_.json).reduce(_ ++ _)),
      "channel" -> o.channel
    ) ++ o.username.map(t => Json.obj("username" -> t)).getOrElse(Json.obj())
  }
  val globalUserStateFormat: Format[GlobalUserState] = new Format[GlobalUserState] {
    override def reads(json: JsValue): JsResult[GlobalUserState] = for {
      _ <- (json \ "type").validate[String].filterNot(_ != "globaluserstate")
      tags <- (json \ "tags").validateOpt[JsObject].map(_.map(_.fields.flatMap(x => Tag.fromJson(x._1, x._2)).toSet))
    } yield GlobalUserState(tags)

    override def writes(o: GlobalUserState): JsValue = Json.obj(
      "type" -> "globaluserstate",
      "tags" -> o.tags.map(_.map(_.json).reduce(_ ++ _))
    )
  }
  val hostTargetFormat: Format[HostTarget] = new Format[HostTarget] {
    override def writes(o: HostTarget): JsValue = Json.obj(
      "type" -> o.`type`,
      "channel" -> o.channel,
      "target" -> o.target,
      "viewers" -> o.viewers
    )

    override def reads(json: JsValue): JsResult[HostTarget] = for {
      _ <- (json \ "type").validate[String].filterNot(_ != "hosttarget")
      channel <- (json \ "channel").validate[String]
      target <- (json \ "target").validateOpt[String]
      viewers <- (json \ "viewers").validate[Int]
    } yield HostTarget(channel, target, viewers)
  }

  implicit val messageFormat: Format[Message] = new Format[Message] {
    val formats = Seq(privMsgFormat, pingFormat, pongFormat, joinFormat, partFormat, hostTargetFormat, nickFormat, passFormat, membershipCapFormat, commandsCapFormat, tagsCapFormat, addOpFormats, removeOpFormats, noticeFormat, userStateFormat, roomStateFormat, userNoticeFormat, clearChatFormat)
    override def writes(o: Message): JsValue = o match {
      case Ping => pingFormat.writes(Ping)
      case Pong => pongFormat.writes(Pong)
      case part: Part => partFormat.writes(part)
      case join: Join => joinFormat.writes(join)
      case privMsg: PrivMsg => privMsgFormat.writes(privMsg)
      case nick: Nick => nickFormat.writes(nick)
      case pass: Pass => passFormat.writes(pass)
      case MembershipCap => membershipCapFormat.writes(MembershipCap)
      case CommandsCap => commandsCapFormat.writes(CommandsCap)
      case TagsCap => tagsCapFormat.writes(TagsCap)
      case addOp: AddOp => addOpFormats.writes(addOp)
      case removeOp: RemoveOp => removeOpFormats.writes(removeOp)
      case notice: Notice => noticeFormat.writes(notice)
      case userState: UserState => userStateFormat.writes(userState)
      case roomState: RoomState => roomStateFormat.writes(roomState)
      case userNotice: UserNotice => userNoticeFormat.writes(userNotice)
      case clearChat: ClearChat => clearChatFormat.writes(clearChat)
      case globalUserState: GlobalUserState => globalUserStateFormat.writes(globalUserState)
      case hostTarget: HostTarget => hostTargetFormat.writes(hostTarget)
      case _ => Json.obj("error" -> "Parse Error!")
    }
    override def reads(json: JsValue): JsResult[Message] = {
      formats.view.map(_.reads(json)).find(_.isSuccess) match {
        case Some(result) => result
        case None => JsError()
      }
    }
  }
  implicit val sendableMessageFormat: Format[SendableMessage] = new Format[SendableMessage] {
    val formats = Seq(privMsgFormat, pingFormat, pongFormat, joinFormat, partFormat, nickFormat, passFormat, membershipCapFormat, commandsCapFormat, tagsCapFormat)
    override def reads(json: JsValue): JsResult[SendableMessage] = formats.view.map(_.reads(json)).find(_.isSuccess) match {
      case Some(result) => result
      case None => JsError()
    }
    override def writes(o: SendableMessage): JsValue = messageFormat.writes(o)
  }


  sealed trait MultiMessagePart extends Message
  sealed trait WelcomeMessagePart extends MultiMessagePart {
    val name: String
  }
  sealed trait NamesListMessagePart extends MultiMessagePart {
    val name: String
    val channel: String
    val userList: Seq[String]
  }
  sealed trait MultiMessageEnd
  sealed trait Dedupable
  sealed trait Forwardable

  sealed trait ForwardableOnce

  object MultiMessage {
    def fromList(messages: List[MultiMessagePart]): Option[MultiMessage] = messages.headOption match {
      case Some(_: NamesListMessagePart) =>
        val namesListMessages = messages.filter(_.isInstanceOf[NamesListMessagePart]).map(_.asInstanceOf[NamesListMessagePart])
        val name = namesListMessages.head.name
        val channel = namesListMessages.head.channel
        val userList = namesListMessages.map(_.userList).reduce(_ ++ _)
        Some(NamesList(name, channel, userList, namesListMessages))
      case Some(_: WelcomeMessagePart) =>
        val welcomeMessages = messages.filter(_.isInstanceOf[WelcomeMessagePart]).map(_.asInstanceOf[WelcomeMessagePart])
        val name = welcomeMessages.head.name
        Some(Welcome(name, welcomeMessages))
      case _ => None
    }
  }
  sealed trait MultiMessage extends Message {
    val messages: Seq[MultiMessagePart]
  }

  sealed trait SendableMessage extends Message {
    def send: ByteString
  }
  sealed trait ModuleSendableMessage extends Message


  case class `001`(name: String) extends WelcomeMessagePart {
    override val `type`: String = "001"
    override def forward: ByteString = ByteString(s":tmi.twitch.tv 001 $name :Welcome, GLHF!\r\n")
  }
  case class `002`(name: String) extends WelcomeMessagePart {
    override val `type`: String = "002"
    override def forward: ByteString = ByteString(s":tmi.twitch.tv 002 $name :Your host is tmi.twitch.tv\r\n")
  }
  case class `003`(name: String) extends WelcomeMessagePart {
    override val `type`: String = "003"
    override def forward: ByteString = ByteString(s":tmi.twitch.tv 003 $name :This server is rather new\r\n")
  }
  case class `004`(name: String) extends WelcomeMessagePart {
    override val `type`: String = "004"
    override def forward: ByteString = ByteString(s":tmi.twitch.tv 004 $name :-\r\n")
  }
  case class `375`(name: String) extends WelcomeMessagePart {
    override val `type`: String = "375"
    override def forward: ByteString = ByteString(s":tmi.twitch.tv 375 $name :-\r\n")
  }
  case class `372`(name: String) extends WelcomeMessagePart {
    override val `type`: String = "372"
    override def forward: ByteString = ByteString(s":tmi.twitch.tv 372 $name :You are in a maze of twisty passages, all alike.\r\n")
  }
  case class `376`(name: String) extends WelcomeMessagePart with MultiMessageEnd {
    override val `type`: String = "376"
    override def forward: ByteString = ByteString(s":tmi.twitch.tv 376 $name :>\r\n")
  }
  object Welcome {
    val defaultMessages: Seq[(String) => WelcomeMessagePart with Product with Serializable] = Seq(`001`, `002`, `003`, `004`, `375`, `372`, `376`)
    def generate(name: String) = Welcome(name, defaultMessages.map(_(name)))
  }
  case class Welcome(name: String, messages: Seq[WelcomeMessagePart]) extends MultiMessage with Dedupable with ForwardableOnce {
    override val `type`: String = "welcome"

    override def forward: ByteString = messages.map(_.forward).reduce(_ ++ _)
  }

  case class `353`(name: String, channel: String, userList: Seq[String]) extends NamesListMessagePart {
    override val `type`: String = "353"

    override def forward: ByteString = ByteString(s":$name.tmi.twitch.tv 353 $name = #$channel :${userList.mkString(" ")}\r\n")
  }
  case class `366`(name: String, channel: String) extends NamesListMessagePart with MultiMessageEnd {
    override val `type`: String = "366"

    override def forward: ByteString = ByteString(s":$name.tmi.twitch.tv 366 $name #$channel :End of /NAMES list\r\n")

    override val userList: Seq[String] = Seq.empty[String]
  }
  case class NamesList(name: String, channel: String, userList: Seq[String], messages: Seq[Message with NamesListMessagePart]) extends MultiMessage with Forwardable {
    override val `type`: String = "names"

    override def forward: ByteString = messages.map(_.forward).reduce(_ ++ _)
  }

  case object Ping extends SendableMessage {
    override def forward: ByteString = ByteString(s"PING :tmi.twitch.tv\r\n")
    override val `type`: String = "ping"

    override def send: ByteString = forward
  }
  case object Pong extends SendableMessage {
    override def forward: ByteString = ByteString(s":tmi.twitch.tv PONG tmi.twitch.tv :tmi.twitch.tv\r\n")

    override val `type`: String = "pong"

    override def send: ByteString = ByteString(s"PONG :tmi.twitch.tv\r\n")
  }
  object Join {
    def apply(channel: String): Join = Join("", channel)
  }
  case class Join(name: String, channel: String) extends SendableMessage with ModuleSendableMessage with Forwardable {
    override def forward: ByteString = ByteString(s":$name!$name@$name.tmi.twitch.tv JOIN #$channel\r\n")

    override val `type`: String = "join"

    override def send: ByteString = ByteString(s"JOIN #$channel\r\n")
  }
  object Part {
    def apply(channel: String): Part = Part("", channel)
  }
  case class Part(name: String, channel: String) extends SendableMessage with Forwardable {
    override def forward: ByteString = ByteString(s":$name!$name@$name.tmi.twitch.tv PART #$channel\r\n")

    override val `type`: String = "part"

    override def send: ByteString = ByteString(s"PART #$channel\r\n")
  }

  object PrivMsg {
    def apply(channel: String, message: String): PrivMsg = PrivMsg(None, "", channel, message)
  }
  case class PrivMsg(tags: Option[Set[Tag]], name: String, channel: String, message: String) extends SendableMessage with ModuleSendableMessage with Dedupable with Forwardable {
    override val `type`: String = "privmsg"

    override def forward: ByteString = ByteString(s"${tags.map(t => s"@${t.map(_.stringRepr).mkString(";")} ").getOrElse("")}:$name!$name@$name.tmi.twitch.tv PRIVMSG #$channel :$message\r\n")

    override def send: ByteString = ByteString(s"PRIVMSG #$channel :$message\r\n")
  }
  case class Nick(name: String) extends SendableMessage {
    override def forward: ByteString = ByteString(s"NICK $name\r\n")

    override val `type`: String = "nick"

    override def send: ByteString = forward
  }
  case class Pass(pass: String) extends SendableMessage {
    override def forward: ByteString = ByteString(s"PASS oauth:$pass\r\n")

    override val `type`: String = "pass"

    override def send: ByteString = forward
  }
  case class HostTarget(channel: String, target: Option[String], viewers: Int) extends Message with Dedupable with Forwardable {
    override val `type`: String = "hosttarget"

    override def forward: ByteString = ByteString(s":tmi.twitch.tv HOSTTARGET #$channel :${target.getOrElse("-")} $viewers\r\n")
  }

  trait Capability extends SendableMessage with Dedupable with ForwardableOnce {
    def name: String
    override def forward: ByteString = ByteString(s":tmi.twitch.tv CAP * ACK :twitch.tv/$name\r\n")
    override def send: ByteString = ByteString(s"CAP REQ :twitch.tv/$name\r\n")
  }
  case object MembershipCap extends Capability {
    override def name: String = "membership"

    override val `type`: String = "membershipcapability"
  }
  case object CommandsCap extends Capability {
    override def name: String = "commands"

    override val `type`: String = "commandscapability"
  }
  case object TagsCap extends Capability {
    override def name: String = "tags"

    override val `type`: String = "tagscapability"
  }

  trait Mode extends Message with Forwardable
  case class AddOp(name: String, channel: String) extends Mode {
    override val `type`: String = "addop"

    override def forward: ByteString = ByteString(s":jtv MODE #$channel +o $name\r\n")
  }
  case class RemoveOp(name: String, channel: String) extends Mode {
    override val `type`: String = "removeop"

    override def forward: ByteString = ByteString(s":jtv MODE #$channel -o $name\r\n")
  }

  case class Notice(tags: Option[Set[Tag]], channel: String, message: String) extends Message with Dedupable with Forwardable {
    override val `type`: String = "notice"

    override def forward: ByteString = ByteString(s"${tags.map(t => s"@${t.map(_.stringRepr).mkString(";")} ").getOrElse("")}:tmi.twitch.tv NOTICE #$channel :$message\r\n")
  }
  case class UserState(tags: Option[Set[Tag]], channel: String) extends Message with Dedupable with Forwardable {
    override val `type`: String = "userstate"

    override def forward: ByteString = ByteString(s"${tags.map(t => s"@${t.map(_.stringRepr).mkString(";")} ").getOrElse("")}:tmi.twitch.tv USERSTATE #$channel\r\n")
  }
  case class GlobalUserState(tags: Option[Set[Tag]]) extends Message with Dedupable with Forwardable {
    override val `type`: String = "globaluserstate"

    override def forward: ByteString = ByteString(s"${tags.map(t => s"@${t.map(_.stringRepr).mkString(";")} ").getOrElse("")}:tmi.twitch.tv GLOBALUSERSTATE\r\n")
  }
  case class RoomState(tags: Option[Set[Tag]], channel: String) extends Message with Dedupable with Forwardable {
    override val `type`: String = "roomstate"

    override def forward: ByteString = ByteString(s"${tags.map(t => s"@${t.map(_.stringRepr).mkString(";")} ").getOrElse("")}:tmi.twitch.tv ROOMSTATE #$channel\r\n")
  }
  case class UserNotice(tags: Option[Set[Tag]], channel: String, message: Option[String]) extends Message with ModuleSendableMessage with Dedupable with Forwardable {
    override val `type`: String = "usernotice"

    override def forward: ByteString = ByteString(s"${tags.map(t => s"@${t.map(_.stringRepr).mkString(";")} ").getOrElse("")}:tmi.twitch.tv USERNOTICE #$channel${message.map(m => s" :$m")}\r\n")
  }
  case class ClearChat(tags: Option[Set[Tag]], channel: String, username: Option[String]) extends Message with Dedupable with Forwardable {
    override val `type`: String = "clearchat"

    override def forward: ByteString = ByteString(s"${tags.map(t => s"@${t.map(_.stringRepr).mkString(";")} ").getOrElse("")}:tmi.twitch.tv CLEARCHAT #$channel${username.map(u => s" :$u")}\r\n")
  }
}

sealed trait Message {
  val `type`: String
  def forward: ByteString
}