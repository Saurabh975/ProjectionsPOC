ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "ProjectionsPOC"
  )

val AkkaVersion = "2.8.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-persistence-typed" % AkkaVersion,
  "com.lightbend.akka" %% "akka-projection-cassandra" % "1.4.2",
  "com.lightbend.akka" %% "akka-projection-core" % "1.4.2",
  "com.typesafe.akka" %% "akka-persistence-query" % AkkaVersion,
  "com.lightbend.akka" %% "akka-projection-eventsourced" % "1.4.2",
  "com.typesafe.akka" %% "akka-cluster-sharding-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-persistence-cassandra" % "1.1.1",
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-cluster-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-serialization-jackson" % AkkaVersion,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
  "org.scalatest" %% "scalatest" % "3.2.10" % Test,
  "org.slf4j" % "slf4j-api" % "1.7.32", // don't upgrade it
  "ch.qos.logback" % "logback-classic" % "1.2.6",
  "com.esri.geometry" % "esri-geometry-api" % "2.2.4",
//  "org.apache.tinkerpop" % "gremlin-core" % "3.6.2",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
  "com.typesafe.akka" %% "akka-serialization-jackson" % AkkaVersion,
)
