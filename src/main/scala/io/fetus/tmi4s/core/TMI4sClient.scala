package io.fetus.tmi4s.core

import java.net.InetSocketAddress

import akka.NotUsed
import akka.actor.{Actor, ActorRef, Cancellable, Props}
import akka.stream.{ActorMaterializer, ClosedShape, KillSwitches, OverflowStrategy}
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, Partition, RunnableGraph, Sink, Source}
import com.typesafe.config.Config
import io.fetus.tmi4s.models.irc.Message
import io.fetus.tmi4s.models.irc.Message.{Ping, Pong, SendableMessage}
import io.fetus.tmi4s.models.irc.MessageContainer.{FromTwitch, ToTwitch}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class TMI4sClient(config: Config, connection: Flow[FromTwitch[Message], ToTwitch[SendableMessage], NotUsed], localAddress: InetSocketAddress, remoteAddress: InetSocketAddress) extends Actor {
  private implicit val materializer: ActorMaterializer = ActorMaterializer()(context.system)
  private implicit val ec: ExecutionContext = context.dispatcher

  private val tmi4s: ActorRef = context.actorOf(TMI4s.props(config))
  private val (messages, pingCanceller, killSwitch, completed) = {
    val messages = Source.queue[FromTwitch[Message]](100000, OverflowStrategy.backpressure)
    val pingEmitter: Source[FromTwitch[Message.Ping.type], Cancellable] = Source.tick(5.minutes, 5.minutes, FromTwitch(Ping))
    val tmi4sSink = Sink.foreach[ToTwitch[SendableMessage]](tmi4s ! _)
    val killSwitch = KillSwitches.single[FromTwitch[Message]]
    val graph = GraphDSL.create(messages, pingEmitter, killSwitch, tmi4sSink)((_, _, _, _)) { implicit builder => (messages, pingEmitter, killSwitch, tmi4sSink) =>
      import GraphDSL.Implicits._
      import io.fetus.tmi4s.util.Flows._

      val messagePartitioner = builder.add(Partition[ToTwitch[SendableMessage]](3, {
        case ToTwitch(Ping) => 0
        case ToTwitch(Pong) => 1
        case ToTwitch(_) => 2
      }))
      val messageMerger = builder.add(Merge[FromTwitch[Message]](3))

      messages ~> messageMerger.in(0)
      pingEmitter ~> messageMerger.in(1)

      messageMerger.out ~> killSwitch ~> connection ~> messagePartitioner.in

      messagePartitioner.out(0).map(_.msg) ~> filterType[Ping.type] ~> pingToPongFromTwitch ~> messageMerger.in(2)
      // TODO: How does throwing an exception get handled here? Make sure you shut down actor in response to this.
      messagePartitioner.out(1).map(_.msg) ~> filterType[Pong.type] ~> pongChecker(5.minutes, 10.minutes)
      messagePartitioner.out(2) ~> tmi4sSink

      ClosedShape
    }
    RunnableGraph.fromGraph(graph).run()
  }

  completed.onComplete(_ => context.stop(self))

  override def receive: Receive = {
    case FromTwitch(msg: Message) => messages.offer(FromTwitch(msg))
  }

  override def postStop(): Unit = {
    println("TMI4s Client Disconnected:")
    println(s"\tLocal Address: ${localAddress.getAddress.getCanonicalHostName}:${localAddress.getPort}")
    println(s"\tRemote Address: ${remoteAddress.getAddress.getCanonicalHostName}:${remoteAddress.getPort}")
    pingCanceller.cancel()
    killSwitch.shutdown()
    super.postStop()
  }
}

object TMI4sClient {
  def props(
             config: Config,
             connection: Flow[FromTwitch[Message], ToTwitch[SendableMessage], NotUsed],
             localAddress: InetSocketAddress,
             remoteAddress: InetSocketAddress,
           ) = Props(new TMI4sClient(config, connection, localAddress, remoteAddress))
}
