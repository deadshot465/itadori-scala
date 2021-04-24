import ackcord.DiscordClient
import ackcord.commands.MessageParser.RemainingAsString
import ackcord.commands._
import ackcord.data.{OutgoingEmbed, OutgoingEmbedAuthor, OutgoingEmbedFooter, OutgoingEmbedThumbnail, Permission}
import ackcord.requests.{CreateMessage, Requests}
import ackcord.syntax.TextChannelSyntax
import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.Flow
import io.circe._
import io.circe.parser._
import io.circe.syntax.EncoderOps

import java.nio.charset.Charset
import java.time.temporal.ChronoUnit
import java.util.Base64
import scala.collection.immutable.HashMap
import scala.concurrent.Future
import scala.util.{Failure, Success}

class ItadoriCommands(client: DiscordClient, requests: Requests) extends CommandController(requests) {
  private val ElevatedCommand = GuildCommand.andThen(
    CommandBuilder.needPermission[GuildUserCommandMessage](Permission.Administrator)
  )

  val about: NamedDescribedComplexCommand[NotUsed, NotUsed] = Command.namedParser(getPrefix("about"))
    .described("About", "虎杖悠仁の情報を返す。")
    .toSink {
      Flow[CommandMessage[NotUsed]]
        .map { m =>
          val (_, _, iconUrl) = Utility.getBotNameIdIcon(m.cache)
          CreateMessage.mkEmbed(m.message.channelId, OutgoingEmbed(
            author = Some(OutgoingEmbedAuthor("呪術廻戦の虎杖悠仁", iconUrl = Some(iconUrl))),
            color = Some(Utility.itadoriColor),
            description = Some("The Land of Cute Boisの虎杖悠仁。\n虎杖はアニメ・マンガ「[呪術廻戦]()」の主人公です。\n虎杖バージョン0.6.2の開発者：\n**Tetsuki Syu#1250、Kirito#9286**\n制作言語・フレームワーク：\n[Scala](https://www.scala-lang.org/)と[Ackcord](https://ackcord.katsstuff.net/)ライブラリ。"),
            footer = Some(OutgoingEmbedFooter("虎杖ボット：リリース 0.6.2 | 2021-04-25")),
            thumbnail = Some(OutgoingEmbedThumbnail(Utility.scalaLogo))
        ))}
        .to(requests.sinkIgnore)
    }

  val eval: NamedDescribedComplexCommand[RemainingAsString, NotUsed] = Command.namedParser(getPrefix("eval"))
    .described("Eval", "Scalaコードを解釈します。")
    .parsing(MessageParser[RemainingAsString])
    .asyncOpt { implicit m =>
      val commandPrefix = "i?eval "
      val code = m.message.content.substring(commandPrefix.length).split("\n")
      val actualCode = code.slice(1, code.length - 1).mkString("\n")
      val requestData = RapidAPI.RequestData(
        RapidAPI.scalaLangID,
        Base64.getEncoder.encodeToString(actualCode.getBytes(Charset.forName("UTF-8")))
      ).asJson.toString()

      val authHeader = RapidAPI.generateAuthHeader("judge0-ce.p.rapidapi.com")

      val request = HttpRequest(
        method = HttpMethods.POST,
        uri = RapidAPI.submissionUrl,
        headers = authHeader,
        entity = HttpEntity(ContentTypes.`application/json`, requestData)
      )

      implicit val system: ActorSystem[Nothing] = client.system
      import requestHelper._

      Http().singleRequest(request).onComplete {
        case Success(value) =>
          println(s"POST status code: ${value.status.value}")
          var stringData: String = ""
          Unmarshal(value.entity).to[String].onComplete {
            case Success(v) =>
              stringData = v
              val responseData = parse(stringData)
                .getOrElse(Json.Null)
                .as[HashMap[String, String]]
                .getOrElse(HashMap.empty)
              val token = responseData("token")
              println(s"Token: $token")
              val (userName, _, iconUrl) = Utility.getUserNameIdIcon(m.message, m.cache)
              RapidAPI.getEvalResult(authHeader, token, m.textChannel.id, userName, iconUrl)
                .foreach { res =>
                  for {
                    _ <- run(res)
                  } yield()
                }
            case Failure(e) =>
              println(s"Error occurred when sending the request: ${e.getMessage}")
              stringData = e.getMessage
          }
        case Failure(e) =>
          println(s"Error occurred when sending the request: ${e.getMessage}")
      }

      for {
        _ <- run(m.textChannel.sendMessage("リクエスト取ったよ！ちょっと待ってくれ…"))
      } yield()
    }

  val ping: NamedDescribedComplexCommand[NotUsed, NotUsed] = Command.namedParser(getPrefix("ping"))
    .described("Ping", "レイテンシを返す。")
    .asyncOpt { implicit m =>
      import requestHelper._
      for {
        sentMsg <- run(m.textChannel.sendMessage("\uD83C\uDFD3 ポン！"))
        time = ChronoUnit.MILLIS.between(m.message.timestamp, sentMsg.timestamp)
        _ <- run(m.textChannel.sendMessage(s"コマンドとレスポンスの間に${time}ミリ秒が経ちました。"))
      } yield()
    }

  val response: NamedDescribedComplexCommand[_, NotUsed] = ElevatedCommand.namedParser(getPrefix("response"))
    .described("Response", "虎杖の返事を変更する。")
    .parsing(MessageParser[RemainingAsString])
    .withRequest { m =>
      val split = m.parsed.remaining.split(" ")
      val cmd = split(0).toLowerCase
      val message = split.drop(1).mkString(" ")
      cmd match {
        case "add" =>
          Utility.randomResponses += message
          Utility.writeRandomResponsesToLocal()
          m.textChannel.sendMessage("わかったよ！そうしよう！")

        case "remove" =>
          if (Utility.randomResponses.contains(message)) {
            Utility.randomResponses -= message
            Utility.writeRandomResponsesToLocal()
            m.textChannel.sendMessage("わかったよ！遠慮しとく！")
          } else {
            m.textChannel.sendMessage("何言ってんの？俺はそんなこと知らない！")
          }
      }
    }

  private def getPrefix(aliases: String*): StructuredPrefixParser = {
    PrefixParser.structuredAsync(
      (c, m) => m.guild(c).fold(Future.successful(false))(_ => Future.successful(false)),
      (c, m) => m.guild(c).fold(Future.successful(Seq("i?")))(_ => Future.successful(Seq("i?"))),
      (_, _) => Future.successful(aliases)
    )
  }
}
