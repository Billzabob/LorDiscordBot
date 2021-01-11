package lorstats

import cats.syntax.all._

object CommandParser {
  def parseCardsAndDecks(content: String): List[Parsable] = {
    import atto._, Atto._
    val cardOrDeck =
      squareBrackets(stringOf1(letterOrDigit)).map(Deck) | braces(opt(char('+')) ~ opt(char('*')) ~ takeWhile(a => !"}+*".contains(a)) ~ opt(char('+')) ~ opt(char('*'))).map {
        case ((((plus, asterisk), name), plus2), asterisk2) =>
          Card(name, asterisk.isDefined || asterisk2.isDefined, plus.isDefined || plus2.isDefined)
      }
    val cardsAndDecks = many(takeWhile(char => char != '[' && char != '{') ~> cardOrDeck)
    cardsAndDecks.parseOnly(content).option.orEmpty
  }

  sealed trait Parsable                                                extends Product with Serializable
  case class Card(name: String, associated: Boolean, levelUp: Boolean) extends Parsable
  case class Deck(code: String)                                        extends Parsable
}
