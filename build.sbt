name := "ProjectSG"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws
)     

libraryDependencies ++= Seq(
	"com.datastax.cassandra" % "cassandra-driver-core" % "2.0.0-rc2",
	"com.github.nscala-time" %% "nscala-time" % "1.0.0",
	"com.typesafe.play.plugins" %% "play-plugins-mailer" % "2.3.0"
)

resolvers ++= Seq(
    "RoundEights" at "http://maven.spikemark.net/roundeights",
    "jBCrypt Repository" at "http://repo1.maven.org/maven2/org/"
)

libraryDependencies ++= Seq(
    "com.roundeights" %% "hasher" % "1.0.0",
    "org.mindrot" % "jbcrypt" % "0.3m"
)


lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalacOptions += "-feature"

scalacOptions += "-language:implicitConversions"
