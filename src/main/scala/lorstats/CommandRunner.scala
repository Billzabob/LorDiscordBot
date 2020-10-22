package lorstats

import cats.data.NonEmptyList
import cats.syntax.all._
import cats.effect.{Blocker, IO}
import dissonance.DiscordClient
import dissonance.model._
import dissonance.model.embed._
import fordeckmacia.Deck
import lorstats.model.Card
import org.apache.commons.text.similarity.JaroWinklerDistance
import scodec.Attempt.{Failure, Successful}

class CommandRunner(client: DiscordClient, cardList: NonEmptyList[Card], blocker: Blocker) {
  def card(cardName: String, channelId: Snowflake): IO[Unit] = {
    val card  = cardList.maximumBy(card => compareStrings(card.name, cardName))
    val embed = Embed.make.withTitle(card.name).withImage(Image(Some(card.assets.head.gameAbsolutePath), None, None, None))
    val levelUp = if (card.supertype == "Champion") {
      cardList.find(c => c.name.toLowerCase == cardName.toLowerCase && c.cardCode != card.cardCode)
    } else None
    val withThumb = levelUp.fold(embed)(l => embed.withThumbnail(Image(Some(l.assets.head.gameAbsolutePath), None, None, None)))
    client.sendEmbed(withThumb, channelId).void
  }

  def deck(deckCode: String, channelId: Snowflake): IO[Unit] =
    Deck.decode(deckCode) match {
      case Successful(deck) =>
        val deckWithMetadata = deck.cards.map { case (card, count) => cardList.find(_.cardCode == card.code).get -> count }
        // TODO: Add template while deck is rendering since it can take a few seconds
        DeckRenderer.renderDeck(deckWithMetadata).use { file =>
          client.sendFile(file, channelId, blocker).void
        }
      case Failure(_) =>
        client.sendMessage(s"`$deckCode` is not a valid deck code you dummy", channelId).void
    }

  private def compareStrings(str1: String, str2: String): Double = jaroWinkler(str1, str2)

  private val jaroWinkler = new JaroWinklerDistance()
}
