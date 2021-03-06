package lorstats

import cats.data.NonEmptyList
import cats.effect.IO
import dissonance.data._
import dissonance.DiscordClient

class CardLookup(client: DiscordClient, cardSearcher: CardSearcher) {
  def card(cardName: String, channelId: Snowflake): IO[Unit] = IO(println(s"Retrieving card: $cardName")) *> {
    cardSearcher.searchCard(cardName, None) match {
      case NonEmptyList(card, Nil) =>
        client.sendMessage(card.assets.head.gameAbsolutePath.renderString, channelId).void
      case NonEmptyList(card, others) =>
        val cards = (card :: others).map(_.name).distinct.mkString(", ")
        client.sendMessage(s"Multiple possible matches: $cards", channelId).void
    }
  }

  def cardSlashCommand(cardName: String, champLevel: Option[Int], id: Snowflake, token: String): IO[Unit] = IO(println(s"Retrieving card: $cardName")) *> {
    cardSearcher.searchCard(cardName, champLevel) match {
      case NonEmptyList(card, Nil) =>
        val response = InteractionResponse(
          InteractionResponseType.ChannelMessageWithSource,
          Some(
            InteractionApplicationCommandCallbackData(
              None,
              card.assets.head.gameAbsolutePath.renderString,
              None,
              None
            )
          )
        )
        client.sendInteractionResponse(response, id, token)
      case NonEmptyList(card, others) =>
        val cards = (card :: others).map(_.name).distinct.mkString(", ")
        val response = InteractionResponse(
          InteractionResponseType.ChannelMessageWithSource,
          Some(
            InteractionApplicationCommandCallbackData(
              None,
              s"Multiple possible matches: $cards",
              None,
              None
            )
          )
        )
        client.sendInteractionResponse(response, id, token)
    }
  }
}
