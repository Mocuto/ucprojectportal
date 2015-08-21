name := "ProjectSG"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.6"

val phantomVersion = "1.5.0"

libraryDependencies += "com.typesafe.play" %% "anorm" % "2.4.0"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws
)

libraryDependencies += specs2 % Test

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

lazy val openSAMLVersion = "2.6.0"

resolvers ++= Seq(
    "RoundEights" at "http://maven.spikemark.net/roundeights",
    "jBCrypt Repository" at "http://repo1.maven.org/maven2/org/"
)

libraryDependencies ++= Seq(
	"com.datastax.cassandra" % "cassandra-driver-core" % "2.1.0",
	"com.github.nscala-time" %% "nscala-time" % "1.0.0",
	"com.typesafe.play" %% "play-mailer" % "3.0.1",
  "com.kenshoo" %% "metrics-play" % "2.4.0_0.3.0",
  "com.websudos"  %% "phantom-dsl" % phantomVersion
)

resolvers ++= Seq(
    "RoundEights" at "http://maven.spikemark.net/roundeights",
    "jBCrypt Repository" at "http://repo1.maven.org/maven2/org/",
    "Typesafe repository snapshots" at "http://repo.typesafe.com/typesafe/snapshots/",
  "Typesafe repository releases" at "http://repo.typesafe.com/typesafe/releases/",
  "Sonatype repo"                    at "https://oss.sonatype.org/content/groups/scala-tools/",
  "Sonatype releases"                at "https://oss.sonatype.org/content/repositories/releases",
  "Sonatype snapshots"               at "https://oss.sonatype.org/content/repositories/snapshots",
  "Sonatype staging"                 at "http://oss.sonatype.org/content/repositories/staging",
  "Java.net Maven2 Repository"       at "http://download.java.net/maven/2/",
  "Twitter Repository"               at "http://maven.twttr.com",
  "Websudos releases"                at "http://maven.websudos.co.uk/ext-release-local"
)

libraryDependencies ++= Seq(
    "com.roundeights" %% "hasher" % "1.0.0",
    "org.mindrot" % "jbcrypt" % "0.3m",
    "org.apache.lucene" % "lucene-core" % "5.0.0",
    "com.websudos"  %% "phantom-dsl" % phantomVersion,
    "org.opensaml" % "opensaml" % openSAMLVersion,
    "org.apache.lucene" % "lucene-analyzers-common" % "5.0.0",
    "org.apache.lucene" % "lucene-queryparser" % "5.0.0",
    "org.apache.lucene" % "lucene-facet" % "5.0.0"
)

libraryDependencies += "com.github.marklister" %% "base64" % "v0.1"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalacOptions += "-feature"

scalacOptions += "-language:implicitConversions"

scalacOptions += "-language:postfixOps"
