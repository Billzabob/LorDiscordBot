package lorstats

import cats.syntax.all._
import cats.effect.Blocker
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.implicits._
import com.sksamuel.scrimage.pixels.Pixel
import dissonance.data._
import dissonance.DiscordClient
import java.net.URI
import java.nio.file.Files
import lorstats.model.Card
import scala.util.Try

class ImageStuff(client: DiscordClient, blocker: Blocker) {
  // TODO: Lots and lots of cleanup
  def sendQuizCard(input: Card, channel: Snowflake, id: Snowflake, token: String) = {
    val address = input.assets.head.gameAbsolutePath.renderString.replace("http", "https")
    println(address)
    val image = ImmutableImage.loader().fromStream(new URI(address).toURL.openStream)
    val rows = image.rows
    val paddingY = 200
    val range = List.range(-3, 3)
    val ranges = range.flatMap(x => range.map(y => x -> y)).filterNot(_ == 0 -> 0)
    val foo = rows.flatMap(_.map { p =>
      val isWhite = ranges.map { case (a, b) => Try(rows(p.y + a)(p.x + b)).getOrElse(p) }.forall { g =>
        val color = g.toColor
        val value = 250
        color.red > value && color.green > value && color.blue > value
      } && p.y > paddingY && p.y < image.height - paddingY

      (isWhite, p.x, p.y)
    })

    val bar = foo.filter(_._1)
    val maxX = bar.map(_._2).max + 20
    val minX = bar.map(_._2).min - 20
    val maxY = bar.map(_._3).max + 30
    val minY = bar.map(_._3).min - 30

    val pixels = rows.flatMap(_.map { p =>
      if (p.x >= minX && p.x <= maxX && p.y >= minY && p.y <= maxY) new Pixel(p.x, p.y, 0, 0, 0, p.alpha) else p
    })

    val grey = ImmutableImage.create(image.width, image.height, pixels, image.getType())
    val tempImage = Files.createTempFile("quiz", ".png")
    val file = grey.output(tempImage).toFile
    val response = InteractionResponse(
      InteractionResponseType.AcknowledgeWithSource,
      Some(
        InteractionApplicationCommandCallbackData(
          None,
          "",
          None,
          None
        )
      )
    )
    client.sendInteractionResponse(response, id, token) >> client.sendFile(file, channel, blocker).as(file.delete()).void
  }
}
