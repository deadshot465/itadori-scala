import ackcord.{CacheSnapshot, DiscordClient}
import ackcord.data.{Message, OutgoingEmbedThumbnail}

import scala.util.Try

object Utility {
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
}
