package lorstats.model

import dissonance.data._
import skunk._
import skunk.codec.all._

case class Quiz(channel: Snowflake, cardName: String)

object Quiz {
  val channelCodec: Codec[Long] = int8
  val cardNameCodec: Codec[String] = varchar(32)
  val quizCodec: Codec[Quiz] = (channelCodec ~ cardNameCodec).gimap[Quiz]
}
