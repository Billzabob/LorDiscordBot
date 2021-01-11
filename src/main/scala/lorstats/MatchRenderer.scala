package lorstats

import cats.effect.{IO, Resource}
import cats.syntax.all._
import java.io.File
import java.nio.file.Files
import lorstats.LorApiClient.GameInfo
import fordeckmacia.Deck
import cats.data.NonEmptyList
import lorstats.model.Card
import sys.process._

class MatchRenderer(cards: NonEmptyList[Card]) {
  def renderMatch: GameInfo => Resource[IO, File] = { case GameInfo(account, opp, game) =>
    val player   = game.info.players.find(_.puuid == account.puuid).getOrElse(throw new Exception(s"Couldn't find player"))
    val opponent = game.info.players.find(a => a.puuid.some == opp.map(_.puuid))

    val (playerChampHtml, playerChampCount) = Deck
      .decode(player.deckCode)
      .toOption
      .map { d =>
        val deck   = d.cards.map { case (card, _) => cards.find(_.cardCode == card.code).getOrElse(throw new Exception(s"No match for card $card")) }.toList
        val champs = deck.filter(_.supertype == "Champion")
        (champs.map(_.cardCode).foldMap(champHtml), champs.size)
      }
      .orEmpty

    val (opponentChampHtml, opponentChampCount) = opponent
      .map(_.deckCode)
      .flatMap(code => Deck.decode(code).toOption)
      .map { d =>
        val deck   = d.cards.map { case (card, _) => cards.find(_.cardCode == card.code).getOrElse(throw new Exception(s"No match for card $card")) }.toList
        val champs = deck.filter(_.supertype == "Champion")
        (champs.map(_.cardCode).foldMap(champHtml), champs.size)
      }
      .orEmpty

    val width = (playerChampCount + opponentChampCount) * 150 + 68

    val playerRegionsHtml   = player.factions.map(factionToIcon).foldMap(regionHtml)
    val opponentRegionsHtml = opponent.map(_.factions.map(factionToIcon)).orEmpty.foldMap(regionHtml)

    val imageHtml = html(account.gameName, opp.map(_.gameName).orEmpty, playerChampHtml, opponentChampHtml, playerRegionsHtml, opponentRegionsHtml)

    Resource.make(IO {
      val tempPngFile = Files.createTempFile("game", ".png")
      (s"""echo "$imageHtml"""" #| s"wkhtmltoimage --transparent --width $width - ${tempPngFile.toAbsolutePath()}") !! ProcessLogger(_ => ())
      tempPngFile.toFile()
    })(file => IO(file.delete()).void)
  }

  def html(playerName: String, opponentName: String, playerChamps: String, opponentChamps: String, playerRegions: String, opponentRegions: String) = s"""
    <html>
      <head>
      <style>
      body {
        background-color: transparent;
        padding: 0px;
        margin: 0px;
        display: inline-block;
      }
      h1 {
        display: inline-block;
        width: 40px;
        margin: 115px 10px 0px 10px;
        vertical-align: top;
        color: #FDF4A5;
      }
      h2 {
        margin: 5px;
        color: #A1957B;
      }
      .champlist {
        background-color: transparent;
        padding: 0px;
        margin: 0px;
        font-size: 0;
        height: 200px;
      }
      .region {
        border-radius: 50%;
        background-color: #211B1A;
        padding: 3px;
        margin: 10px;
      }
      .champ {
        border-radius: 10px;
      }
      .player {
        display: inline-block;
        background-color: transparent;
        padding: 0px;
        margin: 0px;
        text-align: center;
      }
      </style>
      </head>
      <body>
        <div class=player>
          <h2>$playerName</h2>
          <div class=champlist>$playerChamps</div>
          <div>$playerRegions</div>
        </div>
          <h1>VS</h1>
        <div class=player>
          <h2>$opponentName</h2>
          <div class=champlist>$opponentChamps</div>
          <div>$opponentRegions</div>
        </div>
      </body>
    </html>"""

  private def champHtml(code: String) =
    s"<img class=champ src='https://lor-cdn.s3.eu-central-1.amazonaws.com/cards-img/cropped-panels/$code-full.jpg' width=146 height=200>"

  private def regionHtml(region: String) =
    s"<img class=region src='https://dd.b.pvp.net/latest/core/en_us/img/regions/icon-$region.png' width=45>"

  private def factionToIcon(faction: String) = faction match {
    case "faction_Bilgewater_Name"  => "bilgewater"
    case "faction_Piltover_Name"    => "piltoverzaun"
    case "faction_Demacia_Name"     => "demacia"
    case "faction_MtTargon_Name"    => "targon"
    case "faction_Ionia_Name"       => "ionia"
    case "faction_ShadowIsles_Name" => "shadowisles"
    case "faction_Noxus_Name"       => "noxus"
    case "faction_Freljord_Name"    => "freljord"
    case other                      => throw new Exception(s"Unknown faction: $other")
  }
}
