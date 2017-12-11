package io.fetus.tmi4s.core.channelDistributor

import akka.actor.{Actor, Props}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import io.fetus.tmi4s.core.channelDistributor.ChannelConnection.Disconnected
import io.fetus.tmi4s.models.irc.Message.{Join, Part}
import io.fetus.tmi4s.models.irc.MessageContainer.{FromTwitch, ToTwitch}
import io.fetus.tmi4s.models.irc.{Authenticate, Message}

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

/**
  * Created on 2017-11-28.
  *
  * @author Sina
  */
class ChannelDistributor(num: Int, per: FiniteDuration, auth: Authenticate) extends Actor {
  implicit val ec: ExecutionContext = context.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system).withSupervisionStrategy(_ => Supervision.Restart))(context.system)
  val routees = mutable.Map.empty[String, ChannelConnection]
  val channelToRoutee = mutable.Map.empty[String, String]

  val nameIterator: Iterator[String] = Iterator
    .iterate(List(""))(_.flatMap(s => ('a' to 'z').map(s + _)))
    .flatten
    .drop(1)
    .map("$" + _)

  override def receive: Receive = {
    case ToTwitch(join: Join) =>
      // Find an available routee to send the message to. If none are available, create one.
      routees.find(_._2.hasRoom).fold {
        val routee = new ChannelConnection(num, per, auth, self, nameIterator.next())(context.system, materializer, ec)
        routees.put(routee.name, routee)
        channelToRoutee.put(join.channel, routee.name)
        routee.join(ToTwitch(join))
      } {
        case (routeeName, routee) =>
          channelToRoutee.put(join.channel, routeeName)
          routee.join(ToTwitch(join))
      }
    case ToTwitch(part: Part) =>
      channelToRoutee.get(part.channel).flatMap(routees.get).foreach { routee =>
        routee.part(ToTwitch(part))
        if (routee.idle) {
          routee.shutdown()
        }
      }
    case FromTwitch(msg: Message) =>
      context.parent ! FromTwitch(msg)
    case Disconnected(routee, Some(ex)) =>
      println(s"TMI4s Channel Distributor ${routee.name} Disconnected")
      println(s"\tCause: ${ex.getMessage} because of ${ex.getCause.getMessage}")
      println(s"\tConnection Hosted Channels:")
      println(routee.channels.map(channel => s"\t\t$channel").mkString(",\n"))
      routees.remove(routee.name)
      routee.channels.foreach { channel =>
        channelToRoutee.remove(channel)
        self ! ToTwitch(Join(channel))
      }
    case Disconnected(routee, None) =>
      println(s"TMI4s Channel Distributor ${routee.name} Disconnected")
      println(s"\tCause: Unknown")
      println(s"\tConnection Hosted Channels:")
      println(routee.channels.map(channel => s"\t\t$channel").mkString(",\n"))
      routees.remove(routee.name)
      routee.channels.foreach { channel =>
        channelToRoutee.remove(channel)
        self ! ToTwitch(Join(channel))
      }
  }
}

object ChannelDistributor {
  def props(num: Int, per: FiniteDuration, auth: Authenticate): Props = Props(new ChannelDistributor(num, per, auth))
}
