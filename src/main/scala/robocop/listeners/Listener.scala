package robocop.listeners

import java.time.OffsetDateTime
import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}

import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities._
import net.dv8tion.jda.core.events.guild.{GuildAvailableEvent, GuildJoinEvent, GuildLeaveEvent, GuildUnavailableEvent}
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent
import net.dv8tion.jda.core.events.{DisconnectEvent, ReadyEvent, ReconnectedEvent, ShutdownEvent}
import net.dv8tion.jda.core.hooks.ListenerAdapter
import robocop.commands._
import robocop.database.Robobase
import robocop.models._
import robocop.utils.{ModLog, WebhookBuffer}
import robocop.utils.Implicits._

import scala.collection.immutable.Queue

object Listener {
  val prefix = "cop~"
  val commandList: List[Command] = List(
    Info.About,
    Info.Help,
    Info.Ping,
    Info.Created,

    Lockdown.Lock,
    Lockdown.SilentLock,
    Lockdown.Unlock,

    Management.ShutdownSingle,
    Management.ShutdownAll,
    Management.FullShutdown,
    Management.RestartSingle,
    Management.RestartAll,
    Management.RestartShard,
    Management.BetaTest,
    Management.PrintConfig,
    Management.Update,
    Management.FullRestart,

    Eval.EvalCommand,
    Eval.NashornEval,
    Eval.SqlEval,

    Moderation.Kick,
    Moderation.Ban,
    Moderation.Forceban,
    Moderation.Unban,
    Moderation.Check
  )

  def ago(time: OffsetDateTime): (Long, Long, Long, Long, Long) = {
    val difference = System.currentTimeMillis() / 1000 - time.toEpochSecond
    val seconds = difference % 60
    val minutes = (difference / 60) % 60
    val hours = (difference / 3600) % 24
    val days = (difference / 86400) % 365
    val years = difference / 31536000
    (seconds, minutes, hours, days, years)
  }

  def agoString(user: User): String = agoString(user.getCreationTime)

  def agoString(time: OffsetDateTime): String = {
    val (seconds, minutes, hours, days, years) = ago(time)
    var agoString = ""
    if (years > 0) {
      agoString += s"$years years, "
    }
    if (days > 0) {
      agoString += s"$days days, "
      if (days > 7) {
        return agoString.substring(0, agoString.length - 2)
      } else {
        agoString += f"$hours%2d:$minutes%2d:$seconds%2d"
      }
    } else {
      agoString += f"$hours%2d:$minutes%2d:$seconds%2d"
    }
    agoString
  }

  var messageCache: Map[Long, Queue[Message]] = Map()

  var owner: User = null
}

class Listener(shardId: Int, webhookUrl: String) extends ListenerAdapter {

  import Listener._

  val db = new Robobase()
  val shard = shardId + 1

  val loghook = WebhookBuffer(url = webhookUrl, name = Some("Robocop Fallback"))

  override def onGuildMessageReceived(event: GuildMessageReceivedEvent): Unit = {
    val message = event.getMessage
    val config = db.config(event.getGuild.getIdLong) match {
      case Some(cfg) => cfg
      case None =>
        val config = Configuration.default(event.getGuild.getIdLong)
        db.config += config
        config
    }

    if (config.lockOnEveryone && message.mentionsEveryone() && message.getMember.hasPermission(Permission.MESSAGE_MENTION_EVERYONE)) {
      new Thread(new Runnable {
        override def run(): Unit = {
          Lockdown.lock(message.getTextChannel, "🔒 **Auto-Lockdown**: auto-locked due to mention of everyone")
          Thread.sleep(1000 * 60 * 2)
          Lockdown.unlock(message.getTextChannel, "🔓 Auto-Lockdown cleared, please don't start spamming.")
        }
      }).run()
    }

    val channel = message.getChannel
    try {
      val content = message.getContentRaw
      if (content.isEmpty || !content.startsWith(prefix))
        return
      val parts = content.substring(prefix.length).split(" ")
      val command = parts.head
      val args = parts.tail

      val commands = commandList.filter(x => x.name == command || x.aliases.contains(command))
      if (commands.isEmpty)
        return
      val filtered = commands.filter(x => x.checkGuild(message.getMember))
      if (filtered.length > 1)
        channel.sendMessage(BotError.NotUnique.toString).queue()
      else if (filtered.length == 1) {
        val commandObj = filtered.head
        if (!config.betaEnabled && commandObj.beta) {
          loghook += s"`[WARN]` ${message.getGuild.getName} tried to run ${commandObj.name}, but is not enabled."
        } else {
          val before = System.currentTimeMillis()
          val log = commandObj.execute(args, message, db)
          val diff = System.currentTimeMillis() - before
          db.metrics += Metrics(commandObj.name, diff.toInt)
          if (log.isDefined) {
            ModLog.log(message.getGuild, log.get)
          }
        }
      }
    } catch {
      case err: BotError => channel.sendMessage(err.toString).queue()
    }

  }

  override def onPrivateMessageReceived(event: PrivateMessageReceivedEvent): Unit = {
    val message = event.getMessage

    val channel = message.getChannel
    try {
      val content = message.getContentRaw
      if (content.isEmpty || !content.startsWith(prefix))
        return
      val parts = content.substring(prefix.length).split(" ")
      val command = parts.head
      val args = parts.tail

      val commands = commandList.filter(x => x.name == command || x.aliases.contains(command))
      if (commands.isEmpty)
        return
      val filtered = commands.filter(x => x.checkDM(message.getAuthor))
      if (filtered.length > 1)
        channel.sendMessage(BotError.NotUnique.toString).queue()
      else if (filtered.length == 1) {
        val commandObj = filtered.head
        commandObj.execute(args, message, db)
      }
    } catch {
      case err: Throwable => err.printStackTrace(); channel.sendMessage(err.toString.take(2000)).queue()
    }
  }

  override def onReady(event: ReadyEvent): Unit = {
    val self = event.getJDA.getSelfUser
    loghook.profile = Some(self.getAvatarUrl)
    loghook.name = Some(self.getName)
    println(info"Ready! Connected Shard ${event.getJDA.getShardInfo.getShardId}")
    loghook += s"ℹ Shard `$shard/${event.getJDA.getShardInfo.getShardTotal}`: <:online:438228497505452032> CONNECTED"

    val executor = new ScheduledThreadPoolExecutor(1)
    val loop = new Runnable {

      override def run(): Unit = {
        val pings = Pings(event.getJDA.getPing, shard, event.getJDA.getShardInfo.getShardTotal, System.currentTimeMillis())
        if(pings.duration > 0)
          db.pings += pings
      }
    }
    val thread = executor.scheduleAtFixedRate(loop, 0, 30, TimeUnit.SECONDS)
    event.getJDA.asBot().getApplicationInfo.queue(x => owner = x.getOwner)
  }

  override def onDisconnect(event: DisconnectEvent): Unit = {
    println(warn"Disconnected with ${event.getCloseCode} at ${event.getDisconnectTime}")
    loghook += s"ℹ Shard `$shard/${event.getJDA.getShardInfo.getShardTotal}`: <:dnd:438228496431579148> DISCONNECTED"
  }

  override def onReconnect(event: ReconnectedEvent): Unit = {
    println(warn"Reconnected Shard ${event.getJDA.getShardInfo.getShardId}")
    loghook += s"ℹ Shard `$shard/${event.getJDA.getShardInfo.getShardTotal}`: <:away:438228497408720896> RECONNECTED"
  }

  override def onShutdown(event: ShutdownEvent): Unit = {
    println(warn"Shutting down $shard")
    loghook ! s"🛰 Shutting down `$shard/${event.getJDA.getShardInfo.getShardTotal}`."
    loghook.shutdown()
  }

  override def onGuildJoin(event: GuildJoinEvent): Unit = {
    loghook += s"📥 Joined guild ${event.getGuild.getName} `(${event.getGuild.getId})`"
  }

  override def onGuildLeave(event: GuildLeaveEvent): Unit = {
    loghook += s"📥 Joined guild ${event.getGuild.getName} `(${event.getGuild.getId})`"
  }

  override def onGuildAvailable(event: GuildAvailableEvent): Unit = {
    loghook += s"<:streaming:438228497408851969> Shard $shard Got guild ${event.getGuild.getName} `(${event.getGuild.getId})`"
  }

  override def onGuildUnavailable(event: GuildUnavailableEvent): Unit = {
    loghook += s"<:streaming:438228497408851969> Shard $shard Lost guild ${event.getGuild.getName} `(${event.getGuild.getId})`"
  }

}
