package lorstats.model

import io.circe.generic.extras.defaults._
import io.circe.generic.extras.semiauto._
import io.circe.Decoder

case class Account(
    puuid: String,
    gameName: String,
    tagLine: String
)

object Account {
  implicit val accountDecoder: Decoder[Account] = deriveConfiguredDecoder
}
