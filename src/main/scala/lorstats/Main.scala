package lorstats

import cats.data.NonEmptyList
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
import scala.util.Random

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
          val cardLookup       = new CardLookup(discord.client, cardSearcher)
          val gameplayNotifier = new GameplayNotifier(riotToken, discord, pool, cards, blocker)
          val image = new ImageStuff(discord.client, blocker)
          Stream(
            eventsStream.mapAsyncUnordered(Int.MaxValue)(handleEvents(cardLookup, cards, image)).handleError(e => println(e)).repeat.drain,
            gameplayNotifier.notifyNewGames.handleError(e => println(e)).repeat
          ).parJoinUnbounded.compile.drain
        }
      }
      .as(ExitCode.Success)
  }

  def handleEvents(cardLookup: CardLookup, cards: NonEmptyList[model.Card], image: ImageStuff): Event => IO[Unit] = {
    case MessageCreate(BasicMessage(_, content, _, channelId)) =>
      CommandParser.parseCardsAndDecks(content).traverse_ { case Card(name) =>
        cardLookup.card(name, channelId)
      }
    case InteractionCreate(id, _, ApplicationCommandInteractionData(_, "card", Some(commands)), _, _, _, token, _) =>
      val commandMap = commands.map(c => c.name -> c.value).toMap
      commandMap.get("card-name").flattenOption.flatMap(_.as[String].toOption) match {
        case Some(searchTerm) =>
          val champLevel = commandMap.get("champ-level").flattenOption.flatMap(_.as[Int].toOption)
          cardLookup.cardSlashCommand(searchTerm, champLevel, id, token)
        case None =>
          IO.unit
      }
    case InteractionCreate(id, _, ApplicationCommandInteractionData(_, "quiz", _), _, channel, _, token, _) =>
      val i = Random.between(0, cards.size)
      image.sendQuizCard(cards.toList(i), channel, id, token)
    case _ => IO.unit
  }

  def withRetry: Client[IO] => Client[IO] =
    Retry(RetryPolicy(_ => Some(1.minute)))
}
