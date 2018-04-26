package robocop

import net.dv8tion.jda.core.entities.Game
import net.dv8tion.jda.core.{AccountType, JDA, JDABuilder, OnlineStatus}
import robocop.listeners.{Listener, LogListener}

import scala.xml.XML

object Main {

  val sharding: Seq[Int] = 0 to 3
  var shards: Map[Int, JDA] = Map()

  val xml = XML.loadFile("config.xml")

  val token = xml \\ "robocop" \\ "token" text
  val webhook = xml \\ "robocop" \\ "webhook" text

  def main(args: Array[String]): Unit = {
    sharding.foreach(shardId => {
      val jda = start(shardId)
      shards += (shardId -> jda)
    })
  }

  def start(shardId: Int): JDA = {
    new JDABuilder(AccountType.BOT)
      .setToken(token)
      .setGame(Game.watching("the Terminator Trilogy"))
      .setAudioEnabled(false)
      .setEnableShutdownHook(true)
      .setStatus(OnlineStatus.ONLINE)
      .addEventListener(new Listener(shardId, webhook))
      .addEventListener(new LogListener(shardId))
      .useSharding(shardId, sharding.length)
      .buildAsync()
  }
}
