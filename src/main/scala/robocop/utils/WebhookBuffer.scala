package robocop.utils

import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}

import net.dv8tion.jda.webhook.{WebhookClientBuilder, WebhookMessageBuilder}

case class WebhookBuffer(url: String, var name: Option[String] = None, var profile: Option[String] = None) {

  var active = true

  private val webhook = new WebhookClientBuilder(url).build()
  private var buffer: Array[String] = Array()

  private val executor = new ScheduledThreadPoolExecutor(1)
  private val loop = new Runnable {
    override def run(): Unit = sendBuffer()

  }
  private val thread = executor.scheduleAtFixedRate(loop, 2500, 5000, TimeUnit.MILLISECONDS)

  private def send(message: String): Unit = {
    val builder = new WebhookMessageBuilder()
    name.foreach(builder.setUsername)
    profile.foreach(builder.setAvatarUrl)
    builder.append(message)
    webhook.send(builder.build())
  }

  private def sendBuffer(): Unit = {
    if (buffer.nonEmpty) {
      send(buffer.mkString("\n"))
      buffer = Array()
    }
  }

  def +=(item: String): Unit = buffer :+= item

  def !(item: String): Unit = send(item)

  def shutdown(): Unit = {
    if(active) {
      thread.cancel(true)
      active = false
    }
    sendBuffer()
  }


}
