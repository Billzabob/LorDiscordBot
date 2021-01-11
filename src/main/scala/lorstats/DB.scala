package lorstats

import cats.effect._
import fs2.Stream
import lorstats.DB._
import lorstats.model.LatestMatch
import lorstats.model.LatestMatch._
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
}

object DB {
  type ConnectionPool = Resource[IO, Session[IO]]

  def pool(implicit c: Concurrent[IO], cs: ContextShift[IO]): Resource[IO, ConnectionPool] =
    Session.pooled(
      host = "localhost",
      port = 5432,
      user = "nhallstrom",
      database = "LoR",
      password = None,
      max = 10
    )

  val getLatestMatchesQuery =
    sql"""
      SELECT puuid, last_match
      FROM players
    """.query(latestMatchCodec)

  val setLatestMatchCommand =
    sql"""
      UPDATE players
      SET last_match = $lastMatchIdCodec
      WHERE puuid = $puuidCodec
    """.command.contramap[LatestMatch](lm => lm.lastMatchId ~ lm.puuid)
}
