package io.fetus.tmi4s.core.channelDistributor

import akka.actor.{ActorRef, ActorSystem}
import akka.stream._
import akka.stream.scaladsl.{Broadcast, Concat, GraphDSL, Merge, RunnableGraph, Sink, Source, SourceQueueWithComplete}
import akka.{Done, NotUsed}
import io.fetus.tmi4s.core.channelDistributor.ChannelConnection.Disconnected
import io.fetus.tmi4s.models.irc.Message.{Join, Part, Ping, SendableMessage}
import io.fetus.tmi4s.models.irc.MessageContainer.{FromTwitch, ToTwitch}
import io.fetus.tmi4s.models.irc.{Authenticate, Message}

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Success}

class ChannelConnection(num: Int, per: FiniteDuration, auth: Authenticate, parent: ActorRef, val name: String)(implicit private val system: ActorSystem, mat: Materializer, ec: ExecutionContext) {
  private val _channels: mutable.Set[String] = mutable.Set.empty
  private val (joins, parts, connected, killSwitch, completed) = {
    import io.fetus.tmi4s.util.Flows._
    val authSource: Source[ToTwitch[Message.SendableMessage], NotUsed] = Source(auth.caps.toList ++ List(auth.pass, auth.nick)).map(ToTwitch.apply)
    val joins: Source[ToTwitch[Join], SourceQueueWithComplete[ToTwitch[Join]]] = Source.queue[ToTwitch[Join]](100000, OverflowStrategy.backpressure).throttle(num, per, num, ThrottleMode.shaping)
    val parts: Source[ToTwitch[Part], SourceQueueWithComplete[ToTwitch[Part]]] = Source.queue[ToTwitch[Part]](100000, OverflowStrategy.backpressure)
    val responder = Sink.foreach[FromTwitch[Message]](parent ! _)
    val killSwitch: Graph[FlowShape[ToTwitch[SendableMessage], ToTwitch[SendableMessage]], UniqueKillSwitch] = KillSwitches.single[ToTwitch[SendableMessage]]
    val graph = GraphDSL.create(joins, parts, tcp("irc.chat.twitch.tv", 6667), killSwitch, responder)((_, _, _, _, _)) { implicit builder => (joins, parts, connection, killSwitch, responder) =>
      import GraphDSL.Implicits._

      val messageCombiner = builder.add(Merge[ToTwitch[SendableMessage]](2))
      val authenticator = builder.add(Concat[ToTwitch[SendableMessage]](2))
      val pongCombiner = builder.add(Merge[ToTwitch[SendableMessage]](2))
      val pingSplitter = builder.add(Broadcast[FromTwitch[Message]](2))
      joins ~> messageCombiner.in(0)
      parts ~> messageCombiner.in(1)

      authSource ~> authenticator.in(0)
      messageCombiner.out ~> authenticator.in(1)

      authenticator.out ~> pongCombiner.in(0)
      pongCombiner.out ~> killSwitch ~> connection ~> pingSplitter.in
      pingSplitter.out(0).map(_.msg) ~> filterType[Ping.type] ~> pingToPongToTwitch ~> pongCombiner.in(1)
      pingSplitter.out(1) ~> responder

      ClosedShape
    }
    RunnableGraph.fromGraph(graph).run()
  }
  completed.onComplete {
    case Success(Done) =>
      println(s"TMI4s Channel Connection Disconnected:")
      println(s"\tConnection: $name")
      println(s"\tDropped Channels:")
      println(channels.map(channel => s"\t\t$channel").mkString("\n"))
      println(s"\tCause: Unknown")
      parent ! Disconnected(this, Some(new Exception("Disconnected from twitch.")))
    case Failure(ex) =>
      println(s"TMI4s Channel Connection Disconnected:")
      println(s"\tConnection: $name")
      println(s"\tDropped Channels:")
      println(channels.map(channel => s"\t\t$channel").mkString("\n"))
      println(s"\tCause: ${ex.getMessage}")
      parent ! Disconnected(this, Some(new Exception("Disconnected from twitch.", ex)))
  }
  connected.onComplete {
    case Success(connection) =>
      println(s"TMI4s Channel Connection Connected:")
      println(s"\tConnection: $name")
      println(s"\tConnection Details:")
      println(s"\t\tLocal Address: ${connection.localAddress.getAddress.getCanonicalHostName}:${connection.localAddress.getPort}")
      println(s"\t\tRemote Address: ${connection.remoteAddress}:${connection.remoteAddress}")
    case Failure(ex) =>
      println(s"TMI4s Channel Connection Failed to Connect:")
      println(s"\tConnection: $name")
      println(s"\tDropped Channels:")
      println(channels.map(channel => s"\t\t$channel").mkString("\n"))
      parent ! Disconnected(this, Some(new Exception("Could not connect to twitch.", ex)))
  }

  def join(msg: ToTwitch[Join]): QueueOfferResult = {
    val res = Await.result(joins.offer(msg), Duration.Inf)
    _channels.add(msg.msg.channel)
    res
  }
  def part(msg: ToTwitch[Part]): QueueOfferResult = {
    val res = Await.result(parts.offer(msg), Duration.Inf)
    _channels.add(msg.msg.channel)
    res
  }
  def channels: Set[String] = _channels.toSet
  def idle: Boolean = _channels.isEmpty
  def hasRoom: Boolean = _channels.size < num
  def shutdown(): Unit = killSwitch.shutdown()
}

object ChannelConnection {
  case class Disconnected(target: ChannelConnection, ex: Option[Throwable])
}
