package io.fetus.tmi4s.core.messageDistributor

import akka.actor.{Actor, Props}
import akka.stream._
import akka.stream.scaladsl.{Broadcast, Concat, GraphDSL, Merge, RunnableGraph, Sink, Source, SourceQueueWithComplete}
import akka.{Done, NotUsed}
import com.typesafe.config.Config
import io.fetus.tmi4s.core.messageDistributor.MessageConnection.Disconnected
import io.fetus.tmi4s.models.irc.Message.{Ping, PrivMsg, SendableMessage, UserState}
import io.fetus.tmi4s.models.irc.MessageContainer.{FromTwitch, ToTwitch}
import io.fetus.tmi4s.models.irc.{Authenticate, Message}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class MessageConnection(num: Int, per: FiniteDuration, auth: Authenticate, config: Config) extends Actor {
  private implicit val materializer: ActorMaterializer = ActorMaterializer()(context.system)
  private implicit val ec: ExecutionContext = context.dispatcher
  private val (messages, connected, killSwitch, completed) = {
    import io.fetus.tmi4s.util.Flows._
    val authSource: Source[ToTwitch[Message.SendableMessage], NotUsed] = Source(auth.caps.toList ++ List(auth.pass, auth.nick)).map(ToTwitch.apply)
    val messages: Source[ToTwitch[PrivMsg], SourceQueueWithComplete[ToTwitch[PrivMsg]]] = Source.queue[ToTwitch[PrivMsg]](100000, OverflowStrategy.backpressure).throttle(num, per, 1, ThrottleMode.shaping)
    val responder: Sink[FromTwitch[Message], Future[Done]] = Sink.foreach[FromTwitch[Message]](context.parent ! _)
    val killSwitch: Graph[FlowShape[ToTwitch[SendableMessage], ToTwitch[SendableMessage]], UniqueKillSwitch] = KillSwitches.single[ToTwitch[SendableMessage]]
    val graph = GraphDSL.create(messages, tcp("irc.chat.twitch.tv", 6667)(context.system), killSwitch, responder)((_, _, _, _)) { implicit builder => (messages, connection, killSwitch, responder) =>
      import GraphDSL.Implicits._

      val authenticator = builder.add(Concat[ToTwitch[SendableMessage]](2))
      val pongCombiner = builder.add(Merge[ToTwitch[SendableMessage]](2))
      val pingSplitter = builder.add(Broadcast[FromTwitch[Message]](2))

      authSource ~> authenticator.in(0)
      messages ~> authenticator.in(1)

      authenticator.out ~> pongCombiner.in(0)

      pongCombiner.out ~> killSwitch ~> connection ~> pingSplitter.in
      pingSplitter.out(0).map(_.msg) ~> filterType[Ping.type] ~> pingToPongToTwitch ~> pongCombiner.in(1)
      pingSplitter.out(1).filter(_.msg.isInstanceOf[UserState]) ~> responder

      ClosedShape
    }
    RunnableGraph.fromGraph(graph).run()
  }

  completed.onComplete {
    case Success(Done) =>
      println(s"TMI4s Message Connection Disconnected:")
      println(s"\tCause: Unknown")
      context.parent ! Disconnected(None)
    case Failure(ex) =>
      println(s"TMI4s Message Connection Disconnected:")
      println(s"\tCause: ${ex.getMessage}")
      context.parent ! Disconnected(Some(ex))
  }

  connected.onComplete {
    case Failure(ex) =>
      println(s"TMI4s Message Connection Failed to Connect:")
      println(s"\tCause: ${ex.getMessage}")
      context.parent ! Disconnected(Some(ex))
    case Success(_) =>
      println(s"TMI4s Message Connection Connected")
  }

  override def receive: Receive = {
    case ToTwitch(msg: PrivMsg) => messages.offer(ToTwitch(msg))
  }

  override def postStop(): Unit = {
    killSwitch.shutdown()
  }
}

object MessageConnection {
  case class Disconnected(ex: Option[Throwable])
  def props(num: Int, per: FiniteDuration, auth: Authenticate, config: Config) = Props(new MessageConnection(num, per, auth, config))
}
