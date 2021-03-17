package lorstats

import cats.data.NonEmptyList
import cats.effect.{IO, Timer}
import cats.syntax.all._
import dissonance.data._
import dissonance.DiscordClient
import lorstats.model.{Card, Guess, Quiz}
import org.apache.commons.text.similarity.LevenshteinDistance
import org.http4s.Uri
import scala.util.Random
import scala.concurrent.duration._
import skunk.SqlState

class Quizer(cards: NonEmptyList[Card], client: DiscordClient, random: Random, db: DB)(implicit t: Timer[IO]) {
  def sendQuiz(channel: Snowflake, id: Snowflake, token: String): IO[Unit] = {
    val quiz = for {
      card <- IO(random.between(0, cardsWithoutChampSpells.size)).map(cardsWithoutChampSpells.toList)
      _    <- IO(println(s"Starting quiz for card: ${card.name}"))
      _    <- db.setCardQuizForChannel(Quiz(channel, card.name))
      _ <- client
             .sendInteractionResponse(
               InteractionResponse(
                 InteractionResponseType.ChannelMessageWithSource,
                 InteractionApplicationCommandCallbackData.make
                   .addEmbed(
                     Embed.make
                       .withTitle("Guess the name using /answer")
                       .withDescription("You have 30 seconds!")
                       .withImage(Image(Some(Uri.unsafeFromString(s"https://lor-quiz.sfo3.digitaloceanspaces.com/cards/${card.cardCode}.png")), None, Some(1024), Some(680)))
                   )
                   .some
               ),
               id,
               token
             )
             .attempt
      _ <- IO.sleep(30.seconds).attempt
      _ <- reportAnswer(channel, card.name).attempt
      _ <- db.clearQuiz(channel).attempt
    } yield ()

    quiz.recoverWith { case SqlState.UniqueViolation(_) =>
      client.sendInteractionResponse(
        InteractionResponse(
          InteractionResponseType.ChannelMessageWithSource,
          InteractionApplicationCommandCallbackData.make.withContent("There is already a quiz in progress!").addFlag(InteractionCallbackFlag.Ephemeral).some
        ),
        id,
        token
      )
    }
  }

  def checkAnswer(channel: Snowflake, user: Snowflake, id: Snowflake, token: String, answer: String): IO[Unit] = {
    db.currentQuizCard(channel).flatMap { card =>
      val response = card match {
        case Some(cardName) if cardName.toLowerCase == answer.toLowerCase =>
          InteractionResponse(
            InteractionResponseType.ChannelMessage,
            InteractionApplicationCommandCallbackData.make.addEmbed(Embed.make.withDescription(s"<@$user> guessed RIGHT").withColor(Color.green)).some
          )
        case Some(cardName) if compareStrings(cardName, answer) < 2 =>
          InteractionResponse(
            InteractionResponseType.ChannelMessage,
            InteractionApplicationCommandCallbackData.make.addEmbed(Embed.make.withDescription(s"<@$user> was close!").withColor(Color.blue)).some
          )
        case Some(_) =>
          InteractionResponse(
            InteractionResponseType.ChannelMessage,
            InteractionApplicationCommandCallbackData.make.addEmbed(Embed.make.withDescription(s"<@$user> guessed WRONG").withColor(Color.red)).some
          )
        case None =>
          InteractionResponse(
            InteractionResponseType.ChannelMessageWithSource,
            InteractionApplicationCommandCallbackData.make.withContent("There is no quiz active, start one with **/quiz**").addFlag(InteractionCallbackFlag.Ephemeral).some
          )
      }
      client.sendInteractionResponse(response, id, token) >> db.addGuessForPlayer(Guess(channel, user, answer.take(32)))
    }
  }

  private def reportAnswer(channel: Snowflake, answer: String): IO[Unit] = for {
    guesses <- db.getGuessesForChannel(channel).compile.toList
    e        = Embed.make.withTitle(s"The answer was: $answer")
    _ <- if (guesses.isEmpty)
           client.sendEmbed(e.withDescription("There were no guesses"), channel)
         else {
           val fields = guesses
             .groupBy(_.user)
             .map { case (user, guesses) =>
               s"<@$user>: " + guesses.map(_.answer).mkString(", ")
             }
             .toList
           val embed = e.withDescription(fields.mkString("\n"))
           client.sendEmbed(embed, channel)
         }
  } yield ()

  private val cardsWithoutChampSpells = cards.filterNot(c => c.supertype == "Champion" && c.`type` == "Spell").filterNot(_.keywords.contains("Skill"))

  private val levenshtein                                     = new LevenshteinDistance()
  private def compareStrings(str1: String, str2: String): Int = levenshtein(str1.toLowerCase, str2.toLowerCase)
}
