package io.fetus.tmi4s.models.irc.tags

/**
  * Created on 2016-07-03.
  *
  * @author Sina
  */
object MessageId {

  def toMessageID(token: String): Option[MessageId] = token match {
    case "subs_on" => Some(SubsOn)
    case "subs_off" => Some(SubsOff)
    case "already_subs_on" => Some(AlreadySubsOn)
    case "already_subs_off" => Some(AlreadySubsOff)
    case "slow_on" => Some(SlowOn)
    case "slow_off" => Some(SlowOff)
    case "r9k_on" => Some(R9KOn)
    case "r9k_off" => Some(R9KOff)
    case "already_r9k_on" => Some(AlreadyR9KOn)
    case "already_r9k_off" => Some(AlreadyR9KOff)
    case "host_on" => Some(HostOn)
    case "host_off" => Some(HostOff)
    case "bad_host_hosting" => Some(BadHostHosting)
    case "hosts_remaining" => Some(HostsRemaining)
    case "emote_only_on" => Some(EmoteOnlyOn)
    case "emote_only_off" => Some(EmoteOnlyOff)
    case "already_emote_only_on" => Some(AlreadyEmoteOnlyOn)
    case "already_emote_only_off" => Some(AlreadyEmoteOnlyOff)
    case "msg_channel_suspended" => Some(MsgChannelSuspended)
    case "timeout_success" => Some(TimeoutSuccess)
    case "ban_success" => Some(BanSuccess)
    case "unban_success" => Some(UnbanSuccess)
    case "bad_unban_no_ban" => Some(BadUnbanNoBan)
    case "already_banned" => Some(AlreadyBanned)
    case "unrecognized_cmd" => Some(UnrecognizedCommand)
    case "followers_on_zero" => Some(FollowersOnZero)
    case "followers_off" => Some(FollowersOff)
    case "followers_on" => Some(FollowersOn)
    case "room_mods" => Some(RoomMods)
    case "sub" => Some(Sub)
    case "resub" => Some(Resub)
    case "raid" => Some(Raid)
    case "ritual" => Some(Ritual)
    case _ => None
  }
  case object AlreadyBanned extends BanNotice {
    override val name: String = "already_banned"
  }
  case object AlreadyEmoteOnlyOff extends EmoteNotice {
    override val name: String = "already_emote_only_off"
  }
  case object AlreadyEmoteOnlyOn extends EmoteNotice {
    override val name: String = "already_emote_only_on"
  }
  case object AlreadyR9KOff extends R9KNotice {
    override val name: String = "already_r9k_off"
  }
  case object AlreadyR9KOn extends R9KNotice {
    override val name: String = "already_r9k_on"
  }
  case object AlreadySubsOff extends SubNotice {
    override val name: String = "already_subs_off"
  }
  case object AlreadySubsOn extends SubNotice {
    override val name: String = "already_subs_on"
  }
  case object BadHostHosting extends HostNotice {
    override val name: String = "bad_host_hosting"
  }
  case object BadUnbanNoBan extends BanNotice {
    override val name: String = "bad_unban_no_ban"
  }
  case object BanSuccess extends BanNotice {
    override val name: String = "ban_success"
  }
  case object EmoteOnlyOff extends EmoteNotice {
    override val name: String = "emote_only_off"
  }
  case object EmoteOnlyOn extends EmoteNotice {
    override val name: String = "emote_only_on"
  }
  case object HostOff extends HostNotice {
    override val name: String = "host_off"
  }
  case object HostOn extends HostNotice {
    override val name: String = "host_on"
  }
  case object HostsRemaining extends HostNotice {
    override val name: String = "hosts_remaining"
  }
  case object MsgChannelSuspended extends ChannelNotice {
    override val name: String = "msg_channel_suspended"
  }
  case object R9KOff extends R9KNotice {
    override val name: String = "r9k_off"
  }
  case object R9KOn extends R9KNotice {
    override val name: String = "r9k_on"
  }
  case object SlowOff extends SlowNotice {
    override val name: String = "slow_off"
  }
  case object SlowOn extends SlowNotice {
    override val name: String = "slow_on"
  }
  case object SubsOff extends SubNotice {
    override val name: String = "subs_off"
  }
  case object SubsOn extends SubNotice {
    override val name: String = "subs_on"
  }
  case object TimeoutSuccess extends BanNotice {
    override val name: String = "timeout_success"
  }
  case object UnbanSuccess extends BanNotice {
    override val name: String = "unban_success"
  }
  case object UnrecognizedCommand extends MiscNotice {
    override val name: String = "unrecognized_command"
  }

  case object FollowersOnZero extends FollowNotice {
    override val name: String = "followers_on_zero"
  }
  case object FollowersOff extends FollowNotice {
    override val name: String = "followers_off"
  }
  case object FollowersOn extends FollowNotice {
    override val name: String = "followers_on"
  }
  case object RoomMods extends MiscNotice {
    override val name: String = "room_mods"
  }

  case object Sub extends SubNotice {
    override val name: String = "sub"
  }
  case object Resub extends SubNotice {
    override val name: String = "resub"
  }
  case object Raid extends RaidNotice {
    override val name: String = "raid"
  }
  case object Ritual extends RitualNotice {
    override val name: String = "ritual"
  }

  sealed trait SubNotice extends MessageId
  sealed trait SlowNotice extends MessageId
  sealed trait R9KNotice extends MessageId
  sealed trait HostNotice extends MessageId
  sealed trait EmoteNotice extends MessageId
  sealed trait ChannelNotice extends MessageId
  sealed trait BanNotice extends MessageId
  sealed trait MiscNotice extends MessageId
  sealed trait FollowNotice extends MessageId
  sealed trait RaidNotice extends MessageId
  sealed trait RitualNotice extends MessageId
}

sealed trait MessageId {
  val name: String
}
