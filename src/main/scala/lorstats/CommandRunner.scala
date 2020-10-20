package lorstats

import cats.data.NonEmptyList
import cats.syntax.all._
import cats.effect.{Blocker, IO}
import dissonance.DiscordClient
import dissonance.model._
import dissonance.model.embed._
import fordeckmacia.Deck
import lorstats.model.Card
import scodec.Attempt.{Failure, Successful}

class CommandRunner(client: DiscordClient, cardList: NonEmptyList[Card], blocker: Blocker) {
  def card(cardName: String, channelId: Snowflake): IO[Unit] = {
    val card  = cardList.minimumBy(card => levenshtein(card.name, cardName))
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
        DeckRenderer.renderDeck(deckWithMetadata).flatMap { path =>
          client.sendFile(path.toFile(), channelId, blocker).void
        }
      case Failure(_) =>
        client.sendMessage("That's not a valid deck code you dummy", channelId).void
    }

  // Ripped from wikipedia
  private def levenshtein(str1: String, str2: String): Int = {
    val lenStr1 = str1.length
    val lenStr2 = str2.length

    val d = Array.ofDim[Int](lenStr1 + 1, lenStr2 + 1)

    for (i <- 0 to lenStr1) d(i)(0) = i
    for (j <- 0 to lenStr2) d(0)(j) = j

    for (i <- 1 to lenStr1; j <- 1 to lenStr2) {
      val cost = if (str1(i - 1).toLower == str2(j - 1).toLower) 0 else 1

      d(i)(j) = List(
        d(i - 1)(j) + 1,       // deletion
        d(i)(j - 1) + 1,       // insertion
        d(i - 1)(j - 1) + cost // substitution
      ).min
    }

    d(lenStr1)(lenStr2)
  }
}
