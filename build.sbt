name := "FifaScoreKeeperGraphQL"

version := "1.0"

description := "FifaScoreKeeper GraphQL server with akka-http and sangria"

scalaVersion := "2.12.6"

scalacOptions ++= Seq("-deprecation", "-feature")

libraryDependencies ++= Seq(
  "org.sangria-graphql" %% "sangria" % "1.4.2",
  "org.sangria-graphql" %% "sangria-spray-json" % "1.0.1",
  "com.typesafe.akka" %% "akka-http" % "10.1.4",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.4",
  "com.typesafe.akka" %% "akka-actor" % "2.5.14",
  "com.typesafe.akka" %% "akka-stream" % "2.5.14",

  "com.typesafe.slick" %% "slick" % "3.2.3",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.2.3",
  "org.slf4j" % "slf4j-nop" % "1.7.25",
  "com.h2database" % "h2" % "1.4.197",

  "org.scalatest" %% "scalatest" % "3.0.5" % Test
)

Revolver.settings
