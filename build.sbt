name := """ltihub"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava,PlayEbean)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(  
  javaJdbc,
  "org.imsglobal" % "basiclti-util" % "1.1.2",
  "org.postgresql" % "postgresql" % "42.1.4"
)

