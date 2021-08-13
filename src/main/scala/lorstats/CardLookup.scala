package lorstats

import cats.data.NonEmptyList
import cats.effect.IO
import cats.syntax.all._
import dissonance.data._
import dissonance.DiscordClient

class CardLookup(client: DiscordClient, cardSearcher: CardSearcher) {
  def card(cardName: String, channelId: Snowflake, username: String): IO[Unit] = {
    val cards = cardSearcher.searchCard(cardName, None)
    val sendResponse = cards match {
      case NonEmptyList(card, Nil) =>
        client.sendMessage(card.assets.head.gameAbsolutePath.renderString, channelId).void
      case NonEmptyList(card, others) =>
        val otherCards = s"Did you mean: " ++ others.map(_.name).mkString(", ")
        client.sendMessage(card.assets.head.gameAbsolutePath.renderString, channelId) >> client.sendMessage(otherCards, channelId).void
    }
    IO(println(s"Retrieving card for $username: $cardName, found ${cards.map(_.name).map(name => s"'$name'").intercalate(", ")}")) *> sendResponse
  }

  def cardSlashCommand(cardName: String, champLevel: Option[Int], id: Snowflake, token: String, channelId: Snowflake, username: String): IO[Unit] = {
    val cards = cardSearcher.searchCard(cardName, champLevel)
    val sendResponse = cards match {
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
    IO(println(s"Retrieving card by command for $username: $cardName, found ${cards.map(_.name).map(name => s"'$name'").intercalate(", ")}")) *> sendResponse
  }

  def cardArtSlashCommand(cardName: String, champLevel: Option[Int], id: Snowflake, token: String, username: String): IO[Unit] = {
    val cards = cardSearcher.searchCard(cardName, champLevel)
    val sendResponse = cards match {
      case NonEmptyList(card, _) =>
        val response = InteractionResponse(
          InteractionResponseType.ChannelMessageWithSource,
          InteractionApplicationCommandCallbackData.make
            .addEmbed(
              Embed.make
                .withImage(Image(Some(card.assets.head.fullAbsolutePath), none, none, none))
                .withTitle(card.name)
                .withFooter(Footer(card.flavorText, none, none))
                .withColor(getColorForRegion(card.regionRef))
            )
            .some
        )
        client.sendInteractionResponse(response, id, token)
    }
    IO(println(s"Retrieving card art by command for $username: $cardName, found ${cards.map(_.name).map(name => s"'$name'").intercalate(", ")}")) *> sendResponse
  }

  private val getColorForRegion: String => Color = {
    case "BandleCity" => Color(196, 205, 70)
    case "ShadowIsles" => Color(82, 179, 157)
    case "Ionia" => Color(196, 132, 155)
    case "PiltoverZaun" => Color(231, 153, 108)
    case "Noxus" => Color(182, 83, 77)
    case "Demacia" => Color(238, 230, 208)
    case "Bilgewater" => Color(179, 100, 51)
    case "Shurima" => Color(245, 208, 46)
    case "Targon" => Color(140, 91, 254)
    case "Freljord" => Color(191, 231, 250)
  }
}
