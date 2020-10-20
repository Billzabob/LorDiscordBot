package lorstats.model

import cats.data.NonEmptyList
import io.circe.generic.extras.semiauto._
import io.circe.Decoder

case class MatchInfo(
    gameMode: String,
    gameType: String,
    gameStartTimeUtc: String,
    gameVersion: String,
    players: NonEmptyList[Player],
    totalTurnCount: Int
)

object MatchInfo extends SnakeCaseMemberNames {
  implicit val matchInfoDecoder: Decoder[MatchInfo] = deriveConfiguredDecoder
}
