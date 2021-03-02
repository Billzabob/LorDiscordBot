package lorstats.model

import dissonance.data._
import skunk._
import skunk.codec.all._

case class Guess(
    channel: Snowflake,
    user: Snowflake,
    answer: String
)

object Guess {
  val answerCodec: Codec[String] = varchar(32)
  val guessCodec: Codec[Guess]   = (int8 ~ int8 ~ answerCodec).gimap[Guess]
}
