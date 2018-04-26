package robocop.commands

import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities._
import robocop.database.Robobase
import robocop.listeners.Listener
import robocop.models.{BotError, Command}
import robocop.utils.Checks

object Lockdown {

  var channels: Map[String, Array[(Role, Long, Long)]] = Map()

  def lock(channel: TextChannel, message: String): Unit = {
    if (channel.getGuild.getSelfMember.hasPermission(channel, Permission.MANAGE_CHANNEL)) {
      val overrideObj = channel.getPermissionOverride(channel.getGuild.getPublicRole)
      if ((overrideObj.getDeniedRaw & 2112) == 2112) {
        channel.sendMessage(s"🔏 Channel was already locked. Use `${Listener.prefix}unlock` to unlock the channel.").queue()
      } else {
        channels += (channel.getId -> channel.getRolePermissionOverrides.toArray(Array[PermissionOverride]()).map(x => (x.getRole, x.getAllowedRaw, x.getDeniedRaw)))
        val manager = channel.getManager
        channel.getRolePermissionOverrides.toArray(Array[PermissionOverride]())
          .filter(x => !(x.getRole.hasPermission(Permission.MESSAGE_MANAGE) || x.getAllowed.contains(Permission.MESSAGE_MANAGE)))
          .foreach(x => manager.putPermissionOverride(x.getRole, x.getAllowedRaw - (x.getAllowedRaw & 2112), x.getDeniedRaw | 2112))
        manager.queue(_ => channel.sendMessage(message).queue())
      }
    } else {
      throw new BotError.NoPermissions(Permission.MANAGE_CHANNEL)
    }
  }

  def unlock(channel: TextChannel, message: String): Unit = {
    if (channel.getGuild.getSelfMember.hasPermission(channel, Permission.MANAGE_PERMISSIONS)) {
      if (!channels.contains(channel.getId)) {
        channel.sendMessage("🔏 Channel was already unlocked.").queue()
        return
      }
      val overrides = channels(channel.getId)
      val manager = channel.getManager
      overrides.foreach(x => manager.putPermissionOverride(x._1, x._2, x._3))
      manager.queue(_ => channel.sendMessage(message).queue())
      channels -= channel.getId
    } else {
      throw new BotError.NoPermissions(Permission.MANAGE_PERMISSIONS)
    }
  }

  object Lock extends Command {
    override def name: String = "lock"

    override def help: String = "Locks the current channel, so only the team has access to it."

    override def checkGuild(member: Member): Boolean = Checks.standardMod(member)

    override def execute(args: Array[String], message: Message, db: Robobase): Option[String] = {
      lock(message.getTextChannel, "🔒 Channel locked down. Only staff members may speak. Do not bring the topic to other channels or risk disciplinary actions.")
      Some(s"🔒 `${message.getAuthor.getName}#${message.getAuthor.getDiscriminator}` locked down ${message.getTextChannel.getAsMention}")
    }
  }

  object SilentLock extends Command {
    override def name: String = "softlock"

    override def help: String = "Locks the current channel, so only the team has access to it. Leaves out the warning."

    override def checkGuild(member: Member): Boolean = Checks.standardMod(member)

    override def execute(args: Array[String], message: Message, db: Robobase): Option[String] = {
      lock(message.getTextChannel, "🔒 Channel locked down. Only staff members may speak.")
      Some(s"🔒 `${message.getAuthor.getName}#${message.getAuthor.getDiscriminator}` softly locked down ${message.getTextChannel.getAsMention}")
    }
  }

  object Unlock extends Command {
    override def name: String = "unlock"

    override def logline(channel: TextChannel, member: Member): String = s"🔓 `${member.getUser.getName}#${member.getUser.getDiscriminator}` unlocked ${channel.getAsMention}"

    override def help: String = "Unlocks the current channel."

    override def checkGuild(member: Member): Boolean = Checks.standardMod(member)

    override def execute(args: Array[String], message: Message, db: Robobase): Option[String] = {
      unlock(message.getTextChannel, "🔓 Channel unlocked.")
      Some(s"🔓 `${message.getAuthor.getName}#${message.getAuthor.getDiscriminator}` unlocked ${message.getTextChannel.getAsMention}")
    }
  }

}
