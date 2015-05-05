name := "exchange2"

version := "1.0"

scalaVersion := "2.11.6"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += Resolver.sonatypeRepo("public")

libraryDependencies += "org.specs2" %% "specs2" % "2.3.12" % "test"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.0.3"