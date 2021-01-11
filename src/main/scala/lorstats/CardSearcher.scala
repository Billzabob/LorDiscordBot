package lorstats

import cats.data.NonEmptyList
import cats.syntax.all._
import lorstats.model.Card
import org.apache.commons.text.similarity.JaroWinklerDistance

class CardSearcher(cardList: NonEmptyList[Card]) {

  // Better but could still use some work:
  //   - Give more weight to full word
  //   - Average instead of .maximum?
  def searchCard(searchTerm: String): Card =
    cardList.sortBy(_.name.size).maximumBy { card =>
      val c = for {
        a <- NonEmptyList(searchTerm, searchTerm.split(' ').toList).map(_.toLowerCase)
        b <- NonEmptyList(card.name, card.name.split(' ').toList).map(_.toLowerCase)
      } yield compareStrings(a, b)
      c.maximum
    }

  private def compareStrings(str1: String, str2: String): Double = jaroWinkler(str1, str2)

  private val jaroWinkler = new JaroWinklerDistance()
}
