import ackcord.data.{EmbedField, OutgoingEmbed, OutgoingEmbedAuthor, OutgoingEmbedFooter, OutgoingEmbedThumbnail, TextChannelId}
import ackcord.requests.CreateMessage
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpMethods, HttpRequest}
import akka.http.scaladsl.unmarshalling.Unmarshal
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser._
import io.circe.{Decoder, Encoder, Json}

import java.nio.charset.Charset
import java.util.Base64
import scala.concurrent.{ExecutionContext, Future}

object RapidAPI {
  val submissionUrl = "https://judge0-ce.p.rapidapi.com/submissions?base64_encoded=true&fields=*"
  val scalaLangID = 81
  private val apiKey: String = System.getenv("RAPID_API_KEY")
  private val resultRawUrl = "https://judge0-ce.p.rapidapi.com/submissions/{token}?base64_encoded=true&fields=*"

  def generateAuthHeader(host: String): Seq[RawHeader] = {
    Seq(
      RawHeader("content-type", "application/json"),
      RawHeader("x-rapidapi-key", apiKey),
      RawHeader("x-rapidapi-host", host)
    )
  }

  def getEvalResult(header: Seq[RawHeader], token: String,
                    channelId: TextChannelId, authorName: String,
                    iconUrl: String)(implicit system: ActorSystem[Nothing], executionContext: ExecutionContext): Future[CreateMessage] = {
    val request = HttpRequest(
      method = HttpMethods.GET,
      uri = resultRawUrl.replace("{token}", token),
      headers = header
    )
    getResult(request, channelId, authorName, iconUrl)
  }

  private def getResult(request: HttpRequest, channelId: TextChannelId,
                        authorName: String, iconUrl: String)(implicit system: ActorSystem[Nothing],
                                                                                            executionContext: ExecutionContext): Future[CreateMessage] = {
    Http().singleRequest(request)
      .flatMap { response =>
        println(s"GET status code: ${response.status.value}")
        Unmarshal(response.entity).to[String]
      }
      .map { s =>
        val json = parse(s).getOrElse(Json.Null)
        json.as[EvalResultData]
      }
      .flatMap {
        case Left(value) =>
          Future.successful(CreateMessage.mkContent(channelId, s"何かエラーが発生していたらしいな：${value.getMessage()}"))
        case Right(value) =>
          if (value.stderr.isDefined && value.stderr.get != "") {
            var message = s"何かエラーが発生していたらしいな：${value.stderr.get}"
            if (value.message.isDefined && value.message.get != "") {
              message += s"\nちなみにこれは他のメッセージだ：${value.message.get}"
            }
            Future.successful(CreateMessage.mkContent(channelId, message))
          } else {
            if (value.stdout.isEmpty || value.stdout.getOrElse("") == "") {
              if (value.compile_output.isDefined && value.compile_output.get != "") {
                val decoded = Base64.getDecoder.decode(
                  value.compile_output.get.trim.replaceAll("\n", "")
                )
                var output = new String(decoded)
                if (output.length > 2047) {
                  output = output.substring(0, 2000)
                }
                Future.successful(CreateMessage
                .mkContent(channelId, s"わるい。俺がこれをコンパイルできなかった。これはエラーのメッセージだ。もしよかったら：$output"))
              } else {
                getResult(request, channelId, authorName, iconUrl)
              }
            } else {
              val decoded = Base64.getDecoder.decode(
                value.stdout.get.trim.replaceAll("\n", "")
              )
              val stdout = new String(decoded)
              var fields = scala.collection.mutable.Seq(
                EmbedField("費やす時間", s"${value.time.get} 秒", Some(true)),
                EmbedField("メモリー", s"${value.memory.get} KB", Some(true)),
              )
              if (value.exit_code.isDefined) {
                fields :+= EmbedField("エグジットコード", value.exit_code.get.toString, Some(true))
              }
              if (value.exit_signal.isDefined) {
                fields :+= EmbedField("エグジットシグナル", value.exit_signal.get.toString, Some(true))
              }
              val description = s"これは**${authorName}**のコードの解釈結果だ！\n```bash\n$stdout\n```"
              if (description.length >= 2047) {
                Future.successful(CreateMessage.mkContent(channelId, "ごめん！コードの解釈結果は長すぎそうだ！"))
              } else {
                val embed = OutgoingEmbed(
                  title = Some(""),
                  description = Some(description),
                  color = Some(Utility.itadoriColor),
                  thumbnail = Some(OutgoingEmbedThumbnail(Utility.scalaLogo)),
                  author = Some(OutgoingEmbedAuthor(authorName, iconUrl = Some(iconUrl))),
                  fields = fields.toSeq
                )
                Future.successful(CreateMessage.mkEmbed(channelId, embed))
              }
            }
          }
      }
  }

  case class RequestData(language_id: Int, source_code: String)
  implicit val rapidAPIRequestDataEncoder: Encoder[RequestData] = deriveEncoder

  case class EvalResultData(stdout: Option[String], stderr: Option[String],
                            message: Option[String], memory: Option[Float],
                            time: Option[Float], exit_code: Option[Int],
                            exit_signal: Option[Int], compile_output: Option[String])
  implicit val rapidAPIEvalResultDecoder: Decoder[EvalResultData] = deriveDecoder
}
