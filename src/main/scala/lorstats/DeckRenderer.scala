package lorstats

import cats.effect.IO
import cats.syntax.all._
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import lorstats.model.Card
import sys.process._

object DeckRenderer {
  def renderDeck(deck: Map[Card, Int]): IO[Path] = {
    val cards     = deck.toList.sortBy(_._1.name).sortBy(_._1.cost).foldMap { case (card, count) => cardHtml(card.cardCode, count) }
    val imageHtml = html(cards)
    IO {
      val tempHtmlFile = Files.createTempFile("deck", ".html")
      val path         = Files.write(tempHtmlFile, imageHtml.getBytes(StandardCharsets.UTF_8))
      val tempPngFile  = Files.createTempFile("deck", ".png")
      s"wkhtmltoimage --width 412 file://${path.toAbsolutePath()} ${tempPngFile.toAbsolutePath()}" ! ProcessLogger(_ => ())
      tempPngFile
    }
  }

  private def html(cardsHtml: String) = s"""
    <html>
      <head>
        <style>
        body {
          background-color: #332021;
          padding:0px;
          margin:0px;
        }
        img {
          vertical-align: middle;
          padding: 0px;
          margin: 0px;
        }
        span {
          font-size: 30px;
          vertical-align: -8px;
          padding: 16px 20px;
          margin: 0px;
          color: #B8854A;
          background-color: #1A1515;
        }
        </style>
      </head>
      <body>$cardsHtml</body>
    </html>"""

  private def cardHtml(code: String, count: Int) =
    s"<div><img src='https://raw.githubusercontent.com/rikumiyao/hs-deck-viewer/master/src/resources/LorTiles/$code.png'><span>$count</span></div>"
}
