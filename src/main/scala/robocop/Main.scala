package robocop

import java.util.function.{BiConsumer, IntFunction}

import net.dv8tion.jda.bot.sharding.{DefaultShardManagerBuilder, ShardManager}
import net.dv8tion.jda.core.entities.Game
import net.dv8tion.jda.core.exceptions.HttpException
import net.dv8tion.jda.core.{AccountType, JDA, JDABuilder, OnlineStatus}
import robocop.listeners.{Listener, LogListener}
import robocop.utils.Implicits._

import scala.xml.XML

object Main {

  var sharding: Int = 0
  var shardManager: ShardManager = _
  var token = ""
  var webhookUrl = ""

  def main(args: Array[String]): Unit = {

    val xml = XML.loadFile("config.xml") \\ "config"

    val config = if (args.length >= 1) {
      xml \\ args.head
    } else {
      xml \\ "robocop"
    }

    if (config.isEmpty) {
      println(error"Key not found in config.")
      System.exit(1)
    }

    token = config \\ "token" text

    webhookUrl = config \\ "webhook" text

    sharding = (config \\ "shards" text).toInt

    println(debug"Starting up with ${sharding} shards.")

    shardManager = new DefaultShardManagerBuilder()
        .setShardsTotal(sharding)
        .setShards(0, sharding - 1)
        .addEventListenerProvider((value: Int) => new Listener(value, webhookUrl))
        .setToken(token)
        .setGame(Game.watching("the Terminator Trilogy"))
        .setAudioEnabled(false)
        .setEnableShutdownHook(true)
        .setStatus(OnlineStatus.ONLINE)
        .build()
  }
}
