import ackcord._
import ackcord.commands.PrefixParser

object Main {
  def main(args: Array[String]): Unit = {
    val token = System.getenv("TOKEN")
    val clientSettings = ClientSettings(token)
    import clientSettings.executionContext

    clientSettings.createClient()
      .foreach { client =>
        client.onEventSideEffectsIgnore {
          case APIMessage.Ready(cache) => printf("%s#%s is now online.\n", cache.current.botUser.username, cache.current.botUser.discriminator)
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
          itadoriCommands.about)
        client.login()
      }
  }
}
