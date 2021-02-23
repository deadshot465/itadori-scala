import ackcord.APIMessage.Ready
import ackcord.syntax.TextChannelSyntax
import ackcord.{APIMessage, DiscordClient, EventsController}
import akka.NotUsed

class MessageListener(client: DiscordClient) extends EventsController(client.requests) {
  val onCreate: ackcord.EventListener[APIMessage.MessageCreate, NotUsed] = TextChannelEvent.on[APIMessage.MessageCreate].withSideEffects { implicit m =>

  }
}