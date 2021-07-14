import com.jsuereth.sbtpgp.PgpKeys.publishSigned

name := "akka-streams-json"

val scala213Version = "2.13.6"
val scala212Version = "2.12.14"

val circeVersion     = "0.14.1"
val akkaVersion      = "2.6.15"
val akkaHttpVersion  = "10.2.4"
val jawnVersion      = "1.2.0"
val scalaTestVersion = "3.2.9"

ThisBuild / crossScalaVersions   := Seq(scala212Version, scala213Version)
ThisBuild / scalaVersion         := (ThisBuild / crossScalaVersions).value.last
ThisBuild / organization         := "org.mdedetrich"
ThisBuild / mimaFailOnNoPrevious := false // Set this to true when we start caring about binary compatibility
ThisBuild / versionScheme        := Some(VersionScheme.EarlySemVer)

lazy val streamJson = project
  .in(file("stream-json"))
  .settings(
    name := "akka-stream-json",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "org.typelevel"     %% "jawn-parser" % jawnVersion
    )
  )

lazy val httpJson = project
  .in(file("http-json"))
  .settings(
    name := "akka-http-json",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion % Provided
    )
  )
  .dependsOn(streamJson)

lazy val streamCirce = project
  .in(file("support") / "stream-circe")
  .settings(
    name := "akka-stream-circe",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-stream" % akkaVersion % Provided,
      "io.circe"          %% "circe-jawn"  % circeVersion
    )
  )
  .dependsOn(streamJson)

lazy val httpCirce = project
  .in(file("support") / "http-circe")
  .settings(
    name := "akka-http-circe",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion % Provided
    )
  )
  .dependsOn(streamCirce, httpJson)

lazy val parent = project
  .in(file("."))
  .dependsOn(httpJson, httpCirce)
  .aggregate(streamJson, httpJson, streamCirce, httpCirce, tests)
  .settings(
    publish / skip       := true,
    publishSigned / skip := true
  )

lazy val tests = project
  .in(file("tests"))
  .dependsOn(streamJson, httpJson, streamCirce, httpCirce)
  .settings(
    libraryDependencies ++=
      List(
        "com.typesafe.akka" %% "akka-http"     % akkaHttpVersion  % Test,
        "org.scalatest"     %% "scalatest"     % scalaTestVersion % Test,
        "io.circe"          %% "circe-generic" % circeVersion     % Test
      ),
    publish / skip       := true,
    publishSigned / skip := true
  )

ThisBuild / scalacOptions ++= Seq(
  "-target:jvm-1.8",
  "-encoding",
  "UTF-8",
  "-deprecation",         // warning and location for usages of deprecated APIs
  "-feature",             // warning and location for usages of features that should be imported explicitly
  "-unchecked",           // additional warnings where generated code depends on assumptions
  "-Xlint",               // recommended additional warnings
  "-Xcheckinit",          // runtime error when a val is not initialized due to trait hierarchies (instead of NPE somewhere else)
  "-Ywarn-value-discard", // Warn when non-Unit expression results are unused
  "-Ywarn-dead-code",
  "-language:postfixOps"
)

Defaults.itSettings

configs(IntegrationTest)

ThisBuild / homepage := Some(url("https://github.com/mdedetrich/akka-streams-json"))

ThisBuild / scmInfo := Some(
  ScmInfo(url("https://github.com/mdedetrich/akka-streams-json"), "git@github.com:mdedetrich/akka-streams-json.git")
)

ThisBuild / developers := List(
  Developer("knutwalker", "Paul Horn", "", url("https://github.com/knutwalker/")),
  Developer("mdedetrich", "Matthew de Detrich", "mdedetrich@gmail.com", url("https://github.com/mdedetrich"))
)

ThisBuild / licenses += ("Apache-2.0", url("https://opensource.org/licenses/Apache-2.0"))

ThisBuild / publishMavenStyle      := true
ThisBuild / publishTo              := sonatypePublishToBundle.value
ThisBuild / test / publishArtifact := false
ThisBuild / pomIncludeRepository   := (_ => false)

import ReleaseTransformations._

releaseCrossBuild             := true
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
  releaseStepCommand("sonatypeBundleRelease"),
  pushChanges
)

val flagsFor12 = Seq(
  "-Xlint:_",
  "-Ywarn-infer-any",
  "-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver
  "-Ywarn-inaccessible",
  "-Ywarn-infer-any",
  "-opt-inline-from:<sources>",
  "-opt:l:method"
)

val flagsFor13 = Seq(
  "-Xlint:_",
  "-opt-inline-from:<sources>",
  "-opt:l:method"
)

ThisBuild / scalacOptions ++= {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, n)) if n == 13 =>
      flagsFor13
    case Some((2, n)) if n == 12 =>
      flagsFor12
  }
}

IntegrationTest / parallelExecution := false

ThisBuild / githubWorkflowBuild := Seq(
  WorkflowStep.Sbt(List("mimaReportBinaryIssues"), name = Some("Report binary compatibility issues")),
  WorkflowStep.Sbt(List("clean", "coverage", "test"), name = Some("Build project"))
)

ThisBuild / githubWorkflowBuildPostamble ++= Seq(
  // See https://github.com/scoverage/sbt-coveralls#github-actions-integration
  WorkflowStep.Sbt(
    List("coverageReport", "coverageAggregate", "coveralls"),
    name = Some("Upload coverage data to Coveralls"),
    env = Map(
      "COVERALLS_REPO_TOKEN" -> "${{ secrets.GITHUB_TOKEN }}",
      "COVERALLS_FLAG_NAME"  -> "Scala ${{ matrix.scala }}"
    )
  )
)

// This is causing problems with env variables being passed in, see
// https://github.com/sbt/sbt/issues/6468
ThisBuild / githubWorkflowUseSbtThinClient := false

ThisBuild / githubWorkflowPublishTargetBranches := Seq()
