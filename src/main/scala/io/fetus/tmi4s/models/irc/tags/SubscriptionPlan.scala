package io.fetus.tmi4s.models.irc.tags

sealed trait SubscriptionPlan

object SubscriptionPlan {
  case object Prime extends SubscriptionPlan
  case object `1000` extends SubscriptionPlan
  case object `2000` extends SubscriptionPlan
  case object `3000` extends SubscriptionPlan

  def parse(s: String): Option[SubscriptionPlan] = s match {
    case "Prime" => Some(Prime)
    case "1000" => Some(`1000`)
    case "2000" => Some(`2000`)
    case "3000" => Some(`3000`)
    case _ => None
  }
}
