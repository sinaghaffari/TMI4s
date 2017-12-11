package io.fetus.tmi4s.models.irc.tags

import play.api.libs.json.{Format, JsResult, JsString, JsValue}

object UserType {
  implicit val userTypeFormat: Format[UserType] = new Format[UserType] {
    override def reads(json: JsValue): JsResult[UserType] = json.validate[String].map {
      case "mod" => Mod
      case "global_mod" => GlobalMod
      case "admin" => Admin
      case "staff" => Staff
      case "broadcaster" => Broadcaster
    }

    override def writes(o: UserType): JsValue = o match {
      case Mod => JsString("mod")
      case GlobalMod => JsString("global_mod")
      case Admin => JsString("admin")
      case Staff => JsString("staff")
      case Broadcaster => JsString("broadcaster")
    }
  }
  case object Staff extends UserType {
    val name = "staff"
  }
  case object Broadcaster extends UserType {
    val name = "broadcaster"
  }
  case object Mod extends UserType {
    val name = "mod"
  }
  case object GlobalMod extends UserType {
    val name = "global_mod"
  }
  case object Admin extends UserType {
    val name = "admin"
  }
}

sealed trait UserType {
  val name: String
}
