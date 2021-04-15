import akka.http.scaladsl.model.headers.RawHeader
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

object RapidAPI {
  val submissionUrl = "https://judge0-ce.p.rapidapi.com/submissions?base64_encoded=true&fields=*"
  val scalaLangID = 81
  private val apiKey: String = System.getenv("RAPID_API_KEY")

  def generateAuthHeader(host: String): Seq[RawHeader] = {
    Seq(
      RawHeader("content-type", "application/json"),
      RawHeader("x-rapidapi-key", apiKey),
      RawHeader("x-rapidapi-host", host)
    )
  }

  case class RequestData(language_id: Int, source_code: String)
  implicit val rapidAPIRequestDataEncoder: Encoder[RequestData] = deriveEncoder
}
