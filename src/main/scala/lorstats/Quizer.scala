package lorstats

import cats.data.NonEmptyList
import cats.effect.{Blocker, IO, Timer}
import cats.syntax.all._
import dissonance.data._
import dissonance.DiscordClient
import lorstats.model.{Card, Quiz}
import scala.util.Random
import scala.concurrent.duration._

class Quizer(image: ImageStuff, cards: NonEmptyList[Card], client: DiscordClient, blocker: Blocker, random: Random, db: DB)(implicit t: Timer[IO]) {
  def sendQuiz(channel: Snowflake, id: Snowflake, token: String): IO[Unit] = {
    client.sendInteractionResponse(InteractionResponse(InteractionResponseType.AcknowledgeWithSource, None), id, token) >>
      IO(random.between(0, cardsWithoutChampSpells.size)).map(cardsWithoutChampSpells.toList).flatMap { card =>
        db.setCardQuizForChannel(Quiz(channel, card.name)) >>
          image.cardImageWithNameHidden(card).use(image => client.sendFile(image, channel, blocker)) >>
          client.sendMessage("You have 30 seconds to guess the name using **/answer**", channel) >>
          IO.sleep(30.seconds) >>
          client.sendMessage(s"The answer was: **${card.name}**", channel).void
      }
  }

  def checkAnswer(channel: Snowflake, user: Snowflake, id: Snowflake, token: String, answer: String): IO[Unit] = {
    db.currentQuizCard(channel).flatMap { card =>
      val response = card match {
        case Some(cardName) if cardName.toLowerCase == answer.toLowerCase =>
          InteractionResponse(
            InteractionResponseType.ChannelMessage,
            Some(InteractionApplicationCommandCallbackData(None, s"<@$user> guessed RIGHT ✅", None, None))
          )
        case Some(_) =>
          InteractionResponse(
            InteractionResponseType.ChannelMessage,
            Some(InteractionApplicationCommandCallbackData(None, s"<@$user> guessed WRONG ❌", None, None))
          )
        case None =>
          InteractionResponse(
            InteractionResponseType.ChannelMessageWithSource,
            Some(InteractionApplicationCommandCallbackData(None, "There is no quiz active", None, None))
          )
      }
      client.sendInteractionResponse(response, id, token)
    }
  }

  private val cardsWithoutChampSpells = cards.filterNot(c => c.supertype == "Champion" && c.`type` == "Spell").filterNot(_.keywords.contains("Skill"))
}
