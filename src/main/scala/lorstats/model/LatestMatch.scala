package lorstats.model

import skunk._
import skunk.codec.all._

case class LatestMatch(puuid: String, lastMatchId: String)

object LatestMatch {
  val puuidCodec: Codec[String]            = bpchar(78)
  val lastMatchIdCodec: Codec[String]      = bpchar(36)
  val latestMatchCodec: Codec[LatestMatch] = (puuidCodec ~ lastMatchIdCodec).gimap[LatestMatch]
}
