package lorstats.model

import io.circe.generic.extras.semiauto._
import io.circe.Decoder

case class Match(
    metadata: MatchMetadata,
    info: MatchInfo
)

object Match extends SnakeCaseMemberNames {
  implicit val matchDecoder: Decoder[Match] = deriveConfiguredDecoder
}
