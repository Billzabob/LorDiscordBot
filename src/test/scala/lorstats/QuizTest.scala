package lorstats

import java.net.http.HttpClient
import org.http4s.client.jdkhttpclient.JdkHttpClient
import cats.effect.IO
import cats.syntax.all._
import scala.concurrent.ExecutionContext

class QuizTest extends munit.FunSuite {
  test("foo") {
    implicit val cs = IO.contextShift(ExecutionContext.global)
    val client = JdkHttpClient[IO](HttpClient.newHttpClient())
    val imageStuff = new ImageStuff(client)
    val lorClient = new LorApiClient(client, null)
    val bar = List(
      "04DE016",
      "04DE020",
      "04FR019",
      "04FR019T1",
      "04IO020",
      "04MT018",
      "04NX022",
      "04NX021",
      "04NX022T1",
      "04PZ020",
      "04PZ020T1",
      "04SH128",
      "04SH130T10",
      "04SH130T6",
      "04SH130T7",
      "04SH130T8",
      "04SH130T5",
      "04SH129",
      "04SH137",
      "04SH130",
      "04SH130T1",
      "04SH130T14",
      "04SH130T13",
      "04SH131",
      "04SH138",
      "04SH138T1",
      "04SH125",
      "04SI053",
      "04SI056",
      "04SI021",
      "04SI055T1",
      "04SI055T2",
      "04SI055",
      "04SI028",
      "04SI054",
      "04SI029",
      "04SI045"
    )

    val cards = lorClient.getCards.map(_.filter(card => bar.contains(card.cardCode)))

    val foo = cards.flatMap(cards => cards.traverse(card => 
      imageStuff.cardImageWithNameHidden(card, "/Users/nick.hallstrom@divvypay.com/Desktop/cards/" + card.cardCode).use(a => IO(println(a)))
    ))

    foo.unsafeRunSync()
  }
}
