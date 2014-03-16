name := "api"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  javaJdbc,
  javaEbean,
  cache,
  "postgresql" % "postgresql" % "9.0-901-1.jdbc4",
  "mysql" % "mysql-connector-java" % "5.1.18",
  jdbc
)

play.Project.playJavaSettings
