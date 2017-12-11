package io.fetus.tmi4s.models.irc.tags

import java.util.UUID

import play.api.libs.json.{JsObject, JsString, JsValue, Json}

object Tag {
  def fromJson(key: String, value: JsValue): Option[Tag] = {
    key match {
      case "badges" => value.validate[Seq[Badge]].asOpt.map(BadgesTag.apply)
      case "bits" => value.validate[Int].asOpt.map(BitsTag.apply)
      case "color" => value.validate[String].asOpt.map(ColorTag.apply)
      case "display_name" => value.validate[String].asOpt.map(DisplayNameTag.apply)
      case "emotes" => value.validate[Seq[Emote]].asOpt.map(EmotesTag.apply)
      case "id" => value.validate[UUID].asOpt.map(IdTag.apply)
      case "user_is_moderator" => value.validate[Boolean].asOpt.map(ModTag.apply)
      case "channel_id" => value.validate[String].asOpt.map(RoomIdTag.apply)
      case "sent_time_stamp" => value.validate[Long].asOpt.map(SentTsTag.apply)
      case "user_is_subscriber" => value.validate[Boolean].asOpt.map(SubscriberTag.apply)
      case "tmi_sent_time_stamp" => value.validate[Long].asOpt.map(TmiSentTsTag.apply)
      case "user_is_turbo" => value.validate[Boolean].asOpt.map(TurboTag.apply)
      case "user_id" => value.validate[String].asOpt.map(UserIdTag.apply)
      case "user_type" => value.validate[UserType].asOpt.map(UserTypeTag.apply)
      case "emote-sets" => value.validate[Seq[Int]].asOpt.map(EmoteSetsTag.apply)
      case "msg-id" => value.validate[String].asOpt.flatMap(MessageId.toMessageID).map(MessageIdTag.apply)
      case "broadcaster-lang" => value.validate[String].asOpt.map(BroadcasterLanguageTag.apply)
      case "emote-only" => value.validate[Boolean].asOpt.map(EmoteOnlyTag.apply)
      case "followers-only" => value.validate[Int].asOpt.map(FollowersOnlyTag.apply)
      case "r9k" => value.validate[Boolean].asOpt.map(R9KTag.apply)
      case "slow" => value.validate[Int].asOpt.map(SlowTag.apply)
      case "subs-only" => value.validate[Boolean].asOpt.map(SubsOnlyTag.apply)
      case "msg-param-months" => value.validate[Int].asOpt.map(MessageParameterMonthsTag.apply)
      case "system-msg" => value.validate[String].asOpt.map(SystemMessageTag.apply)
      case "login" => value.validate[String].asOpt.map(LoginTag.apply)
      case "ban-reason" => value.validate[String].asOpt.map(BanReasonTag.apply)
      case "ban-duration" => value.validate[Int].asOpt.map(BanDurationTag.apply)
      case "target-user-id" => value.validate[String].asOpt.map(TargetUserId.apply)
      case _ =>
        println(key, value)
        None
    }
  }

  sealed trait PrivMsgTag extends Tag
  sealed trait NoticeTag extends Tag
  sealed trait UserNoticeTag extends Tag
  sealed trait UserStateTag extends Tag
  sealed trait GlobalUserStateTag extends Tag
  sealed trait RoomStateTag extends Tag
  sealed trait ClearChatTag extends Tag

  case class BadgesTag(value: Seq[Badge]) extends PrivMsgTag with UserStateTag with GlobalUserStateTag with UserNoticeTag {
    override val name: String = "badges"
    override val json: JsObject = Json.obj(name -> value)

    override val stringRepr: String = s"$name=${value.mkString(",")}"
  }
  case class BitsTag(value: Int) extends PrivMsgTag with UserStateTag {
    override val name: String = "bits"
    override val json: JsObject = Json.obj(name -> value)

    override val stringRepr: String = s"$name=$value"
  }
  case class ColorTag(value: String) extends PrivMsgTag with UserStateTag with GlobalUserStateTag with UserNoticeTag {
    override val name: String = "color"
    override val json: JsObject = Json.obj(name -> value)

    override val stringRepr: String = s"$name=$value"
  }
  case class DisplayNameTag(value: String) extends PrivMsgTag with UserStateTag with GlobalUserStateTag with UserNoticeTag {
    override val name: String = "display-name"
    override val json: JsObject = Json.obj(name -> value)

    override val stringRepr: String = s"$name=$value"
  }
  case class EmotesTag(value: Seq[Emote]) extends PrivMsgTag with UserNoticeTag {
    override val name: String = "emotes"
    override val json: JsObject = Json.obj(name -> value)

    override val stringRepr: String = s"$name=${value.mkString("/")}"
  }
  case class IdTag(value: UUID) extends PrivMsgTag {
    override val name: String = "id"
    override val json: JsObject = Json.obj(name -> value.toString)

    override val stringRepr: String = s"$name=$value"
  }
  case class ModTag(value: Boolean) extends PrivMsgTag with UserStateTag with UserNoticeTag {
    override val name: String = "mod"
    override val json: JsObject = Json.obj(name -> value)

    override val stringRepr: String = s"$name=${if(value) 1 else 0}"
  }
  case class RoomIdTag(value: String) extends PrivMsgTag with UserNoticeTag with ClearChatTag {
    override val name: String = "room-id"
    override val json: JsObject = Json.obj(name -> value)

    override val stringRepr: String = s"$name=$value"
  }
  case class SentTsTag(value: Long) extends PrivMsgTag {
    override val name: String = "sent-ts"
    override val json: JsObject = Json.obj(name -> value)

    override val stringRepr: String = s"$name=$value"
  }
  case class TmiSentTsTag(value: Long) extends PrivMsgTag {
    override val name: String = "tmi-sent-ts"
    override val json: JsObject = Json.obj(name -> value)

    override val stringRepr: String = s"$name=$value"
  }
  case class TurboTag(value: Boolean) extends PrivMsgTag with UserStateTag with GlobalUserStateTag with UserNoticeTag {
    override val name: String = "turbo"
    override val json: JsObject = Json.obj(name -> value)

    override val stringRepr: String = s"$name=${if(value) 1 else 0}"
  }
  case class UserIdTag(value: String) extends PrivMsgTag with GlobalUserStateTag with UserNoticeTag {
    override val name: String = "user_id"
    override val json: JsObject = Json.obj(name -> value)

    override val stringRepr: String = s"$name=$value"
  }
  case class SubscriberTag(value: Boolean) extends PrivMsgTag with UserStateTag with UserNoticeTag {
    override val name: String = "subscriber"
    override val json: JsObject = Json.obj(name -> value)

    override val stringRepr: String = s"$name=${if(value) 1 else 0}"
  }
  case class UserTypeTag(value: UserType) extends PrivMsgTag with UserStateTag with GlobalUserStateTag with UserNoticeTag {
    override val name: String = "user_type"
    override val json: JsObject = Json.obj(name -> value.name)

    override val stringRepr: String = s"$name=$value"
  }
  case class EmoteSetsTag(value: Seq[Int]) extends UserStateTag with GlobalUserStateTag {
    override val name: String = "emote-sets"
    override val json: JsObject = Json.obj(name -> value)

    override val stringRepr: String = s"$name=${value.mkString(",")}"
  }
  case class MessageIdTag(value: MessageId) extends NoticeTag with UserNoticeTag{
    override val name: String = "msg-id"
    override val json: JsObject = Json.obj(name -> value.name)

    override val stringRepr: String = s"$name=${value.name}"
  }
  case class BroadcasterLanguageTag(value: String) extends RoomStateTag {
    override val name: String = "broadcaster-lang"
    override val json: JsObject = Json.obj(name -> value)

    override val stringRepr: String = s"$name=$value"
  }
  case class EmoteOnlyTag(value: Boolean) extends RoomStateTag {
    override val name: String = "emote-only"
    override val json: JsObject = Json.obj(name -> value)

    override val stringRepr: String = s"$name=${if(value) 1 else 0}"
  }
  case class FollowersOnlyTag(value: Int) extends RoomStateTag {
    override val name: String = "followers-only"
    override val json: JsObject = Json.obj(name -> value)

    override val stringRepr: String = s"$name=$value"
  }
  case class R9KTag(value: Boolean) extends RoomStateTag {
    override val name: String = "r9k"
    override val json: JsObject = Json.obj(name -> value)

    override val stringRepr: String = s"$name=${if(value) 1 else 0}"
  }
  case class SlowTag(value: Int) extends RoomStateTag {
    override val name: String = "slow"
    override val json: JsObject = Json.obj(name -> value)

    override val stringRepr: String = s"$name=$value"
  }
  case class SubsOnlyTag(value: Boolean) extends RoomStateTag {
    override val name: String = "subs-only"
    override val json: JsObject = Json.obj(name -> value)

    override val stringRepr: String = s"$name=${if(value) 1 else 0}"
  }
  case class MessageParameterDisplayName(value: String) extends UserNoticeTag {
    override val name: String = "msg-param-displayName"
    override val json: JsObject = Json.obj(name -> value)

    override val stringRepr: String = s"$name=$value"
  }
  case class MessageParameterLogin(value: String) extends UserNoticeTag {
    override val name: String = "msg-param-login"
    override val json: JsObject = Json.obj(name -> value)

    override val stringRepr: String = s"$name=$value"
  }
  case class MessageParameterMonthsTag(value: Int) extends UserNoticeTag {
    override val name: String = "msg-param-months"
    override val json: JsObject = Json.obj(name -> value)

    override val stringRepr: String = s"$name=$value"
  }
  case class MessageParameterSubPlan(value: SubscriptionPlan) extends UserNoticeTag {
    override val name: String = "msg-param-sub-plan"
    override val json: JsObject = Json.obj(name -> value.toString)

    override val stringRepr: String = s"$name=$value"
  }
  case class MessageParameterSubPlanName(value: String) extends UserNoticeTag {
    override val name: String = "msg-param-sub-plan-name"
    override val json: JsObject = Json.obj(name -> value)

    override val stringRepr: String = s"$name=$value"
  }
  case class MessageParameterViewerCount(value: Int) extends UserNoticeTag {
    override val name: String = "msg-param-viewerCount"
    override val json: JsObject = Json.obj(name -> value)

    override val stringRepr: String = s"$name=$value"
  }
  case class MessageParameterRitualName(value: String) extends UserNoticeTag {
    override val name: String = "msg-param-ritual-name"
    override val json: JsObject = Json.obj(name -> value)

    override val stringRepr: String = s"$name=$value"
  }
  case class SystemMessageTag(value: String) extends UserNoticeTag {
    override val name: String = "system-msg"
    override val json: JsObject = Json.obj(name -> value)

    override val stringRepr: String = s"$name=$value"
  }
  case class LoginTag(value: String) extends UserNoticeTag {
    override val name: String = "login"
    override val json: JsObject = Json.obj(name -> value)

    override val stringRepr: String = s"$name=$value"
  }
  case class BanReasonTag(value: String) extends ClearChatTag {
    override val name: String = "ban-reason"
    override val json: JsObject = Json.obj(name -> value)

    override val stringRepr: String = s"$name=$value"
  }
  case class BanDurationTag(value: Int) extends ClearChatTag {
    override val name: String = "ban-duration"
    override val json: JsObject = Json.obj(name -> value)

    override val stringRepr: String = s"$name=$value"
  }
  case class TargetUserId(value: String) extends ClearChatTag {
    override val name: String = "target-user-id"
    override val json: JsObject = Json.obj(name -> value)

    override val stringRepr: String = s"$name=$value"
  }
  case class UnknownTag(name: String, value: Option[String]) extends Tag {
    override val json: JsObject = Json.obj(name -> JsString(value.getOrElse("")))
    override val stringRepr: String = s"$name=${value.getOrElse("")}"
  }
}
sealed trait Tag {
  val value: Any
  val name: String
  val json: JsObject
  val stringRepr: String
}
