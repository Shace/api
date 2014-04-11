name := "api"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  javaJdbc,
  javaEbean,
  cache,
  "postgresql" % "postgresql" % "9.1-901-1.jdbc4",
  "mysql" % "mysql-connector-java" % "5.1.18",
  "com.amazonaws" % "aws-java-sdk" % "1.7.5",
  "org.imgscalr" % "imgscalr-lib" % "4.2",
  jdbc
)

play.Project.playJavaSettings

ScoverageSbtPlugin.instrumentSettings

CoverallsPlugin.coverallsSettings



