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
    .described("About", "Shows information of Itadori Yuuji.")
    .toSink {
      Flow[CommandMessage[NotUsed]]
        .map { m =>
          val (_, _, iconUrl) = Utility.getBotNameIdIcon(m.cache)
          CreateMessage.mkEmbed(m.message.channelId, OutgoingEmbed(
            author = Some(OutgoingEmbedAuthor("Itadori Yuuji from Jujutsu Kaisen", iconUrl = Some(iconUrl))),
            color = Some(0xD6A09A),
            description = Some("Itadori Yuuji in the Church of Minamoto Kou.\nItadori was inspired by the anime/manga Jujutsu Kaisen (a.k.a. Sorcery Fight).\nItadori version 0.2 was made and developed by:\n**Tetsuki Syu#1250, Kirito#9286**"),
            footer = Some(OutgoingEmbedFooter("Itadori Bot: Release 0.2 | 2021-02-24")),
            thumbnail = Some(OutgoingEmbedThumbnail("https://cdn.discordapp.com/attachments/811517007446671391/813909301365833788/scala-spiral.png"))
        ))}
        .to(requests.sinkIgnore)
    }

  val ping = Command.namedParser(getPrefix("ping"))
    .described("Ping", "Returns latency.")
    .asyncOpt { implicit m =>
      import requestHelper._
      for {
        sentMsg <- run(m.textChannel.sendMessage("\uD83C\uDFD3 Pong!"))
        time = ChronoUnit.MILLIS.between(m.message.timestamp, sentMsg.timestamp)
        _ <- run(m.textChannel.sendMessage(s"$time ms between command and response."))
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
