package robocop.commands


import java.awt.Color

import javax.script.{ScriptEngine, ScriptEngineManager}
import net.dv8tion.jda.core.entities.{Member, Message, TextChannel, User}
import net.dv8tion.jda.core.{EmbedBuilder, JDA, MessageBuilder, Permission}
import robocop.database.Robobase
import robocop.listeners.Listener
import robocop.models.Command
import robocop.utils.Checks
import javax.script.ScriptContext

import scala.tools.nsc.interpreter.IMain

object Eval {

  case class EvalContext(jda: JDA, message: Message, db: Robobase, channel: TextChannel)

  def eval(engine: ScriptEngine, name: String, message: Message, db: Robobase): Unit = {
    try {
      engine.getContext.setAttribute("ctx", EvalContext(message.getJDA, message, db, message.getTextChannel), ScriptContext.ENGINE_SCOPE)

      val content = message.getContentRaw.substring(Listener.prefix.length + name.length + 1) match {
        case x if x.split("\n").head.startsWith("```") => x.split("\n").tail.filter(!_.startsWith("```")).mkString("\n")
        case x => x
      }

      val out = engine.eval(content)
      out match {
        case null => message.getChannel.sendMessage(new MessageBuilder().setEmbed(
          new EmbedBuilder()
            .setColor(Color.GREEN)
            .setTitle(s"$name output")
            .addField("Type", "<:yes:438228135004340224>", false)
            .build()
        ).build()).queue()
        case any => message.getChannel.sendMessage(new MessageBuilder().setEmbed(
          new EmbedBuilder()
            .setColor(Color.GREEN)
            .setTitle(s"$name output")
            .addField("Type", "<:yes:438228135004340224>", false)
            .addField(s"Output: ${out.getClass.getSimpleName}", s"```scala\n${out.toString}\n```", false)
            .build()
        ).build()).queue()
      }
    } catch {
      case e: Throwable => message.getChannel.sendMessage(new MessageBuilder().setEmbed(
        new EmbedBuilder()
          .setColor(Color.RED)
          .setTitle(s"$name output")
          .addField("Type", "<:no:438228135000145941>", false)
          .addField(s"Output: ${e.getClass.getSimpleName}", s"```scala\n${e.toString}\n```", false)
          .build()
      ).build()).queue()
    }
  }


  object EvalCommand extends Command {

    private lazy val engine = new ScriptEngineManager().getEngineByName("scala")

    override def name: String = "scaleval"

    override def help: String = "Evaluates a snippet of scala code"

    override def checkGuild(member: Member): Boolean = Checks.isOwner(member)

    override def checkDM(user: User): Boolean = Checks.isOwner(user)

    override def hidden: Boolean = true

    override def execute(args: Array[String], message: Message, db: Robobase): Option[String] = {
      eval(engine, "Scaleval", message, db)
      None
    }
  }

  object NashornEval extends Command {

    private lazy val engine = new ScriptEngineManager().getEngineByName("nashorn")

    override def name: String = "nashorn"

    override def help: String = "Evaluates a snippet of code using Nashorn"

    override def checkGuild(member: Member): Boolean = Checks.isOwner(member)

    override def checkDM(user: User): Boolean = Checks.isOwner(user)

    override def hidden: Boolean = true

    override def execute(args: Array[String], message: Message, db: Robobase): Option[String] = {
      eval(engine, "Nashorn", message, db)
      None
    }

  }

  object SqlEval extends Command {

    override def name: String = "sql"

    override def help: String = "Executes some SQL in the database."

    override def checkGuild(member: Member): Boolean = Checks.isOwner(member)

    override def checkDM(user: User): Boolean = Checks.isOwner(user)

    override def hidden: Boolean = true

    override def execute(args: Array[String], message: Message, db: Robobase): Option[String] = {
      val query = message.getContentRaw.substring(Listener.prefix.length + name.length + 1) match {
        case x if x.split("\n").head.startsWith("```") => x.split("\n").tail.filter(!_.startsWith("```")).mkString("\n")
        case x => x
      }
      try {
        val out = db.ctx.executeQuery(query)
        message.getChannel.sendMessage(new MessageBuilder().setEmbed(
          new EmbedBuilder()
            .setColor(Color.GREEN)
            .setTitle(s"SQL output")
            .addField("Type", "<:yes:438228135004340224>", false)
            .addField(s"Output: ${out.getClass.getSimpleName}", s"```scala\n${out.toString}\n```", false)
            .build()
        ).build()).queue()
      } catch {
        case e: Throwable if e.toString == "java.sql.SQLException: Query does not return results" => db.ctx.executeAction(query)
          message.getChannel.sendMessage(new MessageBuilder().setEmbed(
            new EmbedBuilder()
              .setColor(Color.GREEN)
              .setTitle(s"SQL output")
              .addField("Type", "<:yes:438228135004340224>", false)
              .build()
          ).build()).queue()
        case e: Throwable => message.getChannel.sendMessage(new MessageBuilder().setEmbed(
          new EmbedBuilder()
            .setColor(Color.RED)
            .setTitle(s"SQL output")
            .addField("Type", "<:no:438228135000145941> If update; ignore.", false)
            .addField(s"Output: ${e.getClass.getSimpleName}", s"```scala\n${e.toString}\n```", false)
            .build()
        ).build()).queue()
      }
      None
    }
  }

}
