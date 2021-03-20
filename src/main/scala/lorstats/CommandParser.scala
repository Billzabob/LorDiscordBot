package lorstats

import cats.syntax.all._

object CommandParser {
  def parseCardsAndDecks(content: String): List[Parsable] = {
    import atto._, Atto._
    val card  = braces(braces(takeWhile(a => !"}".contains(a)))).map(Card)
    val cards = many(takeWhile(_ != '{') ~> card)
    cards.parseOnly(content).option.orEmpty
  }

  sealed trait Parsable         extends Product with Serializable
  case class Card(name: String) extends Parsable
}
