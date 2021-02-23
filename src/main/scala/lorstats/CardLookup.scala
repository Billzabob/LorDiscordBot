package lorstats

import cats.data.NonEmptyList
import cats.effect.IO
import cats.syntax.all._
import dissonance.data._
import dissonance.DiscordClient
import org.http4s.implicits._
import org.http4s.Uri

class CardLookup(client: DiscordClient, cardSearcher: CardSearcher) {
  def card(cardName: String, channelId: Snowflake): IO[Unit] = IO(println(s"Retrieving card: $cardName")) *> {
    cardSearcher.searchCard(cardName) match {
      case NonEmptyList(card, Nil) => 
        val embed = Embed.make
          .withAuthor(Author(card.name.some, None, regionIcon(card.regionRef), None))
          .withImage(Image(Some(card.assets.head.gameAbsolutePath), None, None, None))
        client.sendEmbed(embed, channelId).void
      case NonEmptyList(card, others) => 
        val cards = (card :: others).map(_.name).distinct.mkString(", ")
        client.sendMessage(s"Multiple possible matches: $cards", channelId).void
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
      case "Shurima"      => "icon-shurima.png".some
      case _              => None
    }
    icon.map(uri"https://dd.b.pvp.net/latest/core/en_us/img/regions".addPath)
  }
}
