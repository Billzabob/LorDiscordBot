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

class GameplayNotifier(riotToken: String, discord: Discord, pool: DB.ConnectionPool, cards: NonEmptyList[Card], blocker: Blocker)(implicit
    c: Concurrent[IO],
    t: Timer[IO],
    cs: ContextShift[IO]
) {
  private val db             = new DB(pool)
  private val lorClient      = new LorApiClient(discord.httpClient, riotToken)
  private val memesChannelId = 609120979989299210L
  private val testChannel    = 689701123967156423L
  private val matchRenderer  = new MatchRenderer(cards)
  private val gamesToNotify  = List("Ranked", "Normal", "StandardGauntlet")

  def notifyNewGames = {
    val newMatches = db
      .latestMatches(512)
      .evalMap(getNewerMatch(lorClient))
      .unNone
      .evalTap(latestMatch => IO(println(s"Latest match: $latestMatch")))
      .evalTap(db.setLatestMatch)
      .evalMap(lorClient.getGameInfo)
      .evalTap(a => IO(println("Game type: '" + a.game.info.gameType + "'")))
      .filter(a => gamesToNotify.contains(a.game.info.gameType))

    val notify = newMatches.evalMap(formatMatch)

    (notify ++ fs2.Stream.sleep_(2.minutes)).repeat
  }

  private def formatMatch(game: GameInfo) = {
    val player    = game.game.info.players.find(_.puuid == game.account.puuid).getOrElse(throw new Exception(s"Couldn't find player"))
    val winOrLose = if (player.gameOutcome == "win") "WON" else "LOST"
    val ranked    = if (game.game.info.gameType == "Ranked") "ranked " else ""
    val opponent  = game.game.info.players.find(_.puuid != game.account.puuid)

    val embed = Embed.make
      .withTitle(s"${game.account.gameName} $winOrLose a ${ranked}LoR match!")
      .withTimestamp(OffsetDateTime.parse(game.game.info.gameStartTimeUtc))
      .withDescription(
        "[" + game.account.gameName + "'s Deck](https://lor.mobalytics.gg/decks/code/" + player.deckCode + ")" + opponent
          .map(op => "\n[" + game.opponent.get.gameName + "'s Deck](https://lor.mobalytics.gg/decks/code/" + op.deckCode + ")")
          .orEmpty
      )
      .withColor(if (player.gameOutcome == "win") Color.green else Color.red)

    matchRenderer.renderMatch(game).use { file =>
      List(testChannel, memesChannelId).parTraverse(channel => discord.client.sendEmbedWithFileImage(embed, file, channel, blocker)).void
    }
  }

  private def getNewerMatch(lorClient: LorApiClient): LatestMatch => IO[Option[LatestMatch]] = { case LatestMatch(puuid, lastMatchId) =>
    for {
      matches   <- lorClient.getMatches(puuid)
      lastMatch <- matches.headOption.liftTo[IO](new Exception("No recent matches"))
    } yield if (lastMatch != lastMatchId) Some(LatestMatch(puuid, lastMatch)) else None
  }
}
