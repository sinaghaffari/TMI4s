package io.fetus.tmi4s.util

import akka.actor.{ActorSystem, Cancellable}
import akka.stream.scaladsl.Tcp.OutgoingConnection
import akka.stream.scaladsl.{Flow, Framing, GraphDSL, Sink, Source, Tcp}
import akka.stream.{FlowShape, Graph, SinkShape}
import akka.util.ByteString
import akka.{Done, NotUsed}
import fastparse.core.Parsed
import io.fetus.tmi4s.models.irc.Message
import io.fetus.tmi4s.models.irc.Message.{MultiMessage, MultiMessageEnd, MultiMessagePart, Ping, Pong, SendableMessage}
import io.fetus.tmi4s.models.irc.MessageContainer.{FromTwitch, ToTwitch}
import io.fetus.tmi4s.parsing.{FromTwitchMessageParser, ToTwitchMessageParser}
import org.joda.time.{DateTime, Seconds}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

object Flows {
  val sendableMessageToByteString: Flow[ToTwitch[SendableMessage], ByteString, NotUsed] = Flow[ToTwitch[SendableMessage]].map(_.msg.send)
  val messageToByteString: Flow[FromTwitch[Message], ByteString, NotUsed] = Flow[FromTwitch[Message]].map(_.msg.forward)
  val byteStringToMessage: Flow[ByteString, FromTwitch[Message], NotUsed] = Flow.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val byteStringToSingleMessage: FlowShape[ByteString, FromTwitch[Message]] = builder.add {
        Framing.delimiter(
          ByteString("\r\n"),
          maximumFrameLength = 32768,
          allowTruncation = true
        ).map(_.utf8String).map(t => (t, FromTwitchMessageParser.message.parse(t))).flatMapConcat {
          case (msg, Parsed.Success(s, _)) => Source.single(s)
          case (msg, Parsed.Failure(a, b, c)) =>
            println(s"TMI4s Could Not Parse IRC Message: Message was ignored.")
            println(s"\tMessage:")
            println(s"\t\t$msg")
            println(s"\t$a")
            println(s"\t$b")
            println(s"\t$c")
            Source.empty
          case _ => Source.empty
        }.map(FromTwitch.apply)
      }
      val multiMessageAssembler: FlowShape[FromTwitch[Message], FromTwitch[Message]] = builder.add {
        Flow[FromTwitch[Message]].map(_.msg).statefulMapConcat { () =>
          var messages = List.empty[Message with MultiMessagePart]
          val decider: (Message => List[Message]) = {
            case msg: Message with MultiMessagePart with MultiMessageEnd =>
              messages = messages :+ msg
              val multiMessage = MultiMessage.fromList(messages).toList
              messages = List.empty
              multiMessage
            case msg: Message with MultiMessagePart =>
              messages = messages :+ msg
              List.empty[Message]
            case msg: Message => List(msg)
          }
          decider
        }.map(FromTwitch.apply)
      }
      byteStringToSingleMessage.out ~> multiMessageAssembler.in
      FlowShape(byteStringToSingleMessage.in, multiMessageAssembler.out)
    }
  )
  val byteStringToSendableMessage: Flow[ByteString, ToTwitch[SendableMessage], NotUsed] = {
    Framing.delimiter(
      ByteString("\r\n"),
      maximumFrameLength = 32768,
      allowTruncation = true
    ).map(_.utf8String).map(t => (t, ToTwitchMessageParser.message.parse(t))).flatMapConcat {
      case (msg, Parsed.Success(s, _)) => Source.single(s)
      case (msg, Parsed.Failure(a, b, c)) =>
        println(s"TMI4s Could Not Parse IRC Message: Message was ignored.")
        println(s"\tMessage:")
        println(s"\t\t$msg")
        println(s"\t$a")
        println(s"\t$b")
        println(s"\t$c")
        Source.empty
      case _ => Source.empty
    }.map(ToTwitch.apply)
  }

  def filterType[T : ClassTag]: Flow[Message, T, NotUsed] = Flow[Message].filter {
    case _: T => true
    case _ => false
  }.map(_.asInstanceOf[T])
  def filterMessages[T <: Message](pf: (T => Boolean)): Flow[T, T, NotUsed] = Flow[T].filter(pf)

  val pingToPongToTwitch: Flow[Ping.type, ToTwitch[Pong.type], NotUsed] = Flow[Ping.type].map(_ => ToTwitch(Pong))
  val pingToPongFromTwitch: Flow[Ping.type, FromTwitch[Pong.type], NotUsed] = Flow[Ping.type].map(_ => FromTwitch(Pong))

  def pongChecker(check: FiniteDuration, expect: FiniteDuration)(implicit ec: ExecutionContext): Graph[SinkShape[Pong.type], (Future[Done], Cancellable)] = {
    var lastPong: DateTime = DateTime.now
    val pongSink = Sink.foreach[Pong.type](_ => lastPong = DateTime.now)
    val checkEmitter = Source.tick[Symbol](0.seconds, check, 'check)

    GraphDSL.create(pongSink, checkEmitter)((_, _)) { implicit builder => (pongSink, checkEmitter) =>
      import GraphDSL.Implicits._
      val canceller = builder.add {
        Sink.foreach[(Future[Done], Cancellable)] {
          case (done, cancel) => done.onComplete { _ =>
            cancel.cancel()
          }
        }
      }
      val checkSink: SinkShape[Symbol] = builder.add{
        Sink.foreach[Symbol] {
          case 'check if Seconds.secondsBetween(lastPong, DateTime.now).getSeconds > expect.toSeconds =>
            throw new Exception("Server didn't respond to ping in time.")
          case _ =>
        }
      }
      builder.materializedValue ~> canceller.in
      checkEmitter.out ~> checkSink.in
      SinkShape(pongSink.in)
    }
  }

  def tcp(host: String, port: Int)(implicit system: ActorSystem): Flow[ToTwitch[SendableMessage], FromTwitch[Message], Future[OutgoingConnection]] = {
    val connection: Flow[ByteString, ByteString, Future[OutgoingConnection]] = Tcp()(system).outgoingConnection(host, port)
    Flow.fromGraph(GraphDSL.create(connection) { implicit builder => connection =>
      import GraphDSL.Implicits._

      val msg2Byte = builder.add(sendableMessageToByteString)
      val byte2Msg = builder.add(byteStringToMessage)

      msg2Byte ~> connection ~> byte2Msg
      FlowShape(msg2Byte.in, byte2Msg.out)
    })
  }
}
