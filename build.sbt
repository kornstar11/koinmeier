name := "exchange2"

version := "1.0"

scalaVersion := "2.11.6"

val akkaStreamVersion = "1.0-RC2"

val akkaVer = "2.3.10"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += Resolver.sonatypeRepo("public")

libraryDependencies += "org.specs2" %% "specs2" % "2.3.12" % "test"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.0.3"

libraryDependencies += "com.typesafe.akka" % "akka-stream-experimental_2.11" % akkaStreamVersion

libraryDependencies += "com.typesafe.akka" % "akka-http-core-experimental_2.11" % akkaStreamVersion

libraryDependencies += "com.typesafe.akka" % "akka-http-scala-experimental_2.11" % akkaStreamVersion

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.3.4"

libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % akkaVer

javacOptions += "-XX:+UnlockCommercialFeatures -XX:+FlightRecorder"
