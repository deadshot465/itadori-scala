import Utility.randomResponses
import ackcord.syntax.TextChannelSyntax
import ackcord.{APIMessage, DiscordClient, EventsController}
import akka.NotUsed

import scala.util.Random

class MessageListener(client: DiscordClient) extends EventsController(client.requests) {
  val onCreate: ackcord.EventListener[APIMessage.MessageCreate, NotUsed] = TextChannelEvent.on[APIMessage.MessageCreate].withSideEffects { implicit m =>
    val messageContent = m.event.message.content
    val selfMention = m.event.cache.current.botUser.id.asString
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