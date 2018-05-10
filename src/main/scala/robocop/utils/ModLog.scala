package robocop.utils

import java.time.OffsetDateTime

import net.dv8tion.jda.core.entities.{Guild, IMentionable, TextChannel, User}
import robocop.Main
import robocop.database.Robobase
import robocop.listeners.Listener
import robocop.models.Configuration

object ModLog {

  val db = new Robobase

  sealed abstract case class ActionType(symbol: String, name: String, verb: String)

  object ActionType {

    object Ban extends ActionType("🔨", "Ban", "was banned")

    object Forceban extends ActionType("🔫", "Forceban", "was force-banned")

    object Unban extends ActionType("🌂", "Unban", "was unbanned")

    object Kick extends ActionType("👢", "Kick", "was kicked")

    object Leave extends ActionType("📤", "Leave", "has left the server. Joined")

    object Join extends ActionType("📥", "Join", "has joined the server. Created")

  }

  def logJoinLeave(guild: Guild, message: String): Unit = {
    val config = db.config(guild.getIdLong) match {
      case Some(cfg) => cfg
      case None =>
        val config = Configuration.default(guild.getIdLong)
        db.config += config
        config
    }
    config.joinlogChannel match {
      case Some(x) => val channel = Main.shardManager.getTextChannelById(x)
        if (channel != null)
          channel.sendMessage(message).queue()
      case None =>
    }
  }

  def log(guild: Guild, message: String): Unit = {
    val config = db.config(guild.getIdLong) match {
      case Some(cfg) => cfg
      case None =>
        val config = Configuration.default(guild.getIdLong)
        db.config += config
        config
    }
    config.modlogChannel match {
      case Some(x) => val channel = Main.shardManager.getTextChannelById(x)
        if (channel != null)
          channel.sendMessage(message).queue()
      case None =>
    }
  }

  object UserLogs {

    object Loglines {
      def logline(action: ActionType, user: User): String = {
        s"${action.symbol} ${user.getAsMention} `(${user.getName}#${user.getDiscriminator} | ${user.getId})` ${action.verb}"
      }

      def logline(action: ActionType, user: User, mod: User): String = {
        s"${logline(action, user)} by ${mod.getAsMention}"
      }

      def logline(action: ActionType, user: User, reason: String): String = {
        s"${logline(action, user)} with reason `$reason`"
      }

      def logline(action: ActionType, user: User, mod: User, reason: String): String = {
        s"${logline(action, user, mod)} with reason `$reason`"
      }

      def logline(action: ActionType, user: User, time: OffsetDateTime): String = {
        s"${logline(action, user)} at `${time.toString}` (${Listener.agoString(time)} ago)"
      }

      def logline(action: ActionType, user: String): String = {
        s"${action.symbol} User with ID $user ${action.verb}"
      }

      def logline(action: ActionType, user: String, mod: User): String = {
        s"${logline(action, user)} by ${mod.getAsMention}"
      }

      def logline(action: ActionType, user: String, reason: String): String = {
        s"${logline(action, user)} with reason `$reason`"
      }

      def logline(action: ActionType, user: String, mod: User, reason: String): String = {
        s"${logline(action, user, mod)} with reason `$reason`"
      }

      def logline(action: ActionType, user: String, time: OffsetDateTime): String = {
        s"${logline(action, user)} at `${time.toString}` (${Listener.agoString(time)} ago)"
      }
    }

    def log(action: ActionType, guild: Guild, user: User, mod: User, reason: String): Unit = {
      ModLog.log(guild, Loglines.logline(action, user, mod, reason))
    }

    def log(action: ActionType, guild: Guild, user: User, mod: User): Unit = {
      ModLog.log(guild, Loglines.logline(action, user, mod))
    }

    def log(action: ActionType, guild: Guild, user: User): Unit = {
      ModLog.log(guild, Loglines.logline(action, user))
    }

    def log(action: ActionType, guild: Guild, user: User, reason: String): Unit = {
      ModLog.log(guild, Loglines.logline(action, user, reason))
    }

    def log(action: ActionType, guild: Guild, user: User, time: OffsetDateTime): Unit = {
      ModLog.log(guild, Loglines.logline(action, user, time))
    }

    def logJoinLeave(action: ActionType, guild: Guild, user: User, time: OffsetDateTime): Unit = {
      ModLog.logJoinLeave(guild, Loglines.logline(action, user, time))
    }

    def logJoinLeave(action: ActionType, guild: Guild, user: User): Unit = {
      ModLog.logJoinLeave(guild, Loglines.logline(action, user))
    }
  }

}
