name := "Memakka"

version := "1.0"

scalaVersion := "2.11.7"

crossPaths := false

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.3.0",
  "com.typesafe.akka" %% "akka-actor" % "2.4.0",
  "com.typesafe.akka" %% "akka-cluster" % "2.4.0",
  "com.typesafe.akka" %% "akka-cluster-sharding" % "2.4.0",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
  "org.specs2" %% "specs2-core" % "3.6.4" % "test",
  "net.spy" % "spymemcached" % "2.12.0" % "test"
)

scalacOptions in Test ++= Seq("-Yrangepos")

scalaSource in Compile := baseDirectory.value / "src"

scalaSource in Test := baseDirectory.value / "test-src"

mainClass in (Compile, run) := Some("Memakka")

resourceDirectory in Compile := baseDirectory.value / "resources"