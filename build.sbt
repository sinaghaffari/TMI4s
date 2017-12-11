name := "TMI4s"

version := "0.1"

scalaVersion := "2.12.4"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.6"
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.5.6"
libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.0.10"

libraryDependencies += "com.typesafe.play" % "play-json_2.12" % "2.6.7"
libraryDependencies += "com.typesafe.play" %% "play-ahc-ws-standalone" % "1.1.3"
libraryDependencies += "com.typesafe.play" %% "play-ws-standalone-json" % "1.1.3"

libraryDependencies += "com.google.inject" % "guice" % "4.1.0"
libraryDependencies += "com.github.cb372" % "scalacache-guava_2.12" % "0.21.0"
libraryDependencies += "com.github.cb372" % "scalacache-core_2.12" % "0.21.0"


libraryDependencies += "com.lihaoyi" % "fastparse_2.12" % "1.0.0"
        