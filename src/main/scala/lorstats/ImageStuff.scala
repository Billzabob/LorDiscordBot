package lorstats

import cats.effect.{ContextShift, IO, Resource}
import com.sksamuel.scrimage.color.RGBColor
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.implicits._
import java.awt.Color
import java.io.File
import java.nio.file._
import lorstats.model.Card
import org.http4s.client.Client
import org.http4s.client.middleware.FollowRedirect
import org.http4s.Request

class ImageStuff(client: Client[IO])(implicit cs: ContextShift[IO]) {
  def cardImageWithNameHidden(card: Card, output: String): Resource[IO, File] = {
    val request = clientWithRedirect.stream(Request(uri = card.assets.head.gameAbsolutePath)).flatMap(_.body)
    fs2.io.toInputStreamResource(request).flatMap { bytes =>
      println(card.assets.head.gameAbsolutePath.renderString)
      println(card.name)
      val image = ImmutableImage.loader().withClassLoader(this.getClass().getClassLoader()).fromStream(bytes)
      val rows  = image.rows
      val nameCoordinates = for {
        startRow <- rows.reverseIterator.drop(140).find(_.exists(_.toColor == RGBColor.fromAwt(Color.WHITE)))
        start    <- startRow.headOption.map(_.y)
        endRow   <- rows.reverseIterator.drop(image.height - start).sliding(17).find(_.forall(_.forall(_.toColor != RGBColor.fromAwt(Color.WHITE)))).map(_.head)
        end      <- endRow.headOption.map(_.y)
      } yield (start, end)

      nameCoordinates match {
        case Some((start, end)) =>
          val padding = 5
          val result = image.map { p =>
            if (p.y < start + padding && p.y > end - padding && p.x > padding && p.x < image.width - padding)
              Color.BLACK
            else
              p.toColor
          }
          val tempFile = Resource.liftF(IO(Files.createFile(Paths.get(output + ".png")).toFile))
          tempFile.evalTap(file => IO(result.output(file)))
        case None =>
          Resource.liftF(IO.raiseError(new Exception("Could not find card name in card image")))
      }
    }
  }

  private val clientWithRedirect = FollowRedirect(2)(client)
}
