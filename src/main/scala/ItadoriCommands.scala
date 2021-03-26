import ackcord.DiscordClient
import ackcord.commands._
import ackcord.data.{OutgoingEmbed, OutgoingEmbedAuthor, OutgoingEmbedFooter, OutgoingEmbedThumbnail}
import ackcord.requests.{CreateMessage, Requests}
import ackcord.syntax.TextChannelSyntax
import akka.NotUsed
import akka.stream.scaladsl.Flow

import java.time.temporal.ChronoUnit
import scala.concurrent.Future

class ItadoriCommands(client: DiscordClient, requests: Requests) extends CommandController(requests) {
  val about = Command.namedParser(getPrefix("about"))
    .described("About", "虎杖悠仁の情報を返す。")
    .toSink {
      Flow[CommandMessage[NotUsed]]
        .map { m =>
          val (_, _, iconUrl) = Utility.getBotNameIdIcon(m.cache)
          CreateMessage.mkEmbed(m.message.channelId, OutgoingEmbed(
            author = Some(OutgoingEmbedAuthor("呪術廻戦の虎杖悠仁", iconUrl = Some(iconUrl))),
            color = Some(0xD6A09A),
            description = Some("The Land of Cute Boisの虎杖悠仁。\n虎杖はアニメ・マンガ「[呪術廻戦]()」の主人公です。\n虎杖バージョン0.3の開発者：\n**Tetsuki Syu#1250、Kirito#9286**\n制作言語・フレームワーク：\n[Scala](https://www.scala-lang.org/)と[Ackcord](https://ackcord.katsstuff.net/)ライブラリ。"),
            footer = Some(OutgoingEmbedFooter("虎杖ボット：リリース 0.3 | 2021-03-26")),
            thumbnail = Some(OutgoingEmbedThumbnail("https://cdn.discordapp.com/attachments/811517007446671391/813909301365833788/scala-spiral.png"))
        ))}
        .to(requests.sinkIgnore)
    }

  val ping = Command.namedParser(getPrefix("ping"))
    .described("Ping", "レイテンシを返す。")
    .asyncOpt { implicit m =>
      import requestHelper._
      for {
        sentMsg <- run(m.textChannel.sendMessage("\uD83C\uDFD3 ポン！"))
        time = ChronoUnit.MILLIS.between(m.message.timestamp, sentMsg.timestamp)
        _ <- run(m.textChannel.sendMessage(s"コマンドとレスポンスの間に${time}ミリ秒が経ちました。"))
      } yield()
    }

  private def getPrefix(aliases: String*): StructuredPrefixParser = {
    PrefixParser.structuredAsync(
      (c, m) => m.guild(c).fold(Future.successful(false))(_ => Future.successful(false)),
      (c, m) => m.guild(c).fold(Future.successful(Seq("i?")))(_ => Future.successful(Seq("i?"))),
      (_, _) => Future.successful(aliases)
    )
  }
}
