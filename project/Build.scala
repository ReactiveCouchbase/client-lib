import sbt._
import Keys._

object ApplicationBuild extends Build {

  val appName         = "client-lib"
  val appVersion      = "1.0-SNAPSHOT"

  val local: Project.Initialize[Option[sbt.Resolver]] = version { (version: String) =>
    val localPublishRepo = "./repository"
    if(version.trim.endsWith("SNAPSHOT"))
      Some(Resolver.file("snapshots", new File(localPublishRepo + "/snapshots")))
    else Some(Resolver.file("releases", new File(localPublishRepo + "/releases")))
  }

  lazy val baseSettings = Defaults.defaultSettings ++ Seq(
    autoScalaLibrary := false,
    crossPaths := false
  )

  lazy val root = Project("root", base = file("."))
    .settings(baseSettings: _*)
    .settings(
      publishLocal := {},
      publish := {}
    ).aggregate(
      clientLib
    )

  lazy val clientLib = Project(appName, base = file("client-lib"))
    .settings(baseSettings: _*)
    .settings(
      resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
      libraryDependencies += "org.reactivecouchbase" % "common-lib" % "1.0-SNAPSHOT",
      libraryDependencies += "org.reactivecouchbase" % "concurrent-lib" % "1.0-SNAPSHOT",
      libraryDependencies += "com.google.guava" % "guava" % "19.0",
      libraryDependencies += "junit" % "junit" % "4.11" % "test",
      libraryDependencies += "com.novocode" % "junit-interface" % "0.9" % "test",
      organization := "org.reactivecouchbase",
      version := appVersion,
      publishTo <<= local,
      publishMavenStyle := true,
      publishArtifact in Test := false,
      pomIncludeRepository := { _ => false },
      publishArtifact in (Compile, packageDoc) := false,
      publishArtifact in packageDoc := false
    )
}
