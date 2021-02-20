package lorstats

import cats.data.NonEmptyList
import cats.syntax.all._
import lorstats.model.Card
import org.apache.commons.text.similarity.LevenshteinDistance

class CardSearcher(cardList: NonEmptyList[Card]) {

  def searchCard(searchTerm: String): NonEmptyList[Card] = {
    val cardNameMap = cardList.sortBy(_.cardCode).flatMap { card => 
      val a = card.name.split(' ').sliding(searchTerm.split(' ').size).map(_.mkString(" ")).map(_ -> card)
      NonEmptyList.fromListUnsafe(a.toList) // .split always returns at least 1
    }

    val cards = cardNameMap.minimumByNel(a => compareStrings(a._1, searchTerm)).map(_._2)
    cards.find(c => cards.forall(c2 => c2.name.contains(c.name))) match {
      case Some(card) => NonEmptyList.one(card)
      case None => cards
    }
  }

  private def compareStrings(str1: String, str2: String): Int = levenshtein(str1.toLowerCase, str2.toLowerCase)

  private val levenshtein = new LevenshteinDistance()
}
