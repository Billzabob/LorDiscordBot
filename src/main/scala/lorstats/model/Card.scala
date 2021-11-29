package lorstats.model

import cats.data.NonEmptyList
import io.circe.Decoder
import io.circe.generic.extras.defaults._
import io.circe.generic.extras.semiauto._
import lorstats.model.Card.Asset
import org.http4s.Uri
import org.http4s.circe._
import io.circe.Json

// TODO: Enums
case class Card(
    associatedCards: List[Json],
    associatedCardRefs: List[String],
    assets: NonEmptyList[Asset],
    regions: NonEmptyList[String],
    regionRefs: NonEmptyList[String],
    attack: Int,
    cost: Int,
    health: Int,
    descriptionRaw: String,
    levelupDescriptionRaw: String,
    flavorText: String,
    name: String,
    cardCode: String,
    keywords: List[String],
    spellSpeed: String,
    rarity: String,
    supertype: String,
    `type`: String,
    collectible: Boolean,
    set: String
)

object Card {
  case class Asset(
      gameAbsolutePath: Uri,
      fullAbsolutePath: Uri
  )

  object Asset {
    implicit val assetDecoder: Decoder[Asset] = deriveConfiguredDecoder
  }

  implicit val cardDecoder: Decoder[Card] = deriveConfiguredDecoder
}
