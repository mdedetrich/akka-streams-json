name := "akka-streams-json"

val currentScalaVersion = "2.12.6"
val scala211Version     = "2.11.11"
val circeVersion        = "0.9.3"
val akkaVersion         = "2.5.14"
val akkaHttpVersion     = "10.1.3"
val jawnVersion         = "0.11.1"      // Note: updating to 0.12.1 breaks 'sbt test'
val scalaTestVersion    = "3.0.5"

scalaVersion in ThisBuild := currentScalaVersion
crossScalaVersions in ThisBuild := Seq(currentScalaVersion, scala211Version)
organization in ThisBuild := "org.mdedetrich"

lazy val streamJson = project.in(file("stream-json")) settings (
  name := "akka-stream-json",
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "org.spire-math"    %% "jawn-parser" % jawnVersion
  )
)

lazy val httpJson = project.in(file("http-json")) settings (
  name := "akka-http-json",
  libraryDependencies ++= Seq("com.typesafe.akka" %% "akka-http" % akkaHttpVersion % Provided)
) dependsOn streamJson

lazy val streamCirce = project.in(file("support") / "stream-circe") settings (
  name := "akka-stream-circe",
  libraryDependencies ++= Seq("com.typesafe.akka" %% "akka-stream" % akkaVersion % Provided,
                              "io.circe"          %% "circe-jawn"  % circeVersion)
) dependsOn streamJson

lazy val httpCirce = project.in(file("support") / "http-circe") settings (
  name := "akka-http-circe",
  libraryDependencies ++= Seq("com.typesafe.akka" %% "akka-http" % akkaHttpVersion % Provided)
) dependsOn (streamCirce, httpJson)

lazy val parent = project in file(".") dependsOn (httpJson, httpCirce) aggregate (streamJson, httpJson, streamCirce, httpCirce, tests) settings (
  skip in publish := true
)

lazy val tests = project.in(file("tests")) dependsOn (streamJson, httpJson, streamCirce, httpCirce) settings (
  libraryDependencies ++=
    List(
      "com.typesafe.akka" %% "akka-http"     % akkaHttpVersion  % Test,
      "org.scalatest"     %% "scalatest"     % scalaTestVersion % Test,
      "io.circe"          %% "circe-generic" % circeVersion     % "test"
    ),
  skip in publish := true
)

scalacOptions in ThisBuild ++= Seq(
  "-target:jvm-1.8",
  "-encoding",
  "UTF-8",
  "-deprecation", // warning and location for usages of deprecated APIs
  "-feature", // warning and location for usages of features that should be imported explicitly
  "-unchecked", // additional warnings where generated code depends on assumptions
  "-Xlint", // recommended additional warnings
  "-Xcheckinit", // runtime error when a val is not initialized due to trait hierarchies (instead of NPE somewhere else)
  "-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver
  "-Ywarn-value-discard", // Warn when non-Unit expression results are unused
  "-Ywarn-inaccessible",
  "-Ywarn-dead-code",
  "-language:postfixOps"
)

Defaults.itSettings

configs(IntegrationTest)

homepage in ThisBuild := Some(url("https://github.com/mdedetrich/akka-streams-json"))

scmInfo in ThisBuild := Some(
  ScmInfo(url("https://github.com/mdedetrich/akka-streams-json"), "git@github.com:mdedetrich/akka-streams-json.git"))

developers in ThisBuild := List(
  Developer("knutwalker", "Paul Horn", "", url("https://github.com/knutwalker/")),
  Developer("mdedetrich", "Matthew de Detrich", "mdedetrich@gmail.com", url("https://github.com/mdedetrich"))
)

licenses in ThisBuild += ("Apache-2.0", url("https://opensource.org/licenses/Apache-2.0"))

publishMavenStyle in ThisBuild := true

publishTo in ThisBuild := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test in ThisBuild := false

pomIncludeRepository in ThisBuild := (_ => false)

import ReleaseTransformations._
releaseCrossBuild := true
releasePublishArtifactsAction := PgpKeys.publishSigned.value // Use publishSigned in publishArtifacts step
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  publishArtifacts,
  setNextVersion,
  commitNextVersion,
  releaseStepCommand("sonatypeReleaseAll"),
  pushChanges
)

val flagsFor11 = Seq(
  "-Xlint:_",
  "-Yconst-opt",
  "-Ywarn-infer-any",
  "-Yclosure-elim",
  "-Ydead-code"
)

val flagsFor12 = Seq(
  "-Xlint:_",
  "-Ywarn-infer-any",
  "-opt-inline-from:<sources>"
)

scalacOptions in ThisBuild ++= {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, n)) if n >= 12 =>
      flagsFor12
    case Some((2, n)) if n == 11 =>
      flagsFor11
  }
}

parallelExecution in IntegrationTest := false
