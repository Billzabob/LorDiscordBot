package lorstats

import cats.data.NonEmptyList
import cats.effect.{Blocker, IO}
import cats.syntax.all._
import dissonance.data._
import dissonance.DiscordClient
import lorstats.model.Card
import lorstats.Utils._
import org.http4s.implicits._
import org.http4s.Uri

class CardLookup(client: DiscordClient, cardList: NonEmptyList[Card], cardSearcher: CardSearcher, blocker: Blocker) {
  def card(cardName: String, associated: Boolean, levelUp: Boolean, channelId: Snowflake): IO[Unit] = IO(println(s"Retrieving card: $cardName")) *> {
    val card = cardSearcher.searchCard(cardName)

    val cardWithLevelUp = if (card.supertype == "Champion" && card.`type` == "Unit" && card.collectible == levelUp) {
      card.associatedCardRefs.flatMap(code => cardList.find(_.cardCode == code)).find(card => card.supertype == "Champion" && card.`type` == "Unit").get
    } else card

    if (associated) {
      val imageLinks = cardWithLevelUp.associatedCardRefs.map(code => cardList.find(_.cardCode == code).get).map(_.assets.head.gameAbsolutePath)
      NonEmptyList.fromList(imageLinks) match {
        case Some(links) =>
          CardListRenderer
            .renderCardList(links)
            .use { file =>
              val embed = Embed.make
                .withAuthor(Author(("Associated with: " + cardWithLevelUp.name).some, None, regionIcon(cardWithLevelUp.regionRef), None))
              client.sendEmbedWithFileImage(embed, file, channelId, blocker).void
            }
            .withLoadingMessage("Getting associated cards", channelId, client)
        case None =>
          client.sendMessage("No associated cards", channelId).void
      }
    } else {
      val embed = Embed.make
        .withAuthor(Author(cardWithLevelUp.name.some, None, regionIcon(cardWithLevelUp.regionRef), None))
        .withImage(Image(Some(cardWithLevelUp.assets.head.gameAbsolutePath), None, None, None))
      client.sendEmbed(embed, channelId).void
    }
  }

  private def regionIcon(region: String): Option[Uri] = {
    val icon = region match {
      case "Bilgewater"   => "icon-bilgewater.png".some
      case "Demacia"      => "icon-demacia.png".some
      case "Freljord"     => "icon-freljord.png".some
      case "Ionia"        => "icon-ionia.png".some
      case "Noxus"        => "icon-noxus.png".some
      case "PiltoverZaun" => "icon-piltoverzaun.png".some
      case "ShadowIsles"  => "icon-shadowisles.png".some
      case "Targon"       => "icon-targon.png".some
      case _              => None
    }
    icon.map(uri"https://dd.b.pvp.net/latest/core/en_us/img/regions".addPath)
  }
}
