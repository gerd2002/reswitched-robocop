package robocop.utils

import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}

import net.dv8tion.jda.core.entities.{Message, MessageChannel}

case class ChannelBuffer(channel: MessageChannel) {

  private val executor = new ScheduledThreadPoolExecutor(1)
  private val loop = new Runnable {
    override def run(): Unit = sendBuffer()

  }
  private val thread = executor.scheduleAtFixedRate(loop, 2500, 5000, TimeUnit.MILLISECONDS)
  var active = true
  private var message: Message = _
  private var buffer: Array[String] = Array()

  def +=(item: String): Unit = buffer :+= item

  def !(item: String): Unit = send(item)

  def shutdown(): Unit = {
    if (active) {
      thread.cancel(true)
      active = false
    }
    sendBuffer()
  }

  private def sendBuffer(): Unit = {
    if (buffer.nonEmpty) {
      send(buffer.mkString("\n"))
      buffer = Array()
    }
  }

  private def send(msg: String): Unit = {
    if (message != null) {
      if (message.getContentRaw.length + msg.length >= 1900) {
        channel.sendMessage(s"```\n$msg```").queue(x => message = x)
      } else {
        message.editMessage("```\n" + message.getContentDisplay + msg + "```").queue()
      }
    } else {
      channel.sendMessage(s"```\n$msg```").queue(x => message = x)
    }
  }


}
