package lorstats.model

import io.circe.generic.extras.defaults._
import io.circe.generic.extras.semiauto._
import io.circe.Decoder

case class MastersPlayer(
  name: String,
  rank: Int
)

object MastersPlayer {
  implicit val mastersPlayerDecoder: Decoder[MastersPlayer] = deriveConfiguredDecoder
}
