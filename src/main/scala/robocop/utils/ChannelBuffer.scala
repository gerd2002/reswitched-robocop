package robocop.utils

import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}

import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.webhook.{WebhookClientBuilder, WebhookMessageBuilder}

case class ChannelBuffer(channel: TextChannel) {

  private var buffer: Array[String] = Array()

  private val executor = new ScheduledThreadPoolExecutor(1)
  private val loop = new Runnable {
    override def run(): Unit = sendBuffer()

  }
  private val thread = executor.scheduleAtFixedRate(loop, 2500, 5000, TimeUnit.MILLISECONDS)

  private def send(message: String): Unit = {
    channel.sendMessage(message).queue()
  }

  private def sendBuffer(): Unit = {
    if (buffer.nonEmpty) {
      send(buffer.mkString("\n"))
      buffer = Array()
    }
  }

  def +=(item: String): Unit = {
    if (item.length + buffer.length > 1980)
      sendBuffer()
    buffer :+= item
  }

  def !(item: String): Unit = send(item)

}
