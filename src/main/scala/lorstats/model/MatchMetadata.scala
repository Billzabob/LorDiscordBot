package lorstats.model

import io.circe.generic.extras.semiauto._
import io.circe.Decoder

case class MatchMetadata(
    dataVersion: String,
    matchId: String,
    participants: List[String]
)

object MatchMetadata extends SnakeCaseMemberNames {
  implicit val matchMetadataDecoder: Decoder[MatchMetadata] = deriveConfiguredDecoder
}
