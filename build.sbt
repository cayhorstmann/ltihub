name := """ltihub"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava,PlayEbean)

scalaVersion := "2.12.4"

libraryDependencies ++= Seq(
  guice,
  javaJdbc,
  "net.oauth.core" % "oauth-provider" % "20100527",
  "oauth.signpost" % "signpost-core" % "1.2.1.2",
  "org.imsglobal" % "basiclti-util" % "1.1.2", 
  "org.postgresql" % "postgresql" % "42.1.4"
)
