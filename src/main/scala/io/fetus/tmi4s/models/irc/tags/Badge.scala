package io.fetus.tmi4s.models.irc.tags

import play.api.libs.json.{Format, Json}

case class Badge(name: String, number: Long) {
  override def toString: String = s"$name/$number"
}

object Badge {
  implicit val badgeFormat: Format[Badge] = Json.format[Badge]
}
