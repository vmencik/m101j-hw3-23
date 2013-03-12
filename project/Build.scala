import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "m101j-homework-3-23"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    "org.mongodb" %% "casbah" % "2.5.0",
    "se.radley" %% "play-plugins-salat" % "1.2",
    "org.apache.commons" % "commons-lang3" % "3.1"
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    resolvers += "OSS Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots"
    
  )

}
