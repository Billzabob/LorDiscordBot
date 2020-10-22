package lorstats

import cats.effect._
import cats.syntax.all._
import dissonance._
import dissonance.data._
import dissonance.data.events._
import fs2.Stream
import lorstats.DB
import lorstats.DB._
import lorstats.LorApiClient.GameInfo
import lorstats.model._
import org.http4s.client.Client
import org.http4s.client.middleware.{Retry, RetryPolicy}
import scala.concurrent.duration._

object Main extends IOApp {

  val memesChannelId = 609120979989299210L

  override def run(args: List[String]): IO[ExitCode] = {
    val discordToken = args(0)
    val riotToken    = args(1)
    (Discord.make(discordToken), pool, Blocker[IO]).tupled
      .use { case (discord, pool, blocker) =>
        val db           = new DB(pool)
        val eventsStream = discord.subscribe(Shard.singleton, Intent.GuildMessages)
        val lorClient    = new LorApiClient(withRetry(discord.httpClient), riotToken)

        val newMatches = db
          .latestMatches(512)
          .evalMap(getNewerMatch(lorClient))
          .unNone
          .evalTap(db.setLatestMatch)
          .evalMap(lorClient.getGameInfo)

        val report = newMatches.evalMap { case GameInfo(account, game) =>
          val player    = game.info.players.filter(_.puuid == account.puuid).head
          val winOrLose = if (player.gameOutcome == "win") "WON" else "LOST"
          val ranked    = if (game.info.gameType == "Ranked") "ranked " else ""
          val embed     = Embed.make.withTitle(s"${account.gameName} $winOrLose a ${ranked}LoR match!").addField(Field("deck code", player.deckCode, None))
          discord.client.sendEmbed(embed, memesChannelId).void
        }

        lorClient.getCards.flatMap { cards =>
          val commandRunner = new CommandRunner(discord.client, cards, blocker)
          Stream(
            eventsStream.evalMap(handleEvents(commandRunner)).drain,
            repeatWithDelay(report, 2.minutes).drain
          ).parJoinUnbounded.compile.drain
        }
      }
      .as(ExitCode.Success)
  }

  def repeatWithDelay[A](stream: Stream[IO, A], delay: FiniteDuration): Stream[IO, A] =
    stream.handleErrorWith(e => Stream.eval_(IO(println(e)))) ++ Stream.sleep_(delay) ++ repeatWithDelay(stream, delay)

  def getNewerMatch(lorClient: LorApiClient): LatestMatch => IO[Option[LatestMatch]] = { case LatestMatch(puuid, lastMatchId) =>
    for {
      matches   <- lorClient.getMatches(puuid)
      lastMatch <- matches.headOption.liftTo[IO](new Exception("No recent matches"))
    } yield if (lastMatch != lastMatchId) Some(LatestMatch(puuid, lastMatch)) else None
  }

  def handleEvents(commandRunner: CommandRunner): Event => IO[Unit] = {
    case MessageCreate(BasicMessage(_, content, _, channelId)) =>
      parseCardsAndDecks(content).traverse_ {
        case Card(name) => commandRunner.card(name, channelId)
        case Deck(code) => commandRunner.deck(code, channelId)
      }
    case _ => IO.unit
  }

  def withRetry: Client[IO] => Client[IO] =
    Retry(RetryPolicy(_ => Some(1.minute)))

  sealed trait Parsable         extends Product with Serializable
  case class Card(name: String) extends Parsable
  case class Deck(code: String) extends Parsable

  def parseCardsAndDecks(content: String): List[Parsable] = {
    import atto._, Atto._
    val cardOrDeck    = squareBrackets(stringOf1(letterOrDigit)).map(Deck) | braces(takeWhile(_ != '}')).map(Card)
    val cardsAndDecks = many(takeWhile(char => char != '[' && char != '{') ~> cardOrDeck)
    cardsAndDecks.parseOnly(content).option.orEmpty
  }
}
