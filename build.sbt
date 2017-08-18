name := """ltihub"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava,PlayEbean)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  
  javaJdbc,
  cache,
  javaWs,
  javaJpa,
  // "mysql" % "mysql-connector-java" % "5.1.28",
  "org.postgresql" % "postgresql" % "42.1.4",
  "commons-io" % "commons-io" % "2.3",
  "org.jsoup" % "jsoup" % "1.7.2"
)

val appDependencies = Seq(
  "org.postgresql" % "postgresql" % "42.1.4"
)
