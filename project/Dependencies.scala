import sbt._

object Dependencies {

  object Versions {
    val fs2         = "2.3.0"
    val atto        = "0.9.1"
    val cats        = "2.4.2"
    val circe       = "0.13.0"
    val skunk       = "0.0.23"
    val slf4j       = "1.7.30"
    val http4s      = "0.21.7"
    val scrimage    = "4.0.17"
    val deckmacia   = "1.0.8+1-f2807078+20210225-1249-SNAPSHOT"
    val dissonance  = "0.0.6+68-8e9d1088-SNAPSHOT"
    val catsEffect  = "2.2.0"
    val commonsText = "1.9"
  }

  object Compile {
    val fs2         = "co.fs2" %% "fs2-core" % Versions.fs2
    val cats        = "org.typelevel" %% "cats-core" % Versions.cats
    val atto        = "org.tpolecat" %% "atto-core" % Versions.atto
    val circe       = Seq("circe-core", "circe-parser", "circe-generic-extras").map("io.circe" %% _ % Versions.circe)
    val skunk       = "org.tpolecat" %% "skunk-core" % Versions.skunk
    val slf4j       = "org.slf4j" % "slf4j-nop" % Versions.slf4j
    val http4s      = "org.http4s" %% "http4s-circe" % Versions.http4s
    val scrimage    = Seq("com.sksamuel.scrimage" % "scrimage-core", "com.sksamuel.scrimage" %% "scrimage-scala").map(_ % Versions.scrimage)
    val deckmacia   = "com.github.billzabob" %% "fordeckmacia" % Versions.deckmacia
    val dissonance  = "com.github.billzabob" %% "dissonance" % Versions.dissonance
    val catsEffect  = "org.typelevel" %% "cats-effect" % Versions.catsEffect
    val commonsText = "org.apache.commons" % "commons-text" % Versions.commonsText
  }

  import Compile._

  lazy val dependencies = Seq(
    fs2,
    atto,
    cats,
    skunk,
    slf4j,
    http4s,
    deckmacia,
    dissonance,
    catsEffect,
    commonsText
  ) ++ circe ++ scrimage
}
