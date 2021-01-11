package lorstats

import cats.data.NonEmptyList
import cats.effect.IO
import cats.Order
import cats.syntax.all._
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

  implicit class NelExtension[A](items: NonEmptyList[A]) {
    def maximumByNel[B](f: A => B)(implicit B: Order[B]): NonEmptyList[A] =
      items
        .reduceLeftTo(NonEmptyList.one) {
          case (l @ NonEmptyList(b, _), a) if B.compare(f(a), f(b)) < 0  => l
          case (l @ NonEmptyList(b, _), a) if B.compare(f(a), f(b)) == 0 => a :: l
          case (_, a)                                                    => NonEmptyList.one(a)
        }
        .reverse
  }
}
