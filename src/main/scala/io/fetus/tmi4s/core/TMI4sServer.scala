package io.fetus.tmi4s.core

import akka.{Done, NotUsed}
import akka.actor.{Actor, Props}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source, Tcp}
import akka.stream.scaladsl.Tcp.IncomingConnection
import com.typesafe.config.Config
import io.fetus.tmi4s.models.irc.Message
import io.fetus.tmi4s.models.irc.Message.SendableMessage
import io.fetus.tmi4s.models.irc.MessageContainer.{FromTwitch, ToTwitch}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class TMI4sServer(config: Config) extends Actor {
  implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))
  implicit val ec: ExecutionContext = context.dispatcher

  private val connections: Source[Tcp.IncomingConnection, Future[Tcp.ServerBinding]] = Tcp()(context.system).bind("0.0.0.0", 6667)
  private val handleConnection: Sink[IncomingConnection, Future[Done]] = Sink.foreach[IncomingConnection] { client =>
    import io.fetus.tmi4s.util.Flows._
    println(s"TMI4s Received Connection:")
    println(s"\tLocal Address: ${client.localAddress.getAddress.getCanonicalHostName}:${client.localAddress.getPort}")
    println(s"\tRemote Address: ${client.remoteAddress.getAddress.getCanonicalHostName}:${client.remoteAddress.getPort}")
    val connection: Flow[FromTwitch[Message], ToTwitch[SendableMessage], NotUsed] = messageToByteString.via(client.flow).via(byteStringToSendableMessage)
    context.actorOf(TMI4sClient.props(config, connection, client.localAddress, client.remoteAddress), name = s"${client.remoteAddress.getAddress.getCanonicalHostName}:${client.remoteAddress.getPort}")
  }
  val (server, done) = connections.toMat(handleConnection)(Keep.both).run()

  server.onComplete {
    case Success(serverBinding) =>
      println(s"TMI4s Started:")
      println(s"\tWelcome!")
      println(s"\tListening on port: ${serverBinding.localAddress.getPort}")
    case Failure(ex) =>
      println(s"TMI4s Could Not Start:")
      println(s"\tCause: ${ex.getMessage}")
      System.exit(1)
  }

  override def receive: Receive = {
    case _ =>
  }
}

object TMI4sServer {
  def props(config: Config) = Props(new TMI4sServer(config))
}
