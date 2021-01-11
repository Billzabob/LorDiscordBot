package lorstats

import cats.data.NonEmptyList
import cats.effect.{Blocker, IO}
import dissonance.data._
import dissonance.DiscordClient
import fordeckmacia.Deck
import lorstats.model.Card
import lorstats.Utils._
import scodec.Attempt.{Failure, Successful}

class DeckLookup(client: DiscordClient, cardList: NonEmptyList[Card], blocker: Blocker) {
  def deck(deckCode: String, channelId: Snowflake): IO[Unit] = IO(println(s"Retrieving deck code: $deckCode")) *> {
    Deck.decode(deckCode) match {
      case Successful(deck) =>
        val deckWithMetadata = deck.cards.map { case (card, count) => cardList.find(_.cardCode == card.code).get -> count }
        DeckRenderer
          .renderDeck(deckWithMetadata)
          .use { file =>
            client.sendFile(file, channelId, blocker).void
          }
          .withLoadingMessage("Generating deck", channelId, client)
      case Failure(_) =>
        client.sendMessage(s"`$deckCode` is not a valid deck code you dummy", channelId).void
    }
  }
}
