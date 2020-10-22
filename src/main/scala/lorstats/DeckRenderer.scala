package lorstats

import cats.effect.{IO, Resource}
import cats.syntax.all._
import java.io.File
import java.nio.file.{Files}
import lorstats.model.Card
import sys.process._

object DeckRenderer {
  def renderDeck(deck: Map[Card, Int]): Resource[IO, File] = {
    val cards     = deck.toList.sortBy(_._1.name).sortBy(_._1.cost).foldMap { case (card, count) => cardHtml(card.cardCode, count) }
    val imageHtml = html(cards).filterNot(_ === '\n')
    Resource.make(IO {
      val tempPngFile = Files.createTempFile("deck", ".png")
      (s"""echo "$imageHtml"""" #| s"wkhtmltoimage --width 412 - ${tempPngFile.toAbsolutePath()}") !! ProcessLogger(_ => ())
      tempPngFile.toFile()
    })(file => IO(file.delete()).void)
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
          border-top-right-radius: 6px;
          border-bottom-right-radius: 6px;
        }
        </style>
      </head>
      <body>$cardsHtml</body>
    </html>"""

  private def cardHtml(code: String, count: Int) =
    s"<div><img src='https://raw.githubusercontent.com/rikumiyao/hs-deck-viewer/master/src/resources/LorTiles/$code.png' height=72 width=357><span>$count</span></div>"
}
