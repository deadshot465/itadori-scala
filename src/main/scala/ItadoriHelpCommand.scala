import ackcord.{CacheSnapshot, DiscordClient}
import ackcord.commands.HelpCommand
import ackcord.data.{EmbedField, Message, OutgoingEmbed, OutgoingEmbedAuthor, OutgoingEmbedFooter, OutgoingEmbedThumbnail}
import ackcord.requests.{CreateMessageData, Requests}

import scala.concurrent.Future

class ItadoriHelpCommand(client: DiscordClient) extends HelpCommand(client.requests) {
  override def createSearchReply(message: Message, query: String, matches: Seq[HelpCommand.HelpCommandProcessedEntry])(implicit c: CacheSnapshot): Future[CreateMessageData] = {
    val (memberName, _, iconUrl) = Utility.getUserNameIdIcon(message, c)
    Future.successful(CreateMessageData(embed = Some(OutgoingEmbed(
      author = Some(OutgoingEmbedAuthor(memberName, iconUrl = Some(iconUrl))),
      description = Some(s"Command matching: `$query`"),
      color = Some(0xD6A09A),
      thumbnail = Utility.getBotThumbnail(c),
      fields = matches.map(createContent(_))
    ))))
  }

  override def createReplyAll(message: Message, page: Int)(implicit c: CacheSnapshot): Future[CreateMessageData] = {
    if (page <= 0) {
      Future.successful(CreateMessageData(embed = Some(OutgoingEmbed(description = Some("Invalid page")))))
    } else {
      Future.traverse(registeredCommands.toSeq) { entry =>
        entry.prefixParser
          .canExecute(c, message)
          .zip(entry.prefixParser.needsMention(c, message))
          .zip(entry.prefixParser.symbols(c, message))
          .zip(entry.prefixParser.aliases(c, message))
          .zip(entry.prefixParser.caseSensitive(c, message))
          .map(entry -> _)
      }
        .map { entries =>
          val commandSlice = entries
            .collect {
              case (entry, ((((canExecute, needsMention), symbols), aliases), caseSensitive)) if canExecute =>
                HelpCommand.HelpCommandProcessedEntry(needsMention, symbols, aliases, caseSensitive, entry.description)
            }
            .sortBy(e => (e.symbols.head, e.aliases.head))
            .slice((page - 1) * 10, page * 10)

          val maxPages = Math.max(Math.ceil(registeredCommands.size / 10D).toInt, 1)
          if (commandSlice.isEmpty) {
            CreateMessageData(s"ページ数：$maxPages")
          } else {
            val (memberName, _, iconUrl) = Utility.getUserNameIdIcon(message, c)
            CreateMessageData(
              embed = Some(
                OutgoingEmbed(
                  author = Some(OutgoingEmbedAuthor(memberName, iconUrl = Some(iconUrl))),
                  color = Some(0xD6A09A),
                  description = Some("以下は虎杖のコマンドリスト。"),
                  thumbnail = Utility.getBotThumbnail(c),
                  fields = commandSlice.map(createContent(_)),
                  footer = Some(OutgoingEmbedFooter(s"ページ：$page/$maxPages"))
                )
              )
            )
          }
        }
    }
  }

  private def createContent(entry: HelpCommand.HelpCommandProcessedEntry)(implicit c: CacheSnapshot): EmbedField = {
    val invocation = {
      val mention = if (entry.needsMention) s"${c.botUser.mention}" else ""
      val symbol = if (entry.symbols.length > 1) entry.symbols.mkString("(", "|", ")") else entry.symbols.head
      val alias = if (entry.aliases.length > 1) entry.aliases.mkString("(", "|", ")") else entry.aliases.head

      mention + symbol + alias
    }

    val builder = new StringBuilder
    builder.append(s"__コマンド名__: ${entry.description.name}\n")
    builder.append(s"__説明__: ${entry.description.description}\n")
    builder.append(s"__サンプル__: `$invocation ${entry.description.usage}`\n")

    EmbedField(entry.description.name, builder.mkString)
  }
}
