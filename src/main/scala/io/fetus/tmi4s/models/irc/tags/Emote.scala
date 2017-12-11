package io.fetus.tmi4s.models.irc.tags

import play.api.libs.json.{Format, JsResult, JsValue, Json}

case class Emote(emoteId: Int, ranges: Seq[Range])

object Emote {
  implicit val rangeFormat: Format[Range] = new Format[Range] {
    override def reads(json: JsValue): JsResult[Range] = for {
      start <- (json \ "start").validate[Int]
      end <- (json  \ "end").validate[Int]
    } yield Range(start, end)

    override def writes(o: Range): JsValue = Json.obj(
      "start" -> o.start,
      "end" -> o.end
    )
  }
  implicit val emoteFormat: Format[Emote] = Json.format[Emote]
}
