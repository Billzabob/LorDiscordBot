package lorstats.model

import io.circe.generic.extras.defaults._
import io.circe.generic.extras.semiauto._
import io.circe.Decoder

case class MastersPlayers(players: List[MastersPlayer])

object MastersPlayers {
  implicit val mastersPlayersDecoder: Decoder[MastersPlayers] = deriveConfiguredDecoder
}
