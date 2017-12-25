package io.fetus.tmi4s.core.channelDistributor

import akka.Done
import akka.actor.{Actor, ActorRef, OneForOneStrategy, Props, SupervisorStrategy}
import akka.stream.scaladsl.{Sink, Source, SourceQueueWithComplete}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, OverflowStrategy, ThrottleMode}
import com.typesafe.config.Config
import io.fetus.tmi4s.models.irc.Message._
import io.fetus.tmi4s.models.irc.MessageContainer.{FromTwitch, ToTwitch}
import io.fetus.tmi4s.models.irc.{Authenticate, Message}

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class ChannelDistributorManager(num: Int, per: FiniteDuration, redundant: Boolean, auth: Authenticate, config: Config) extends Actor {
  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case ex => SupervisorStrategy.Escalate
  }
  implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))
  val forwardableOnce = mutable.Set.empty[Class[_ <: Message]]
  val channels = mutable.Set.empty[String]
  override def receive: Receive = {
    if (redundant) {
      val distributor: ActorRef = context.actorOf(ChannelDistributorRedundancy.props(num, per, auth, config))

      {
        case ToTwitch(join: Join) =>
          distributor ! ToTwitch(join)
        case ToTwitch(part: Part) =>
          distributor ! ToTwitch(part)
        case FromTwitch(msg: Message with Forwardable) =>
          context.parent ! FromTwitch(msg)
        case FromTwitch(msg: Message with ForwardableOnce) if forwardableOnce.add(msg.getClass) =>
          context.parent ! FromTwitch(msg)
      }
    } else {
      val distributor: ActorRef = context.actorOf(ChannelDistributor.props(num, per, auth, config))
      val joinSource: Source[ToTwitch[Join], SourceQueueWithComplete[ToTwitch[Join]]] = Source.queue[ToTwitch[Join]](100000, OverflowStrategy.backpressure).throttle(num, per, num, ThrottleMode.shaping)
      val joinSink: Sink[ToTwitch[Join], Future[Done]] = Sink.foreach[ToTwitch[Join]](distributor ! _)
      val joins: SourceQueueWithComplete[ToTwitch[Join]] = joinSource.to(joinSink).run()

      // TODO: Handle potential stream ends.
      // completed.onComplete(_ => channels.foreachcontext.stop(self))

      {
        case ToTwitch(join: Join) =>
          channels.add(join.channel)
          joins.offer(ToTwitch(join))
        case ToTwitch(part: Part) =>
          channels.remove(part.channel)
          distributor ! ToTwitch(part)
        case FromTwitch(msg: Message with Forwardable) =>
          context.parent ! FromTwitch(msg)
        case FromTwitch(msg: Message with ForwardableOnce) if forwardableOnce.add(msg.getClass) =>
          context.parent ! FromTwitch(msg)
      }
    }
  }
}

object ChannelDistributorManager {
  def props(num: Int, per: FiniteDuration, redundant: Boolean, auth: Authenticate, config: Config) = Props(new ChannelDistributorManager(num, per, redundant, auth, config))
}