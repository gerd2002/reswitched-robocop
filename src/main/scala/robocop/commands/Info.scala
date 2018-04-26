package robocop.commands

import net.dv8tion.jda.core.entities.{Member, Message}
import robocop.database.Robobase
import robocop.listeners.Listener
import robocop.models.Command
import robocop.utils.Checks

object Info {

  object About extends Command {
    override def name: String = "about"

    override def help: String = "Displays information about the bot."

    override def execute(args: Array[String], message: Message, db: Robobase): Option[String] = {
      message.respondMention("Robocop, but in scala. Contact Gerd#8888 for more details.")
      None
    }
  }

  object Help extends Command {
    override def name: String = "help"

    override def help: String = "List all commands and provides contextual help."

    override def hidden: Boolean = true

    override def execute(args: Array[String], message: Message, db: Robobase): Option[String] = {
      if (args.length == 0) {
        message.respond(
          s"""
             |```
             |${Listener.commandList.filter(!_.hidden).filter(!_.beta).map(helpline).mkString("\n")}

             |```
        """.stripMargin)
      } else {
        val filtered = Listener.commandList.filter(_.name == args.head)
        if (filtered.isEmpty) {
          message.respond("⚠️ No matches.")
        } else {
          message.respond(
            s"""
               |```
               |${filtered.map(helpline).mkString("\n")}

               |```
            """.stripMargin)
        }
      }
      None
    }

    private def helpline(command: Command): String = {
      s"${command.name}${" " * (10 - command.name.length)} - ${" " * 4}${command.help}"
    }
  }

  object Ping extends Command {
    override def name: String = "ping"

    override def help: String = "Returns the current ping of the bot."

    override def checkGuild(member: Member): Boolean = Checks.standardTrust(member)

    override def execute(args: Array[String], message: Message, db: Robobase): Option[String] = {
      message.respondMention(s"🏓 Pong! ${message.getJDA.getPing}ms")
      None
    }
  }

  object Created extends Command {
    override def name: String = "created"

    override def help: String = "Returns when a user was created; Years are calculated with 365 days."

    override def checkGuild(member: Member): Boolean = Checks.standardTrust(member)

    override def execute(args: Array[String], message: Message, db: Robobase): Option[String] = {
      message.respondMention(s"🕔 Created at `${message.getAuthor.getCreationTime}` (${Listener.agoString(message.getAuthor)} ago)")
      None
    }
  }

}
