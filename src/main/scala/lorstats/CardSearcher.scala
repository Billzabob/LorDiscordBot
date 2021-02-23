package lorstats

import cats.data.NonEmptyList
import cats.syntax.all._
import lorstats.model.Card
import org.apache.commons.text.similarity.LevenshteinDistance

class CardSearcher(cardList: NonEmptyList[Card]) {

  def searchCard(searchTerm: String, champLevel: Option[Int]): NonEmptyList[Card] = {
    val cardNameMap = cardList.flatMap { card =>
      val a = card.name.split(' ').sliding(searchTerm.split(' ').size).map(_.mkString(" ")).map(_ -> card)
      NonEmptyList.fromListUnsafe(a.toList) // .split always returns at least 1
    }

    val cards = cardNameMap.minimumByNel(a => compareStrings(a._1, searchTerm)).map(_._2)
    champLevel match {
      case Some(level) if cards.forall(_.supertype == "Champion") =>
        val champ = cards.toList.applyOrElse[Int, Card](level - 1, _ => cards.last)
        NonEmptyList.one(champ)
      case _ =>
        cards.find(c => cards.forall(c2 => c2.name.toLowerCase.contains(c.name.toLowerCase))) match {
          case Some(card) => NonEmptyList.one(card)
          case None       => cards
        }
    }
  }

  private def compareStrings(str1: String, str2: String): Int = levenshtein(str1.toLowerCase, str2.toLowerCase)

  private val levenshtein = new LevenshteinDistance()
}
