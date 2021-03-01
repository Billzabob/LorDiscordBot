package lorstats

import cats.data.NonEmptyList
import cats.effect.Blocker
import cats.syntax.all._
import dissonance.data._
import dissonance.DiscordClient
import lorstats.model.Card
import scala.util.Random
import cats.effect.IO

class Quizer(image: ImageStuff, cards: NonEmptyList[Card], client: DiscordClient, blocker: Blocker, random: Random) {
  def sendQuiz(channel: Snowflake, id: Snowflake, token: String) = {
    client.sendInteractionResponse(InteractionResponse(InteractionResponseType.AcknowledgeWithSource, None), id, token) >>
      IO(random.between(0, cardsWithoutChampSpells.size)).map(cardsWithoutChampSpells.toList).flatMap { card =>
        image.cardImageWithNameHidden(card).use(image => client.sendFile(image, channel, blocker)).void
      }
  }

  private val cardsWithoutChampSpells = cards.filterNot(c => c.supertype == "Champion" && c.`type` == "Spell").filterNot(_.keywords.contains("Skill"))
}
