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
	"com.github.nscala-time" %% "nscala-time" % "1.0.0"
)

lazy val root = (project in file(".")).enablePlugins(PlayScala)
