package io.fetus.tmi4s.core

import akka.actor.{Actor, ActorSystem, Props}
import akka.stream.ActorMaterializer
import com.google.inject.Guice
import com.typesafe.config.Config
import io.fetus.tmi4s.core.channelDistributor.ChannelDistributorManager
import io.fetus.tmi4s.core.messageDistributor.MessageConnection
import io.fetus.tmi4s.implicits.ImplicitsModule
import io.fetus.tmi4s.models.irc.{Authenticate, Message}
import io.fetus.tmi4s.models.irc.Message.{Capability, Join, Nick, Part, Pass, PrivMsg}
import io.fetus.tmi4s.models.irc.MessageContainer.{FromTwitch, ToTwitch}

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class TMI4s(config: Config) extends Actor {
  val caps: mutable.Set[Capability] = mutable.Set.empty
  var pass: Option[Pass] = None
  override def receive: Receive = {
    case ToTwitch(cap: Capability) =>
      caps.add(cap)
    case ToTwitch(pass: Pass) =>
      this.pass = Some(pass)
    case ToTwitch(nick: Nick) if pass.isDefined =>
      val auth = Authenticate(nick, pass.get, caps.toSet)
      val channelDistributor = context.actorOf(
        ChannelDistributorManager.props(
          config.getInt("tmi4s.twitch.irc.limits.joins.number"),
          config.getInt("tmi4s.twitch.irc.limits.joins.per").milliseconds,
          redundant = config.getBoolean("tmi4s.redundancy"),
          auth
        )
      )
      val messageConnection = context.actorOf(
        MessageConnection.props(
          config.getInt("tmi4s.twitch.irc.limits.messages.number"),
          config.getInt("tmi4s.twitch.irc.limits.messages.per").milliseconds,
          auth
        )
      )
      context.become {
        case ToTwitch(join: Join) => channelDistributor ! ToTwitch(join)
        case ToTwitch(part: Part) => channelDistributor ! ToTwitch(part)
        case ToTwitch(msg: PrivMsg) => messageConnection ! ToTwitch(msg)
        case FromTwitch(msg: Message) => context.parent ! FromTwitch(msg)
      }
  }
}

object TMI4s {
  def main(args: Array[String]): Unit = {
    val injector = Guice.createInjector(new ImplicitsModule)
    implicit val system: ActorSystem = injector.getInstance(classOf[ActorSystem])
    implicit val materializer: ActorMaterializer = injector.getInstance(classOf[ActorMaterializer])
    implicit val ec: ExecutionContext = injector.getInstance(classOf[ExecutionContext])
    val config: Config = injector.getInstance(classOf[Config])
    system.actorOf(TMI4sServer.props(config))
  }
  def props(config: Config): Props = Props(new TMI4s(config))
}