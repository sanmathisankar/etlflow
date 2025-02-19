import Dependencies._
import Versions._
import ScalaCompileOptions._

Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / version := EtlFlowVersion

lazy val commonSettings = Seq(
  scalaVersion               := Scala212,
  dependencyUpdatesFailBuild := true,
  dependencyUpdatesFilter -= moduleFilter(organization = "org.scala-lang"),
  scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 12)) => s2copts ++ s212copts
      case Some((2, 13)) => s2copts
      case Some((3, _))  => s3copts
      case _             => Seq()
    }
  },
  Test / parallelExecution := false,
  testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
)

lazy val coreSettings = Seq(
  name               := "etlflow-core",
  crossScalaVersions := AllScalaVersions,
  libraryDependencies ++= coreLibs ++ coreTestLibs
)

lazy val sparkSettings = Seq(
  name               := "etlflow-spark",
  crossScalaVersions := Scala2Versions,
  libraryDependencies ++= sparkLibs ++ coreTestLibs ++ dbTestLibs ++ sparkTestLibs
)

lazy val dbSettings = Seq(
  name               := "etlflow-db",
  crossScalaVersions := AllScalaVersions,
  libraryDependencies ++= dbLibs ++ coreTestLibs ++ dbTestLibs
)

lazy val httpSettings = Seq(
  name                       := "etlflow-http",
  crossScalaVersions         := AllScalaVersions,
  dependencyUpdatesFailBuild := false,
  libraryDependencies ++= httpLibs ++ coreTestLibs
)

lazy val redisSettings = Seq(
  name               := "etlflow-redis",
  crossScalaVersions := Scala2Versions,
  libraryDependencies ++= redisLibs ++ coreTestLibs
)

lazy val emailSettings = Seq(
  name               := "etlflow-email",
  crossScalaVersions := AllScalaVersions,
  libraryDependencies ++= emailLibs ++ coreTestLibs
)

lazy val awsSettings = Seq(
  name               := "etlflow-aws",
  crossScalaVersions := AllScalaVersions,
  libraryDependencies ++= awsLibs ++ coreTestLibs
)

lazy val gcpSettings = Seq(
  name               := "etlflow-gcp",
  crossScalaVersions := AllScalaVersions,
  libraryDependencies ++= gcpLibs ++ coreTestLibs
)

lazy val k8sSettings = Seq(
  name               := "etlflow-k8s",
  crossScalaVersions := AllScalaVersions,
  libraryDependencies ++= k8sLibs ++ coreTestLibs
)

lazy val etlflow = (project in file("."))
  .settings(
    crossScalaVersions := Nil, // crossScalaVersions must be set to Nil on the aggregating project
    publish / skip     := true
  )
  .aggregate(core, spark, db, http, redis, email, aws, gcp, k8s)

lazy val core = (project in file("modules/core"))
  .settings(commonSettings)
  .settings(coreSettings)

lazy val db = (project in file("modules/db"))
  .settings(commonSettings)
  .settings(dbSettings)
  .dependsOn(core)

lazy val spark = (project in file("modules/spark"))
  .settings(commonSettings)
  .settings(sparkSettings)
  .dependsOn(core)

lazy val http = (project in file("modules/http"))
  .settings(commonSettings)
  .settings(httpSettings)
  .dependsOn(core)

lazy val redis = (project in file("modules/redis"))
  .settings(commonSettings)
  .settings(redisSettings)
  .dependsOn(core)

lazy val email = (project in file("modules/email"))
  .settings(commonSettings)
  .settings(emailSettings)
  .dependsOn(core)

lazy val aws = (project in file("modules/aws"))
  .settings(commonSettings)
  .settings(awsSettings)
  .dependsOn(core)

lazy val gcp = (project in file("modules/gcp"))
  .settings(commonSettings)
  .settings(gcpSettings)
  .dependsOn(core)

lazy val k8s = (project in file("modules/k8s"))
  .settings(commonSettings)
  .settings(k8sSettings)
  .dependsOn(core)
