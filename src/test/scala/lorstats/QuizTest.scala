package lorstats

import java.net.http.HttpClient
import org.http4s.client.jdkhttpclient.JdkHttpClient
import cats.effect.IO
import scala.concurrent.ExecutionContext

class QuizTest extends munit.FunSuite {
  test("foo") {
    implicit val cs = IO.contextShift(ExecutionContext.global)
    val client = JdkHttpClient[IO](HttpClient.newHttpClient())
    val imageStuff = new ImageStuff(client)
    val lorClient = new LorApiClient(client, null)
    val cards = lorClient.getCards

    val foo = cards.flatMap(cards => cards.traverse(card => 
      imageStuff.cardImageWithNameHidden(card, "/Users/nick.hallstrom@divvypay.com/Desktop/cards/" + card.cardCode).use(a => IO(println(a)))
    ))

    foo.unsafeRunSync()
  }
}
