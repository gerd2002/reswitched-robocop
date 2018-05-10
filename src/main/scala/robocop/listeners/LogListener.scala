package robocop.listeners

import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.audit.{ActionType, AuditLogEntry}
import net.dv8tion.jda.core.entities.{Guild, Message, PrivateChannel, Role}
import net.dv8tion.jda.core.events.channel.category.update.CategoryUpdateNameEvent
import net.dv8tion.jda.core.events.channel.category.{CategoryCreateEvent, CategoryDeleteEvent}
import net.dv8tion.jda.core.events.channel.text.update.TextChannelUpdateNameEvent
import net.dv8tion.jda.core.events.channel.text.{TextChannelCreateEvent, TextChannelDeleteEvent}
import net.dv8tion.jda.core.events.channel.voice.update.VoiceChannelUpdateNameEvent
import net.dv8tion.jda.core.events.channel.voice.{VoiceChannelCreateEvent, VoiceChannelDeleteEvent}
import net.dv8tion.jda.core.events.guild.GuildUnbanEvent
import net.dv8tion.jda.core.events.guild.member._
import net.dv8tion.jda.core.events.message.MessageBulkDeleteEvent
import net.dv8tion.jda.core.events.message.guild.{GuildMessageDeleteEvent, GuildMessageReceivedEvent, GuildMessageUpdateEvent}
import net.dv8tion.jda.core.hooks.ListenerAdapter
import robocop.database.Robobase
import robocop.listeners.Listener.ago
import robocop.models.Configuration
import robocop.utils.ModLog
import robocop.utils.ModLog.{UserLogs, ActionType => AcTyp}

import scala.collection.immutable.Queue
import scala.language.postfixOps

object LogListener {
  var messageCache: Map[Long, Queue[(Long, Long, Long, String)]] = Map()
}

class LogListener(shardId: Int) extends ListenerAdapter {

  private val db = new Robobase()
  private val shard = shardId + 1

  override def onGuildMessageReceived(event: GuildMessageReceivedEvent): Unit = {
    try {
      val guildId = event.getGuild.getIdLong
      if (event.getAuthor != event.getJDA.getSelfUser) {
        var queue: Queue[(Long, Long, Long, String)] = Queue()
        if (LogListener.messageCache.contains(guildId)) {
          queue = LogListener.messageCache(guildId)
          LogListener.messageCache = LogListener.messageCache.filter(_._1 != guildId)
        }
        if (event.getMessage.getContentRaw != null) {
          val message = event.getMessage
          queue = queue.enqueue((message.getIdLong, message.getAuthor.getIdLong, message.getChannel.getIdLong, message.getContentRaw))
          if (LogListener.messageCache.size >= 1000) {
            LogListener.messageCache += (guildId -> queue.tail)
          } else {
            LogListener.messageCache += (guildId -> queue)
          }
        }
      }
    } catch {
      case e: Throwable => println(e)
    }
  }

  override def onGuildMessageDelete(event: GuildMessageDeleteEvent): Unit = {
    try {
      val guildId = event.getGuild.getIdLong
      if (LogListener.messageCache.contains(guildId) && LogListener.messageCache(guildId).exists(_._1 == event.getMessageIdLong)) {
        val (_, authorId, channelId, content) = LogListener.messageCache(guildId).find(_._1 == event.getMessageIdLong).get
        val user = event.getJDA.getUserById(authorId)
        if (user != null) {
          ModLog.log(event.getGuild, s"🗑 Message by `(${user.getName}#${user.getDiscriminator} | ${user.getId})` in <#$channelId> was deleted. "
            + s"Content:\n```\n$content\n```")
        } else {
          ModLog.log(event.getGuild, s"🗑 Message by User with ID $authorId in <#$channelId> was deleted. "
            + s"Content:\n```\n$content\n```")
        }
      } else {
        //ModLog.log(event.getGuild, s"🗑 Message ${event.getMessageIdLong} was deleted, uncached.")
      }
    } catch {
      case e: Throwable => println(e)
    }
  }

  override def onGuildMessageUpdate(event: GuildMessageUpdateEvent): Unit = {
    try {
      val guildId = event.getGuild.getIdLong
      if (LogListener.messageCache.contains(guildId) && LogListener.messageCache(guildId).exists(_._1 == event.getMessageIdLong)) {
        val tuple = LogListener.messageCache(guildId).find(_._1 == event.getMessageIdLong).get
        val (messageId, authorId, channelId, content) = tuple
        val user = event.getJDA.getUserById(authorId)

        val queue = LogListener.messageCache(guildId)
        val (before, after) = queue.splitAt(queue.indexOf(tuple))
        val newQueue = (before :+ Tuple4(messageId, authorId, channelId, event.getMessage.getContentRaw)) ++: after.filter(!_.equals(tuple))
        LogListener.messageCache = LogListener.messageCache.filter(_._1 != guildId)
        LogListener.messageCache += (guildId -> newQueue)

        if (user != null) {
          ModLog.log(event.getGuild, s"📝 Message by `(${user.getName}#${user.getDiscriminator} | ${user.getId})` in <#$channelId> was edited.\n "
            + s"Old:\n```\n$content\n```"
            + s"New:\n```\n${event.getMessage.getContentRaw}\n```"
          )
        } else {
          ModLog.log(event.getGuild, s"📝 Message by User with ID $authorId in <#$channelId> was edited.\n "
            + s"Old:\n```\n$content\n```"
            + s"New:\n```\n${event.getMessage.getContentRaw}\n```"
          )
        }
      } else {
        ModLog.log(event.getGuild, s"📝 Message ${event.getMessageIdLong} was edited, uncached.")
      }
    } catch {
      case e: Throwable => println(e)
    }
  }

  override def onCategoryDelete(event: CategoryDeleteEvent): Unit = {
    ModLog.log(event.getGuild, s"🗑 Deleted category `${event.getCategory.getName}`")
  }

  override def onCategoryCreate(event: CategoryCreateEvent): Unit = {
    ModLog.log(event.getGuild, s"📂 Created category `${event.getCategory.getName}`")
  }

  override def onCategoryUpdateName(event: CategoryUpdateNameEvent): Unit = {
    ModLog.log(event.getGuild, s"✏️ Renamed category `${event.getCategory.getName}` (was `${event.getOldName}`)")
  }

  override def onTextChannelDelete(event: TextChannelDeleteEvent): Unit = {
    ModLog.log(event.getGuild, s"🗑 Deleted text channel `#${event.getChannel.getName}`")
  }

  override def onTextChannelCreate(event: TextChannelCreateEvent): Unit = {
    ModLog.log(event.getGuild, s"📂 Created text channel `#${event.getChannel.getName}`")
  }

  override def onTextChannelUpdateName(event: TextChannelUpdateNameEvent): Unit = {
    ModLog.log(event.getGuild, s"✏️ Renamed text channel `#${event.getChannel.getName}` (was `#${event.getOldName}`)")
  }

  override def onVoiceChannelDelete(event: VoiceChannelDeleteEvent): Unit = {
    ModLog.log(event.getGuild, s"🗑 Deleted voice channel `#${event.getChannel.getName}`")
  }

  override def onVoiceChannelCreate(event: VoiceChannelCreateEvent): Unit = {
    ModLog.log(event.getGuild, s"📂 Created voice channel `#${event.getChannel.getName}`")
  }

  override def onVoiceChannelUpdateName(event: VoiceChannelUpdateNameEvent): Unit = {
    ModLog.log(event.getGuild, s"✏️ Renamed voice channel `#${event.getChannel.getName}` (was `#${event.getOldName}`)")
  }

  override def onGuildMemberNickChange(event: GuildMemberNickChangeEvent): Unit = {
    ModLog.log(event.getGuild, s"🔮️ `${event.getMember.getEffectiveName}` `(${event.getUser.getName}#${event.getUser.getDiscriminator})` changed nickname (was `${event.getPrevNick}`)")
  }

  override def onGuildMemberRoleAdd(event: GuildMemberRoleAddEvent): Unit = {
    ModLog.log(event.getGuild, s"🔧 `${event.getMember.getEffectiveName}` `(${event.getUser.getName}#${event.getUser.getDiscriminator})` got role(s) ${event.getRoles.toArray().map(_.asInstanceOf[Role].getName).mkString(", ")}")
  }

  override def onGuildMemberRoleRemove(event: GuildMemberRoleRemoveEvent): Unit = {
    ModLog.log(event.getGuild, s"🔧 `${event.getMember.getEffectiveName}` `(${event.getUser.getName}#${event.getUser.getDiscriminator})` lost role(s) ${event.getRoles.toArray().map(_.asInstanceOf[Role].getName).mkString(", ")}")
  }

  override def onGuildUnban(event: GuildUnbanEvent): Unit = {
    UserLogs.log(AcTyp.Unban, event.getGuild, event.getUser)
  }

  override def onGuildMemberJoin(event: GuildMemberJoinEvent): Unit = {
    val config = db.config(event.getGuild.getIdLong) match {
      case Some(cfg) => cfg
      case None =>
        val config = Configuration.default(event.getGuild.getIdLong)
        db.config += config
        config
    }

    val kicklog = () => event.getGuild.getController.kick(event.getMember).reason(s"🚨 Account less than ${config.minAge} minutes old.").queue(
      _ => ModLog.log(event.getGuild, s"🚨 `${event.getUser.getName}#${event.getUser.getDiscriminator}` `(${event.getUser.getId})` joined the server. Created at ${event.getUser.getCreationTime.toString}, kicking for 🆕")
    )

    if (config.minAge.isDefined) {
      val (sec, min, hour, day, year) = ago(event.getUser.getCreationTime)
      if (min < config.minAge.get && hour == 0 && day == 0 && year == 0) {
        if (event.getGuild.getSelfMember.hasPermission(Permission.KICK_MEMBERS)) {
          event.getUser.openPrivateChannel().queue(
            (t: PrivateChannel) => t.sendMessage("Hey there! Seems like your account is too new to participate in this server.\nPlease try again later.").queue(
              (_: Message) => kicklog(),
              (_: Throwable) => kicklog()
            ),
            (t: Throwable) => kicklog()
          )
        } else {
          ModLog.log(event.getGuild, s"🚨 `${event.getUser.getName}#${event.getUser.getDiscriminator}` `(${event.getUser.getId})` joined the server. Created at ${event.getUser.getCreationTime.toString}, 🚨🚨🚨 **CAN'T KICK FOR 🆕**🚨🚨🚨")
        }
      }
    } else {
      UserLogs.logJoinLeave(AcTyp.Join, event.getGuild, event.getUser, event.getUser.getCreationTime)
    }
  }

  override def onGuildMemberLeave(event: GuildMemberLeaveEvent): Unit = {
    val guild = event.getGuild
    val user = event.getUser
    guild.getBanList.queue(bansJ => {
      val bans = bansJ.toArray().map(_.asInstanceOf[Guild.Ban])
      if (bans.exists(_.getUser.getId == user.getId)) {
        val ban = bans.find(_.getUser.getId == user.getId).get
        guild.getAuditLogs.`type`(ActionType.BAN).queue((jlog: java.util.List[AuditLogEntry]) => {
          val log = jlog.toArray().map(_.asInstanceOf[AuditLogEntry]).filter(_.getTargetId == user.getId)
          if (!log.isEmpty) {
            val entry = log.head
            if (Math.abs(entry.getCreationTime.toEpochSecond - System.currentTimeMillis() / 1000) < ((3 * event.getJDA.getPing) / 1000 + 5)) {
              val banner = entry.getUser
              if (banner != event.getJDA.getSelfUser) {
                if (entry.getReason == null || entry.getReason.isEmpty) {
                  UserLogs.log(AcTyp.Ban, event.getGuild, user, banner)
                } else {
                  UserLogs.log(AcTyp.Ban, event.getGuild, user, banner, entry.getReason)
                }
              }
            } else {
              if (ban.getReason == null || ban.getReason.isEmpty) {
                UserLogs.log(AcTyp.Ban, event.getGuild, user)
              } else {
                UserLogs.log(AcTyp.Ban, event.getGuild, user, ban.getReason)
              }
            }
          } else {
            if (ban.getReason == null || ban.getReason.isEmpty) {
              UserLogs.log(AcTyp.Ban, event.getGuild, user)
            } else {
              UserLogs.log(AcTyp.Ban, event.getGuild, user, ban.getReason)
            }
          }
        }, (e: Throwable) => {
          e.printStackTrace()
          UserLogs.log(AcTyp.Ban, event.getGuild, user, ban.getReason)
        })
      } else {
        guild.getAuditLogs.`type`(ActionType.KICK).queue((jlog: java.util.List[AuditLogEntry]) => {
          val log = jlog.toArray().map(_.asInstanceOf[AuditLogEntry]).filter(_.getTargetId == user.getId)
          if (log.isEmpty) {
            UserLogs.logJoinLeave(AcTyp.Leave, event.getGuild, user, event.getMember.getJoinDate)
          } else {
            val entry = log.head
            if (Math.abs(entry.getCreationTime.toEpochSecond - System.currentTimeMillis() / 1000) < ((3 * event.getJDA.getPing) / 1000 + 5)) {
              if (entry.getUser.getId == event.getJDA.getSelfUser.getId) {
                //return
              } else if (entry.getReason == null || entry.getReason.isEmpty) {
                UserLogs.log(AcTyp.Kick, event.getGuild, user, entry.getUser)
              } else {
                UserLogs.log(AcTyp.Kick, event.getGuild, user, entry.getUser, entry.getReason)
              }
            } else {
              UserLogs.logJoinLeave(AcTyp.Leave, event.getGuild, user, event.getMember.getJoinDate)
            }
          }
        }, (e: Throwable) => {
          e.printStackTrace()
          UserLogs.logJoinLeave(AcTyp.Leave, event.getGuild, user, event.getMember.getJoinDate)
        })
      }
    })
  }

}
