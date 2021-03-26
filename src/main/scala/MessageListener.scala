import ackcord.syntax.TextChannelSyntax
import ackcord.{APIMessage, DiscordClient, EventsController}
import akka.NotUsed

import scala.util.Random

class MessageListener(client: DiscordClient) extends EventsController(client.requests) {
  val randomResponses = Vector("こりゃ！", "なんだ、{user}？", "俺は負けないぞ、{user}！", "応！ https://cdn.discordapp.com/emojis/812399512394924033.png", "ただいま、{user}！ https://cdn.discordapp.com/emojis/812399425783726081.png", "生き様で後悔はしたくない！ https://cdn.discordapp.com/emojis/812395447259496498.png",  "おはよう、{user}！ https://cdn.discordapp.com/emojis/812399281180901398.png")
  val onCreate: ackcord.EventListener[APIMessage.MessageCreate, NotUsed] = TextChannelEvent.on[APIMessage.MessageCreate].withSideEffects { implicit m =>
    val messageContent = m.event.message.content
    val selfMention = m.event.cache.current.botUser.mentionNick
    if (messageContent.contains(selfMention)) {
      val response = randomResponses(Random.between(0, randomResponses.length))
        .replace("{user}", m.event.message.authorUser(m.event.cache.current).get.mentionNick)
      import requestHelper._
      for {
        _ <- run(m.channel.sendMessage(response))
      } yield()
    }
  }
}