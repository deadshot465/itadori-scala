import ackcord._
import ackcord.commands.PrefixParser
import ackcord.data.PresenceStatus
import ackcord.data.raw.RawActivity
import ackcord.gateway.{GatewayIntents, StatusData, StatusUpdate}
import akka.stream.scaladsl.Source

import java.time.Instant
import scala.concurrent.duration.DurationInt
import scala.util.Random

object Main {
  def main(args: Array[String]): Unit = {
    val token = System.getenv("TOKEN")
    val presences = Vector("呪霊狩り", "呪力", "逕庭拳", "黒閃", "屠坐魔", "マンガ", "ゲーム", "呪術練習")
    val initialPresence = presences(Random.between(0, presences.length))
    Utility.readRandomResponsesFromLocal()
    val clientSettings = ClientSettings(token, activity = Some(RawActivity(initialPresence, 0, None, Instant.now(), None, None,
      None, None, None, None, None)), intents = GatewayIntents.All)

    import clientSettings.executionContext

    clientSettings.createClient()
      .foreach { client =>
        import client.system
        client.onEventSideEffectsIgnore {
          case APIMessage.Ready(cache) =>
            printf("%s#%s is now online.\n", cache.current.botUser.username, cache.current.botUser.discriminator)
            client.events.sendGatewayPublish.runWith(Source.tick(1.second, 1.hour, ())
              .map(_ => StatusUpdate(StatusData(
                None,
                Some(
                  RawActivity(presences(Random.between(0, presences.length)),
                    0,
                    None,
                    Instant.now(),
                    None, None, None, None, None, None, None)
                ), PresenceStatus.Online, afk = false))))
        }

        // import client.requestsHelper._

        val messageListener = new MessageListener(client)
        val itadoriCommands = new ItadoriCommands(client, client.requests)
        val itadoriHelpCommand = new ItadoriHelpCommand(client)
        client.registerListener(messageListener.onCreate)
        client.commands.runNewCommand(
          PrefixParser.structured(needsMention = false, Seq("i?"), Seq("help")),
          itadoriHelpCommand.command
        )
        client.commands.bulkRunNamedWithHelp(
          itadoriHelpCommand,
          itadoriCommands.ping,
          itadoriCommands.about,
          itadoriCommands.response)
        client.login()
      }
  }
}
