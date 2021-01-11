package lorstats

import cats.data.NonEmptyList
import cats.effect.{IO, Resource}
import cats.syntax.all._
import java.io.File
import java.nio.file.{Files}
import sys.process._
import org.http4s.Uri

object CardListRenderer {
  def renderCardList(cardLinks: NonEmptyList[Uri]): Resource[IO, File] = {
    val cards     = cardLinks.foldMap(cardHtml)
    val width     = cardLinks.size
    val imageHtml = html(cards, width)
    Resource.make(IO {
      val tempPngFile = Files.createTempFile("cardlist", ".png")
      (s"""echo "$imageHtml"""" #| s"wkhtmltoimage --transparent --width $width - ${tempPngFile.toAbsolutePath()}") !! ProcessLogger(_ => ())
      tempPngFile.toFile()
    })(file => IO(file.delete()).void)
  }

  private def html(cardsHtml: String, count: Int) = s"""
    <html>
      <head>
      <style>
      body {
        background-color: transparent;
        padding: 0px;
        margin: 0px;
        font-size: 0;
        display: inline-block;
        width: ${count * 680};
      }
      img {
        padding: 0px;
        margin: 0px;
      }
      </style>
      </head>
      <body>$cardsHtml</body>
    </html>"""

  private def cardHtml(link: Uri) =
    s"<img src='$link' height=1024 width=680>"
}
