import ackcord.CacheSnapshot
import ackcord.data.{Message, OutgoingEmbedThumbnail}
import io.circe._
import io.circe.parser._
import io.circe.syntax.EncoderOps

import java.nio.charset.Charset
import java.nio.file.{Files, Path}
import scala.collection.mutable
import scala.util.Try

object Utility {
  var randomResponses: mutable.ListBuffer[String] = mutable.ListBuffer.empty
  private val randomResponsesPath = "assets/random_responses.json"

  def getUserAvatarUrl(userId: String, hash: String): String = {
    s"https://cdn.discordapp.com/avatars/$userId/$hash.png"
  }

  def getBotNameIdIcon(cache: CacheSnapshot): (String, String, String) = {
    val memberName = cache.botUser.username
    val userId = cache.botUser.id.toString
    val iconUrl = cache.botUser.avatar.fold("")(hash => getUserAvatarUrl(userId, hash))
    (memberName, userId, iconUrl)
  }

  def getUserNameIdIcon(message: Message, cache: CacheSnapshot): (String, String, String) = {
    val memberName = message.guildMember(cache).fold(message.authorUsername)(m => m.nick.getOrElse(message.authorUsername))
    val userId = message.authorUser(cache).fold("")(u => u.id.toString)
    val iconUrl = message.authorUser(cache).fold("")(m => m.avatar.fold("")(hash => getUserAvatarUrl(userId, hash)))
    (memberName, userId, iconUrl)
  }

  def getBotThumbnail(cache: CacheSnapshot): Option[OutgoingEmbedThumbnail] = {
    Try {
      val botId = cache.botUser.id.toString
      OutgoingEmbedThumbnail(getUserAvatarUrl(botId, cache.botUser.avatar.get))
    }.toOption
  }

  def readRandomResponsesFromLocal(): Unit = {
    val path = Path.of(randomResponsesPath)
    println(s"Random responses file path: ${path.toAbsolutePath}")
    val fileContent = Files.readString(path, Charset.forName("UTF-8"))
    val document = parse(fileContent).getOrElse(Json.Null)
    randomResponses = document.as[mutable.ListBuffer[String]].getOrElse(mutable.ListBuffer.empty)
  }

  def writeRandomResponsesToLocal(): Unit = {
    Files.writeString(Path.of(randomResponsesPath), randomResponses.asJson.toString(), Charset.forName("UTF-8"))
  }
}
