package lorstats

import cats.effect._
import dissonance.data.Snowflake
import fs2.Stream
import lorstats.DB._
import lorstats.model.{LatestMatch, Quiz}
import lorstats.model.LatestMatch._
import lorstats.model.Quiz._
import natchez.Trace.Implicits.noop
import skunk._
import skunk.implicits._

class DB(pool: ConnectionPool) {

  def latestMatches(chunkSize: Int): Stream[IO, LatestMatch] =
    for {
      p <- Stream.resource(pool)
      q <- Stream.resource(p.prepare(getLatestMatchesQuery))
      m <- q.stream(Void, chunkSize)
    } yield m

  def setLatestMatch(latestMatch: LatestMatch): IO[Unit] =
    pool.flatMap(_.prepare(setLatestMatchCommand)).use(_.execute(latestMatch)).void

  def setCardQuizForChannel(quiz: Quiz): IO[Unit] =
    pool.flatMap(_.prepare(setCardQuizForChannelCommand)).use(_.execute(quiz)).void

  def currentQuizCard(channel: Snowflake): IO[Option[String]] =
    pool.flatMap(_.prepare(getCurrentQuizCardQuery)).use(_.option(channel))
}

object DB {
  type ConnectionPool = Resource[IO, Session[IO]]

  def pool(host: String, password: String)(implicit c: Concurrent[IO], cs: ContextShift[IO]): Resource[IO, ConnectionPool] =
    Session.pooled(
      host = host,
      port = 25060,
      user = "doadmin",
      database = "LoR",
      password = Some(password),
      max = 10,
      ssl = SSL.Trusted
    )

  val getLatestMatchesQuery =
    sql"""
      SELECT puuid, last_match
      FROM players
    """.query(latestMatchCodec)

  val getCurrentQuizCardQuery =
    sql"""
      SELECT card_name
      FROM quizzes
      WHERE channel = $channelCodec
    """.query(cardNameCodec)

  val setLatestMatchCommand =
    sql"""
      UPDATE players
      SET last_match = $lastMatchIdCodec
      WHERE puuid = $puuidCodec
    """.command.contramap[LatestMatch](lm => lm.lastMatchId ~ lm.puuid)

  val setCardQuizForChannelCommand =
    sql"""
      INSERT INTO quizzes (channel, card_name)
      VALUES ($channelCodec, $cardNameCodec)
      ON CONFLICT (channel)
      DO UPDATE SET card_name = EXCLUDED.card_name
    """.command.contramap[Quiz](q => q.channel ~ q.cardName)
}
