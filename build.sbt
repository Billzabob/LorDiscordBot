import Dependencies._

Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val root = (project in file("."))
  .settings(
    name := "lorstats",
    scalaVersion := "2.13.5",
    version      := "1.0.0",
    organization := "com.github.billzabob",
    maintainer   := "cocolymoo@gmail.com",
    libraryDependencies ++= dependencies,
    scalacOptions += "-Ymacro-annotations",
    testFrameworks += new TestFramework("munit.Framework")
  )

addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.3" cross CrossVersion.full)
enablePlugins(JavaAppPackaging)
