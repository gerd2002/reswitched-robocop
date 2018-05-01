package robocop

import net.dv8tion.jda.bot.sharding.ShardManager
import net.dv8tion.jda.core.entities.Game
import net.dv8tion.jda.core.{AccountType, JDA, JDABuilder, OnlineStatus}
import robocop.listeners.{Listener, LogListener}
import robocop.utils.Implicits._

import scala.xml.XML

object Main {

  var sharding: Seq[Int] = Seq()
  var shards: Map[Int, JDA] = Map()
  var shardManager: ShardManager = null
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

    sharding = 0 until (config \\ "shards" text).toInt

    println(info"Starting up with ${sharding.length} shards.")

    sharding.foreach(shardId => {
      var tries = 1
      while (tries < 5) {
        try {
          if (shardId == 0) {
            println(info"Starting up master shard. (Try $tries)")
            val jda = start(shardId)
            shards += (shardId -> jda)
            shardManager = jda.asBot().getShardManager
          } else {
            println(info"Starting slave shard $shardId. (Try $tries)")
            shardManager.start(shardId)
          }
        } catch {
          case e: Throwable =>
            tries += 1
            println(error"Error while starting shard $shardId")
            Thread.sleep(10)
            e.printStackTrace()
            if (tries == 4) {
              Thread.sleep(10)
              println(warn"Could not start shard $shardId (possibly master shard). Contact Gerd#8888 with a full log.")
              System.exit( 2)
            } else {
              Thread.sleep(10)
              println(debug"Sleeping ${Math.pow(2, tries).toLong}s before next attempt.")
              Thread.sleep(Math.pow(2, tries).toLong * 1000)
            }
        }
      }
    })
  }

  def start(shardId: Int): JDA = {
    new JDABuilder(AccountType.BOT)
      .setToken(token)
      .setGame(Game.watching("the Terminator Trilogy"))
      .setAudioEnabled(false)
      .setEnableShutdownHook(true)
      .setStatus(OnlineStatus.ONLINE)
      .addEventListener(new Listener(shardId, webhookUrl))
      .addEventListener(new LogListener(shardId))
      .useSharding(shardId, sharding.length)
      .buildAsync()
  }
}
