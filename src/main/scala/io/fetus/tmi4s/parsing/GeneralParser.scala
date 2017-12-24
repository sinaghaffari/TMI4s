package io.fetus.tmi4s.parsing

import java.util.UUID

/**
  * Created by Sina on 2017-02-09.
  */
object GeneralParser {
  import fastparse.all._
  val letter: P[String] = P(CharIn('a' to 'z') | CharIn('A' to 'Z')).!
  val digit: P[String] = P(CharIn('0' to  '9')).!
  val hex: P[String] = P(CharIn('a' to 'f', 'A' to 'F') | digit).!
  val binBool: P[Boolean] = P(CharIn("01").!).map {
    case "0" => false
    case "1" => true
  }
  val word: P[String] = P((CharIn('a' to 'z') | "_" | "-" | digit).rep(1)).!.map(_.toString)

  val name: P[String] = P(CharsWhile(c => !(c == ':' || c == ';' || c == '@' || c == '!' || c == '.' || c == ' ' || c == '=')).!)
  val fakeName: P[Unit] = P(CharsWhile(c => !(c == ':' || c == ';' || c == '@' || c == '!' || c == '.' || c == ' ' || c == '=')))
  val number: P[String] = P(digit.rep(1)).!
  val integer: P[Int] = P("-".!.? ~ number).!.map(_.toInt)
  val hexColor: P[String] = P("#" ~ (letter | digit).rep(min=6, max=6)).!
  val uuid: P[UUID] = P((hex.rep(min=8,max=8) ~ "-" ~ hex.rep(min=4,max=4) ~ "-" ~ hex.rep(min=4,max=4) ~ "-" ~ hex.rep(min=4,max=4) ~ "-" ~ hex.rep(min=12,max=12)).!).map(UUID.fromString)
  val string: P[String] = P(CharPred(_ != ' ').rep.!)

  val channel: P[String] = P("#" ~ name)
  val messageBody: P[String] = P(AnyChar.rep(1)).!
}