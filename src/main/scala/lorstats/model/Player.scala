package lorstats.model

import io.circe.generic.extras.semiauto._
import io.circe.Decoder

case class Player(
    puuid: String,
    deckId: String,   // Can be empty string in special game modes like Labs
    deckCode: String, // Can be empty string in special game modes like Labs
    factions: List[String],
    gameOutcome: String,
    orderOfPlay: Int
)

object Player extends SnakeCaseMemberNames {
  implicit val playerDecoder: Decoder[Player] = deriveConfiguredDecoder
}
