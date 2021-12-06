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
                .withColor(getColorForRegion(getRegion(card.regionRefs)))
            )
            .some
        )
        client.sendInteractionResponse(response, id, token)
    }
    IO(println(s"Retrieving card art by command for $username: $cardName, found ${cards.map(_.name).map(name => s"'$name'").intercalate(", ")}")) *> sendResponse
  }

  def getRegion(regions: NonEmptyList[String]): String = regions match {
    case NonEmptyList(region, Nil) => region
    case multiple                  => multiple.toList.filterNot(_ == "BandleCity").head
  }

  private val getColorForRegion: String => Color = {
    case "BandleCity"   => Color(184, 205, 31)
    case "ShadowIsles"  => Color(52, 114, 100)
    case "Ionia"        => Color(200, 119, 144)
    case "PiltoverZaun" => Color(222, 149, 107)
    case "Noxus"        => Color(150, 72, 70)
    case "Demacia"      => Color(183, 167, 120)
    case "Bilgewater"   => Color(171, 76, 51)
    case "Shurima"      => Color(225, 151, 43)
    case "Targon"       => Color(140, 91, 254)
    case "Freljord"     => Color(80, 175, 213)
  }
}
