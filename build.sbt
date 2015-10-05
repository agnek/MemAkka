name := "Memakka"

version := "1.0"

scalaVersion := "2.11.7"

crossPaths := false

resolvers += "Akka Snapshots" at "http://repo.akka.io/snapshots/"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4-SNAPSHOT",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4",
  "org.specs2" %% "specs2-core" % "3.6.4" % "test",
  "net.spy" % "spymemcached" % "2.12.0" % "test"
)

scalacOptions in Test ++= Seq("-Yrangepos")

scalaSource in Compile := baseDirectory.value / "src"

scalaSource in Test := baseDirectory.value / "test-src"

mainClass in (Compile, run) := Some("Memakka")
