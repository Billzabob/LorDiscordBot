package lorstats

import cats.effect._
import cats.syntax.all._
import dissonance._
import dissonance.data._
import dissonance.data.events._
import fs2.Stream
import lorstats.CommandParser._
import org.http4s.client.Client
import org.http4s.client.middleware.{Retry, RetryPolicy}
import scala.concurrent.duration._

object Main extends IOApp {

  val dissonanceGuild = 689701123962962068L

  override def run(args: List[String]): IO[ExitCode] = {
    val discordToken = args(0)
    val riotToken    = args(1)
    (Discord.make(discordToken), DB.pool, Blocker[IO]).tupled
      .use { case (d, pool, blocker) =>
        val discord      = d.addMiddleware(withRetry)
        val eventsStream = discord.subscribe(Shard.singleton, Intent.GuildMessages)
        val lorClient    = new LorApiClient(discord.httpClient, riotToken)

        lorClient.getCards.flatMap { case (cards) =>
          val cardSearcher     = new CardSearcher(cards)
          val cardLookup       = new CardLookup(discord.client, cards, cardSearcher, blocker)
          val deckLookup       = new DeckLookup(discord.client, cards, blocker)
          val gameplayNotifier = new GameplayNotifier(riotToken, discord, pool, cards, blocker)
          Stream(
            eventsStream.mapAsyncUnordered(Int.MaxValue)(handleEvents(cardLookup, deckLookup)).handleError(e => println(e)).repeat.drain,
            gameplayNotifier.notifyNewGames.handleError(e => println(e)).repeat
          ).parJoinUnbounded.compile.drain
        }
      }
      .as(ExitCode.Success)
  }

  def handleEvents(cardLookup: CardLookup, deckLookup: DeckLookup): Event => IO[Unit] = {
    case MessageCreate(BasicMessage(_, content, _, channelId)) =>
      CommandParser.parseCardsAndDecks(content).traverse_ {
        case Card(name, associated, levelUp) => cardLookup.card(name, associated, levelUp, channelId)
        case Deck(code)                      => deckLookup.deck(code, channelId)
      }
    case _ => IO.unit
  }

  def withRetry: Client[IO] => Client[IO] =
    Retry(RetryPolicy(_ => Some(1.minute)))
}
