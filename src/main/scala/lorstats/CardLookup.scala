package lorstats

import cats.data.NonEmptyList
import cats.effect.IO
import cats.syntax.all._
import dissonance.data._
import dissonance.DiscordClient

class CardLookup(client: DiscordClient, cardSearcher: CardSearcher) {
  def card(cardName: String, channelId: Snowflake): IO[Unit] = IO(println(s"Retrieving card: $cardName")) *> {
    cardSearcher.searchCard(cardName, None) match {
      case NonEmptyList(card, Nil) =>
        client.sendMessage(card.assets.head.gameAbsolutePath.renderString, channelId).void
      case NonEmptyList(card, others) =>
        val otherCards = s"Did you mean: " ++ others.map(_.name).mkString(", ")
        client.sendMessage(card.assets.head.gameAbsolutePath.renderString, channelId) >> client.sendMessage(otherCards, channelId).void
    }
  }

  def cardSlashCommand(cardName: String, champLevel: Option[Int], id: Snowflake, token: String, channelId: Snowflake): IO[Unit] =
    IO(println(s"Retrieving card by command: $cardName")) *> {
      cardSearcher.searchCard(cardName, champLevel) match {
        case NonEmptyList(card, Nil) =>
          val response = InteractionResponse(
            InteractionResponseType.ChannelMessageWithSource,
            InteractionApplicationCommandCallbackData.make.withContent(card.assets.head.gameAbsolutePath.renderString).some
          )
          client.sendInteractionResponse(response, id, token)
        case NonEmptyList(card, others) =>
          val response = InteractionResponse(
            InteractionResponseType.ChannelMessageWithSource,
            InteractionApplicationCommandCallbackData.make.withContent(card.assets.head.gameAbsolutePath.renderString).some
          )
          val otherCards = s"Did you mean: " ++ others.map(_.name).mkString(", ")
          client.sendInteractionResponse(response, id, token) >> client.sendMessage(otherCards, channelId).void
      }
    }
}
