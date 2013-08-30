scalaVersion := "2.10.2"

scalacOptions ++= Seq("-deprecation", "-feature")

scalaSource in Compile := file("src")

scalaSource in Test := file("test")

fork in Test := true

libraryDependencies ++= Seq(
  "play" % "play_2.10" % "2.1.3",
  "org.specs2" %% "specs2" % "1.14" % "test",
  "org.scalacheck" %% "scalacheck" % "1.10.1" % "test"
)

resolvers ++= Seq(
  "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
  "specs2 repository"   at "http://oss.sonatype.org/content/repositories/releases",
  "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
  "Sonatype Releases" at "http://oss.sonatype.org/content/repositories/releases"
)