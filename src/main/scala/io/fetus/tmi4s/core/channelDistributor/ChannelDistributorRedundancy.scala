package io.fetus.tmi4s.core.channelDistributor

import akka.Done
import akka.actor.{Actor, ActorRef, Props}
import akka.stream._
import akka.stream.scaladsl.{Broadcast, GraphDSL, Merge, MergePreferred, RunnableGraph, Sink, Source, SourceQueueWithComplete}
import io.fetus.tmi4s.models.irc.Message.{Dedupable, Join, Part, SendableMessage}
import io.fetus.tmi4s.models.irc.MessageContainer.{FromTwitch, ToTwitch}
import io.fetus.tmi4s.models.irc.{Authenticate, Message}

import scala.concurrent.Future
import scala.concurrent.duration._
import scalacache.Cache
import scalacache.guava.GuavaCache
import scalacache.modes.try_._

/**
  * Created on 2017-11-28.
  *
  * @author Sina
  */
class ChannelDistributorRedundancy(num: Int, per: FiniteDuration, auth: Authenticate) extends Actor {
  implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))
  val leftCache: Cache[Dedupable] = GuavaCache[Dedupable]
  val rightCache: Cache[Dedupable] = GuavaCache[Dedupable]

  val left: ActorRef = context.actorOf(ChannelDistributor.props(num, per, auth))
  val right: ActorRef = context.actorOf(ChannelDistributor.props(num, per, auth))

  val (joins, parts, leftCompleted, rightCompleted) = {
    val joins: Source[ToTwitch[Join], SourceQueueWithComplete[ToTwitch[Join]]] = Source.queue[ToTwitch[Join]](100000, OverflowStrategy.backpressure)
    val parts = Source.queue[ToTwitch[Part]](100000, OverflowStrategy.backpressure)
    val leftSink: Sink[ToTwitch[SendableMessage], Future[Done]] = Sink.foreach[ToTwitch[SendableMessage]](left ! _)
    val rightSink: Sink[ToTwitch[SendableMessage], Future[Done]] = Sink.foreach[ToTwitch[SendableMessage]](right ! _)
    val graph = GraphDSL.create(joins, parts, leftSink, rightSink)((_, _, _, _)) { implicit builder => (joins, parts, leftSink, rightSink) =>
      import GraphDSL.Implicits._
      val joinSplitter = builder.add(Broadcast[ToTwitch[Join]](2))
      val joinMerger = builder.add(MergePreferred[(Symbol, ToTwitch[Join])](1, eagerComplete = true))
      val partMerger = builder.add(Merge[(Symbol, ToTwitch[SendableMessage])](2))
      val messageSplitter = builder.add(Broadcast[(Symbol, ToTwitch[SendableMessage])](2))

      joins ~> joinSplitter.in

      joinSplitter.out(0).map(x => ('left, x)).buffer(1, OverflowStrategy.backpressure) ~> joinMerger.preferred
      joinSplitter.out(1).map(x => ('right, x)).buffer(100000, OverflowStrategy.backpressure).delay(1.second, DelayOverflowStrategy.backpressure) ~> joinMerger.in(0)

      joinMerger.out.throttle(num, per, num, ThrottleMode.shaping) ~> partMerger.in(0)
      parts.out.flatMapConcat(part => Source(List(('left, part), ('right, part)))) ~> partMerger.in(1)
      partMerger.out ~> messageSplitter.in
      messageSplitter.out(0).filter(_._1 == 'left).map(t => {println(t);t}).map(_._2) ~> leftSink
      messageSplitter.out(1).filter(_._1 == 'right).map(t => {println(t);t}).map(_._2) ~> rightSink

      ClosedShape
    }
    RunnableGraph.fromGraph(graph).run()
  }
  // TODO: Handle potential stream ends.
  //  leftCompleted.onComplete()
  //  rightCompleted.onComplete()

  override def receive: Receive = {
    case ToTwitch(join: Join) =>
      joins.offer(ToTwitch(join))
    case ToTwitch(part: Part) =>
      parts.offer(ToTwitch(part))
    case FromTwitch(msg: Message with Dedupable) if sender() == left =>
      if (rightCache.get(msg).toOption.flatten.isEmpty) {
        leftCache.put(msg)(msg, ttl = Some(1.minute))
        context.parent ! FromTwitch(msg)
      } else {
        rightCache.remove(msg)
      }
    case FromTwitch(msg: Message with Dedupable) if sender() == right =>
      if (leftCache.get(msg).toOption.flatten.isEmpty) {
        rightCache.put(msg)(msg, ttl = Some(1.minute))
        context.parent ! FromTwitch(msg)
      } else {
        leftCache.remove(msg)
      }
    case FromTwitch(msg: Message) if sender() == left =>
      context.parent ! FromTwitch(msg)
  }
}

object ChannelDistributorRedundancy {
  def props(num: Int, per: FiniteDuration, auth: Authenticate): Props = Props(new ChannelDistributorRedundancy(num, per, auth))
}
