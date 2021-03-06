package lorstats

import cats.data.NonEmptyList
import cats.effect._
import cats.syntax.all._
import dissonance.data.{Color, Embed}
import dissonance.Discord
import java.time.OffsetDateTime
import lorstats.LorApiClient.GameInfo
import lorstats.model.{Card, LatestMatch}
import scala.concurrent.duration._

class GameplayNotifier(riotToken: String, discord: Discord, db: DB, cards: NonEmptyList[Card], blocker: Blocker)(implicit
    c: Concurrent[IO],
    t: Timer[IO]
) {
  private val lorClient      = new LorApiClient(discord.httpClient, riotToken)
  private val memesChannelId = 609120979989299210L
  private val matchRenderer  = new MatchRenderer(cards)
  private val gamesToNotify  = List("Ranked", "Normal", "StandardGauntlet")

  def notifyNewGames = {
    val newMatches = db
      .latestMatches(512)
      .evalMap(getNewerMatch(lorClient))
      .unNone
      .evalTap(db.setLatestMatch)
      .evalMap(lorClient.getGameInfo)
      .filter(a => gamesToNotify.contains(a.game.info.gameType))

    val notify = newMatches.evalMap(formatMatch)

    (notify ++ fs2.Stream.sleep_(2.minutes)).repeat
  }

  private def formatMatch(game: GameInfo) = {
    val player = game.game.info.players.find(_.puuid == game.account.puuid).getOrElse(throw new Exception(s"Couldn't find player"))
    val (winOrLose, color) = player.gameOutcome match {
      case "win"  => "WON"  -> Color.green
      case "loss" => "LOST" -> Color.red
      case "tie"  => "TIED" -> Color.blue
    }
    val ranked   = if (game.game.info.gameType == "Ranked") "ranked " else ""
    val opponent = game.game.info.players.find(_.puuid != game.account.puuid)

    val embed = Embed.make
      .withTitle(s"${game.account.gameName} $winOrLose a ${ranked}LoR match!")
      .withTimestamp(OffsetDateTime.parse(game.game.info.gameStartTimeUtc))
      .withDescription(
        "[" + game.account.gameName + "'s Deck](https://lor.mobalytics.gg/decks/code/" + player.deckCode + ")" + opponent
          .map(op => "\n[" + game.opponent.get.gameName + "'s Deck](https://lor.mobalytics.gg/decks/code/" + op.deckCode + ")")
          .orEmpty
      )
      .withColor(color)

    matchRenderer.renderMatch(game).use { file =>
      discord.client.sendEmbedWithFileImage(embed, file, memesChannelId, blocker).void
    }
  }

  private def getNewerMatch(lorClient: LorApiClient): LatestMatch => IO[Option[LatestMatch]] = { case LatestMatch(puuid, lastMatchId) =>
    for {
      matches   <- lorClient.getMatches(puuid)
      lastMatch <- matches.headOption.liftTo[IO](new Exception("No recent matches"))
    } yield if (lastMatch != lastMatchId) Some(LatestMatch(puuid, lastMatch)) else None
  }
}
