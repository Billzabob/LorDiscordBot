import sbt._

object Dependencies {

  object Versions {
    val fs2        = "2.3.0"
    val atto       = "0.7.0"
    val cats       = "2.2.0"
    val circe      = "0.13.0"
    val skunk      = "0.0.21"
    val slf4j      = "1.7.30"
    val http4s     = "0.21.7"
    val deckmacia  = "1.0.4"
    val dissonance = "0.0.2"
    val catsEffect = "2.2.0"
  }

  object Compile {
    val fs2        = "co.fs2" %% "fs2-core" % Versions.fs2
    val cats       = "org.typelevel" %% "cats-core" % Versions.cats
    val atto       = "org.tpolecat" %% "atto-core" % Versions.atto
    val circe      = Seq("circe-core", "circe-parser", "circe-generic-extras").map("io.circe" %% _ % Versions.circe)
    val skunk      = "org.tpolecat" %% "skunk-core" % Versions.skunk
    val slf4j      = "org.slf4j" % "slf4j-nop" % Versions.slf4j
    val http4s     = "org.http4s" %% "http4s-circe" % Versions.http4s
    val deckmacia  = "com.github.billzabob" %% "fordeckmacia" % Versions.deckmacia
    val dissonance = "com.github.billzabob" %% "dissonance" % Versions.dissonance
    val catsEffect = "org.typelevel" %% "cats-effect" % Versions.catsEffect
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
    catsEffect
  ) ++ circe
}
