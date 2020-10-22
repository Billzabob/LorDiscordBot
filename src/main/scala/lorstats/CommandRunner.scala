package lorstats

import cats.data.NonEmptyList
import cats.syntax.all._
import cats.effect.{Blocker, IO}
import dissonance.DiscordClient
import dissonance.data._
import fordeckmacia.Deck
import lorstats.model.Card
import org.apache.commons.text.similarity.JaroWinklerDistance
import org.http4s.implicits._
import org.http4s.Uri
import scodec.Attempt.{Failure, Successful}

class CommandRunner(client: DiscordClient, cardList: NonEmptyList[Card], blocker: Blocker) {
  def card(cardName: String, channelId: Snowflake): IO[Unit] = IO(println(s"Retrieving card: $cardName")) *> {
    val card  = cardList.maximumBy(card => compareStrings(card.name, cardName))
    val embed = Embed.make.withAuthor(Author(card.name.some, None, regionIcon(card.regionRef), None)).withImage(Image(Some(card.assets.head.gameAbsolutePath), None, None, None))
    val levelUp = if (card.supertype == "Champion") {
      cardList.find(c => c.name.toLowerCase == card.name.toLowerCase && c.cardCode != card.cardCode)
    } else None
    val withThumb = levelUp.fold(embed)(l => embed.withThumbnail(Image(Some(l.assets.head.gameAbsolutePath), None, None, None)))
    client.sendEmbed(withThumb, channelId).void
  }

  def deck(deckCode: String, channelId: Snowflake): IO[Unit] = IO(println(s"Retrieving deck code: $deckCode")) *> {
    Deck.decode(deckCode) match {
      case Successful(deck) =>
        val deckWithMetadata = deck.cards.map { case (card, count) => cardList.find(_.cardCode == card.code).get -> count }
        // TODO: Add template while deck is rendering since it can take a few seconds
        client.sendMessage("Generating deck <a:loading:768939367972077622>", channelId).flatMap { message =>
          DeckRenderer.renderDeck(deckWithMetadata).use { file =>
            client.sendFile(file, channelId, blocker).void *> client.deleteMessage(channelId, message.id)
          }
        }
      case Failure(_) =>
        client.sendMessage(s"`$deckCode` is not a valid deck code you dummy", channelId).void
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

  private def compareStrings(str1: String, str2: String): Double = jaroWinkler(str1, str2)

  private val jaroWinkler = new JaroWinklerDistance()
}
