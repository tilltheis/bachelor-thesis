scalaVersion := "2.10.1"

scalacOptions ++= Seq("-deprecation", "-feature")

scalaSource in Compile := file("src")

scalaSource in Test := file("test")

libraryDependencies ++= Seq(
  "play" % "play_2.10" % "2.1.1",
  "org.specs2" %% "specs2" % "1.14" % "test"
)

resolvers ++= Seq(
  "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
  "specs2 repository"   at "http://oss.sonatype.org/content/repositories/releases"
)