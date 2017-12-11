package io.fetus.tmi4s.parsing

import io.fetus.tmi4s.models.irc.tags.Tag._
import io.fetus.tmi4s.models.irc.tags._

/**
  * Created by Sina on 2017-02-10.
  */
object TagParser {
  import GeneralParser._
  import fastparse.all._
  val tagValueString: P[String] = P(CharsWhile(x => x != ';' && x != ' ').!)

  val badge: P[Option[Badge]] = P(word ~ "/" ~ number).map { case (s, n) => Some(Badge(s, n.toLong)) }
  val badges: P[Option[BadgesTag]] = P("badges=" ~ badge.rep(0, ",").map(_.flatten)).map(BadgesTag.apply).map(b => if (b.value.isEmpty) None else Some(b))
  val bits: P[Option[BitsTag]] = P("bits=" ~ integer.?).map(_.map(t => BitsTag(t)))
  val color: P[Option[ColorTag]] = P("color=" ~ hexColor.?).map(_.map(ColorTag.apply))
  val displayName: P[Option[DisplayNameTag]] = P("display-name=" ~ name.?).map(_.map(DisplayNameTag.apply))
  val range: P[Range] = P(number.map(_.toInt) ~ "-" ~ number.map(_.toInt)).map(x => Range.apply(x._1, x._2))
  val emote: P[Emote] = P(number.map(_.toInt) ~ ":" ~ range.rep(min = 1, sep = ",")).map(x => Emote.apply(x._1, x._2))
  val emotes: P[Option[EmotesTag]] = P("emotes=" ~ emote.rep(0, "/")).map(EmotesTag.apply).map(b => if (b.value.isEmpty) None else Some(b))
  val id: P[Option[IdTag]] = P("id=" ~ uuid.?).map(_.map(IdTag.apply))
  val mod: P[Option[ModTag]] = P("mod=" ~ binBool.?).map(_.map(ModTag.apply))
  val roomId: P[Option[RoomIdTag]] = P("room-id=" ~ number.?).map(_.map(RoomIdTag.apply))
  val sentTS: P[Option[SentTsTag]] = P("sent-ts=" ~ number.map(_.toLong).?).map(_.map(SentTsTag.apply))
  val subscriber: P[Option[SubscriberTag]] = P("subscriber=" ~ binBool.?).map(_.map(SubscriberTag.apply))
  val tmiSentTS: P[Option[TmiSentTsTag]] = P("tmi-sent-ts=" ~ number.map(_.toLong).?).map(_.map(TmiSentTsTag.apply))
  val turbo: P[Option[TurboTag]] = P("turbo=" ~ binBool.?).map(_.map(TurboTag.apply))
  val userId: P[Option[UserIdTag]] = P("user-id=" ~ number.?).map(_.map(UserIdTag.apply))
  val userType: P[Option[UserTypeTag]] = P("user-type=" ~ name.?).map(_.map {
    case "mod" => Some(UserType.Mod)
    case "global_mod" => Some(UserType.GlobalMod)
    case "admin" => Some(UserType.Admin)
    case "staff" => Some(UserType.Staff)
    case _ => None
  }).map(_.flatten).map(_.map(UserTypeTag.apply))
  val emoteSets: P[Option[EmoteSetsTag]] = P("emote-sets=" ~ number.map(_.toInt).rep(0, ",")).map(EmoteSetsTag.apply).map(b => if (b.value.isEmpty) None else Some(b))
  val messageId: P[Option[MessageIdTag]] = P("msg-id=" ~ name.?).map(_.flatMap(MessageId.toMessageID)).map(_.map(MessageIdTag.apply))
  val broadcasterLanguage: P[Option[BroadcasterLanguageTag]] = P("broadcaster-lang=" ~ name.?).map(_.map(BroadcasterLanguageTag.apply))
  val emoteOnly: P[Option[EmoteOnlyTag]] = P("emote-only=" ~ binBool.?).map(_.map(EmoteOnlyTag.apply))
  val followersOnly: P[Option[FollowersOnlyTag]] = P("followers-only=" ~ integer.?).map(_.map(FollowersOnlyTag.apply))
  val r9k: P[Option[R9KTag]] = P("r9k=" ~ binBool.?).map(_.map(R9KTag.apply))
  val slow: P[Option[SlowTag]] = P("slow=" ~ integer.?).map(_.map(SlowTag.apply))
  val subsOnly: P[Option[SubsOnlyTag]] = P("subs-only=" ~ binBool.?).map(_.map(SubsOnlyTag.apply))
  val msgParamDisplayName: P[Option[MessageParameterDisplayName]] = P("msg-param-displayName=" ~ tagValueString.?).map(_.map(MessageParameterDisplayName.apply))
  val msgParamLogin: P[Option[MessageParameterLogin]] = P("msg-param-login=" ~ tagValueString.?).map(_.map(MessageParameterLogin.apply))
  val msgParamMonths: P[Option[MessageParameterMonthsTag]] = P("msg-param-months=" ~ number.map(_.toInt).?).map(_.map(MessageParameterMonthsTag.apply))
  val msgParamSubPlan: P[Option[MessageParameterSubPlan]] = P("msg-param-sub-plan=" ~ tagValueString.?).map(_.flatMap(SubscriptionPlan.parse).map(MessageParameterSubPlan.apply))
  val msgParamSubPlanName: P[Option[MessageParameterSubPlanName]] = P("msg-param-sub-plan-name=" ~ tagValueString.?).map(_.map(MessageParameterSubPlanName.apply))
  val msgParamViewerCount: P[Option[MessageParameterViewerCount]] = P("msg-param-viewerCount=" ~ number.map(_.toInt).?).map(_.map(MessageParameterViewerCount.apply))
  val msgParamRitualName: P[Option[MessageParameterRitualName]] = P("msg-param-ritual-name=" ~ tagValueString.?).map(_.map(MessageParameterRitualName.apply))
  val systemMsg: P[Option[SystemMessageTag]] = P("system-msg=" ~ tagValueString.?).map(_.map(SystemMessageTag.apply))
  val login: P[Option[LoginTag]] = P("login=" ~ name.?).map(_.map(LoginTag.apply))
  val banReason: P[Option[BanReasonTag]] = P("ban-reason=" ~ tagValueString.?).map(_.map(BanReasonTag.apply))
  val banDuration: P[Option[BanDurationTag]] = P("ban-duration=" ~ number.map(_.toInt).?).map(_.map(BanDurationTag.apply))
  val targetUserId: P[Option[TargetUserId]] = P("target-user-id=" ~ tagValueString.?).map(_.map(TargetUserId.apply))
  val unknownTag: P[Option[UnknownTag]] = P(CharsWhile(_ != '=').! ~ "=" ~ tagValueString.?).map(UnknownTag.tupled).map(Some.apply)

  val tag: P[Option[Tag]] = P(
    badges |
    bits |
    color |
    displayName |
    emotes |
    id |
    mod |
    roomId |
    sentTS |
    subscriber |
    tmiSentTS |
    turbo |
    userId |
    userType |
    emoteSets |
    messageId |
    broadcasterLanguage |
    emoteOnly |
    messageId |
    followersOnly |
    r9k |
    slow |
    subsOnly |
    msgParamDisplayName |
    msgParamLogin |
    msgParamMonths |
    msgParamSubPlan |
    msgParamSubPlanName |
    msgParamViewerCount |
    msgParamRitualName |
    systemMsg |
    login |
    banReason |
    banDuration |
    targetUserId |
    unknownTag
  )
  val tags: P[Set[Tag]] = P("@" ~ tag.rep(0, ";").map(_.flatten.toSet) ~ " ")
}
