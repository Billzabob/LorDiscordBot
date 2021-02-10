package lorstats

import cats.effect.IO
import dissonance.DiscordClient

object Utils {
  implicit class IoExtension[A](task: IO[A]) {
    def withLoadingMessage(message: String, channelId: Long, client: DiscordClient): IO[A] =
      for {
        message <- client.sendMessage(message + " <a:loading:768939367972077622>", channelId)
        result  <- task
        _       <- client.deleteMessage(channelId, message.id)
      } yield result
  }
}
