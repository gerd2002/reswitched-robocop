package robocop.models

import net.dv8tion.jda.core.MessageBuilder
import net.dv8tion.jda.core.entities.{Member, Message, TextChannel, User}
import net.dv8tion.jda.core.requests.restaction.MessageAction
import robocop.database.Robobase

trait Command {

  def name: String
  def help: String
  def aliases: List[String] = List()
  def hidden: Boolean = false
  def beta: Boolean = false
  def logline(channel: TextChannel, member: Member): String = ""
  def checkGuild(member: Member): Boolean = true
  def checkDM(user: User): Boolean = true
  def execute(args: Array[String], message: Message, db: Robobase): Option[String]

  implicit class MessageExtensions(message: Message) {
    private def respondAction(content: Message): MessageAction = message.getChannel.sendMessage(content)
    def respond(content: Message): Unit = respondAction(content).queue()
    def respondMention(content: Message): Unit = respond(s"${message.getAuthor.getAsMention} $content")
    def respondDM(content: Message): Unit = message.getAuthor.openPrivateChannel().complete().sendMessage(content).queue()
    private def respondAction(content: String): MessageAction = message.getChannel.sendMessage(content)
    def respond(content: String): Unit = respondAction(content).queue()
    def respondMention(content: String): Unit = respond(s"${message.getAuthor.getAsMention} $content")
    def respondDM(content: String): Unit = message.getAuthor.openPrivateChannel().complete().sendMessage(content).queue()
  }

  println(s"Registered $name")

}
