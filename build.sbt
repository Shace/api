name := "api"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  javaJdbc,
  javaEbean,
  cache,
  "postgresql" % "postgresql" % "9.1-901-1.jdbc4",
  "mysql" % "mysql-connector-java" % "5.1.18",
  "com.amazonaws" % "aws-java-sdk" % "1.7.5",
  "org.imgscalr" % "imgscalr-lib" % "4.2",
  "com.typesafe.play.plugins" %% "play-plugins-mailer" % "2.3.0",
  jdbc
)

lazy val root = (project in file(".")).enablePlugins(PlayJava)

instrumentSettings

CoverallsPlugin.coverallsSettings



