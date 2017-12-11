package io.fetus.tmi4s.implicits

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.google.inject.name.Named
import com.google.inject.{AbstractModule, Provides, Singleton}
import com.typesafe.config.{Config, ConfigFactory}
import play.api.libs.ws.ahc.StandaloneAhcWSClient

import scala.concurrent.ExecutionContext
import scalacache.Cache
import scalacache.guava.GuavaCache

class ImplicitsModule extends AbstractModule {
  implicit val system: ActorSystem = ActorSystem()
  implicit val dispatcher: ExecutionContext = system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  val ws: StandaloneAhcWSClient = StandaloneAhcWSClient()

  override def configure(): Unit = {
    bind(classOf[ActorSystem]).toInstance(system)
    bind(classOf[ExecutionContext]).toInstance(dispatcher)
    bind(classOf[StandaloneAhcWSClient]).toInstance(ws)
    bind(classOf[ActorMaterializer]).toInstance(materializer)
  }

  @Provides @Singleton
  def provideConfig: Config = {
    ConfigFactory.load()
  }
}
