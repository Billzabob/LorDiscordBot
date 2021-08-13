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
import scala.util.Random

object Main extends IOApp {

  val dissonanceGuild = 689701123962962068L

  override def run(args: List[String]): IO[ExitCode] = {
    val discordToken = args(0)
    val riotToken    = args(1)
    val dbHost       = args(2)
    val dbPassword   = args(3)
    (Discord.make(discordToken), DB.pool(dbHost, dbPassword), Blocker[IO]).tupled
      .use { case (d, pool, blocker) =>
        val discord      = d.addMiddleware(withRetry)
        val eventsStream = discord.subscribe(Shard.singleton, Intent.GuildMessages)
        val lorClient    = new LorApiClient(discord.httpClient, riotToken)

        (lorClient.getCards, IO(Random)).tupled.flatMap { case (cards, random) =>
          val db               = new DB(pool)
          val cardSearcher     = new CardSearcher(cards)
          val cardLookup       = new CardLookup(discord.client, cardSearcher)
          val gameplayNotifier = new GameplayNotifier(riotToken, discord, db, cards, blocker)
          val quizer           = new Quizer(cards, discord.client, random, db)
          Stream(
            eventsStream.mapAsyncUnordered(Int.MaxValue)(handleEvents(cardLookup, quizer)).handleError(e => println(e)).repeat.drain,
            gameplayNotifier.notifyNewGames.handleError(e => println(e)).repeat
          ).parJoinUnbounded.compile.drain
        }
      }
      .as(ExitCode.Success)
  }

  def handleEvents(cardLookup: CardLookup, quizer: Quizer): Event => IO[Unit] = {
    case MessageCreate(BasicMessage(_, content, author, channelId)) =>
      CommandParser.parseCardsAndDecks(content).traverse_ { case Card(name) =>
        cardLookup.card(name, channelId, author.username)
      }
    case InteractionCreate(id, _, ApplicationCommandInteractionData(_, "card", Some(commands)), _, channelId, member, token, _) =>
      val commandMap = commands.map(c => c.name -> c.value).toMap
      commandMap.get("card-name").flattenOption.flatMap(_.as[String].toOption) match {
        case Some(searchTerm) =>
          val champLevel = commandMap.get("champ-level").flattenOption.flatMap(_.as[Int].toOption)
          val user       = member.user.map(_.username).orEmpty
          cardLookup.cardSlashCommand(searchTerm, champLevel, id, token, channelId, user)
        case None =>
          IO.unit
      }
    case InteractionCreate(id, _, ApplicationCommandInteractionData(_, "card-art", Some(commands)), _, channelId, member, token, _) =>
      val commandMap = commands.map(c => c.name -> c.value).toMap
      commandMap.get("card-name").flattenOption.flatMap(_.as[String].toOption) match {
        case Some(searchTerm) =>
          val champLevel = commandMap.get("champ-level").flattenOption.flatMap(_.as[Int].toOption)
          val user       = member.user.map(_.username).orEmpty
          cardLookup.cardArtSlashCommand(searchTerm, champLevel, id, token, channelId, user)
        case None =>
          IO.unit
      }
    case InteractionCreate(id, _, ApplicationCommandInteractionData(_, "quiz", _), _, channel, _, token, _) =>
      quizer.sendQuiz(channel, id, token)
    case InteractionCreate(id, _, ApplicationCommandInteractionData(_, "answer", Some(commands)), _, channel, member, token, _) =>
      val user   = member.user.get.id.value
      val answer = commands.find(_.name == "guess").get.value.get.as[String].toOption.get
      IO(println(s"Someone guessed: $answer")) >> quizer.checkAnswer(channel, user, id, token, answer)
    case _ => IO.unit
  }

  def withRetry: Client[IO] => Client[IO] =
    Retry(RetryPolicy(_ => Some(1.minute)))
}
