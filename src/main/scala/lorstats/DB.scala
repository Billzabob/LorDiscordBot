package lorstats

import cats.effect._
import dissonance.data.Snowflake
import fs2.Stream
import lorstats.DB._
import lorstats.model.{Guess, LatestMatch, Quiz}
import lorstats.model.Guess._
import lorstats.model.LatestMatch._
import lorstats.model.Quiz._
import natchez.Trace.Implicits.noop
import skunk._
import skunk.codec.all._
import skunk.implicits._

class DB(pool: ConnectionPool) {

  def latestMatches(chunkSize: Int): Stream[IO, LatestMatch] =
    for {
      p <- Stream.resource(pool)
      q <- Stream.resource(p.prepare(getLatestMatchesQuery))
      m <- q.stream(Void, chunkSize)
    } yield m

  def getGuessesForChannel(channel: Snowflake): Stream[IO, Guess] =
    for {
      p <- Stream.resource(pool)
      q <- Stream.resource(p.prepare(getGuessesForChannelQuery))
      g <- q.stream(channel, 10)
    } yield g

  def setLatestMatch(latestMatch: LatestMatch): IO[Unit] =
    pool.flatMap(_.prepare(setLatestMatchCommand)).use(_.execute(latestMatch)).void

  def setCardQuizForChannel(quiz: Quiz): IO[Unit] =
    pool.flatMap(_.prepare(setCardQuizForChannelCommand)).use(_.execute(quiz)).void

  def currentQuizCard(channel: Snowflake): IO[Option[String]] =
    pool.flatMap(_.prepare(getCurrentQuizCardQuery)).use(_.option(channel))

  def addGuessForPlayer(guess: Guess): IO[Unit] =
    pool.flatMap(_.prepare(addGuessForPlayerCommand)).use(_.execute(guess)).void

  def clearQuiz(channel: Snowflake): IO[Unit] =
    pool.flatMap(_.prepare(clearQuizCommand)).use(_.execute(channel)).void
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
      WHERE channel = $int8
    """.query(cardNameCodec)

  val getGuessesForChannelQuery =
    sql"""
      SELECT channel, user_id, answer
      FROM guesses, quizzes
      WHERE channel = $int8
    """.query(guessCodec)

  val setLatestMatchCommand =
    sql"""
      UPDATE players
      SET last_match = $lastMatchIdCodec
      WHERE puuid = $puuidCodec
    """.command.contramap[LatestMatch](lm => lm.lastMatchId ~ lm.puuid)

  val setCardQuizForChannelCommand =
    sql"""
      INSERT INTO quizzes (channel, card_name)
      VALUES ($int8, $cardNameCodec)
    """.command.contramap[Quiz](q => q.channel ~ q.cardName)

  val addGuessForPlayerCommand =
    sql"""
      INSERT INTO guesses (quiz, user_id, answer)
      SELECT id, $int8, $answerCodec
      FROM quizzes
      WHERE channel = $int8
    """.command.contramap[Guess](g => g.user ~ g.answer ~ g.channel)

  val clearQuizCommand =
    sql"""
      DELETE FROM quizzes
      WHERE channel = $int8
    """.command
}
