name := "ProjectSG"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.1"

resolvers ++= Seq(
    "RoundEights" at "http://maven.spikemark.net/roundeights",
    "jBCrypt Repository" at "http://repo1.maven.org/maven2/org/"
)

resolvers += "apache-snapshots-repo" at "https://repository.apache.org/snapshots/"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws
)     

libraryDependencies ++= Seq(
	"com.datastax.cassandra" % "cassandra-driver-core" % "2.1.0",
	"com.github.nscala-time" %% "nscala-time" % "1.0.0",
	"com.typesafe.play.plugins" %% "play-plugins-mailer" % "2.3.0",
  "com.kenshoo" %% "metrics-play" % "2.3.0_0.1.6"
)

libraryDependencies ++= Seq(
    "com.roundeights" %% "hasher" % "1.0.0",
    "org.mindrot" % "jbcrypt" % "0.3m",
    "org.apache.lucene" % "lucene-core" % "5.0.0",
    "org.apache.lucene" % "lucene-analyzers-common" % "5.0.0",
    "org.apache.lucene" % "lucene-queryparser" % "5.0.0",
    "org.apache.lucene" % "lucene-facet" % "5.0.0"
)


lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalacOptions += "-feature"

scalacOptions += "-language:implicitConversions"

scalacOptions += "-language:postfixOps"
