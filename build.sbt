name := "drivedog"

mainClass in Compile := Some("gui.Main")

version := "0.1"

scalaVersion := "2.11.7"

parallelExecution in Test := false

libraryDependencies ++= Seq(
 "com.google.api-client" % "google-api-client" % "1.20.0"
 ,"com.google.oauth-client" % "google-oauth-client-jetty" % "1.20.0"
 ,"com.google.apis" % "google-api-services-drive" % "v2-rev170-1.20.0"
 ,"org.scalatest" %% "scalatest" % "2.1.7" % "test"
 ,"com.typesafe.scala-logging" %% "scala-logging" % "3.1.0"
 ,"org.slf4j" % "slf4j-api" % "1.7.10"
 ,"ch.qos.logback" % "logback-classic" % "1.1.3"
 ,"ch.qos.logback" % "logback-core" % "1.1.3"
 ,"org.scala-lang.modules" % "scala-swing_2.11" % "1.0.1"
 ,"org.scala-lang" % "scala-actors" % "2.11.7"
)

