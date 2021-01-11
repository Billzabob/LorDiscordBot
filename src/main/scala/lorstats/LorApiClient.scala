package lorstats

import cats.data.NonEmptyList
import cats.effect.IO
import cats.syntax.all._
import lorstats.LorApiClient._
import lorstats.model._
import org.http4s._
import org.http4s.circe.CirceEntityDecoder
import org.http4s.client.Client
import org.http4s.implicits._

class LorApiClient(client: Client[IO], riotToken: String) extends CirceEntityDecoder {
  def getGameInfo: LatestMatch => IO[GameInfo] = { case LatestMatch(puuid, lastMatchId) =>
    for {
      game            <- riotApiRequest[Match](s"lor/match/v1/matches/$lastMatchId")
      account         <- getAccount(puuid)
      opponent         = game.info.players.find(_.puuid != puuid)
      opponentAccount <- opponent.traverse(p => getAccount(p.puuid))
    } yield GameInfo(account, opponentAccount, game)
  }

  def getAccount(puuid: String) =
    riotApiRequest[Account](s"riot/account/v1/accounts/by-puuid/$puuid")

  def getMatches(puuid: String) =
    riotApiRequest[List[String]](s"lor/match/v1/matches/by-puuid/$puuid/ids")

  def getCards: IO[NonEmptyList[Card]] = {
    val sets = NonEmptyList.of("set1", "set2", "set3")
    sets.flatTraverse { set =>
      client.expect[NonEmptyList[Card]](Request[IO](uri = uri"https://dd.b.pvp.net".addPath(s"latest/$set/en_us/data/$set-en_us.json")))
    }
  }

  private def riotApiRequest[A](path: String)(implicit d: EntityDecoder[IO, A]): IO[A] =
    client
      .expect[A](
        Request[IO](
          uri = uri"https://americas.api.riotgames.com".addPath(path),
          headers = Headers.of(Header("X-Riot-Token", riotToken))
        )
      )
      .handleErrorWith(e => IO(println(s"Error getting from $path:\n$e")) *> IO.raiseError(e))
}

object LorApiClient {
  case class GameInfo(account: Account, opponent: Option[Account], game: Match)
}
