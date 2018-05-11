package robocop.utils

import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}

import net.dv8tion.jda.core.entities.{Message, MessageChannel}

case class ChannelBufferEditing(channel: MessageChannel) {

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
        if (msg.length >= 1900) {
          var (head, tail) = msg.splitAt(1900)
          var parts = Seq(head.toString)
          while (tail.length >= 1900) {
            val tuple = msg.splitAt(1900)
            head = tuple._1
            tail = tuple._2
            parts :+= head
          }
          parts :+= tail
          parts.filter(_!=parts.last).foreach(x => s"```\n$x\n```")
          channel.sendMessage(s"```\n${parts.last}\n```").queue(x => message = x)
        } else {
          channel.sendMessage(s"```\n$msg\n```").queue(x => message = x)
        }
      } else {
        message.editMessage(s"```\n${message.getContentDisplay.replace("```", "")}$msg\n```").queue()
      }
    } else {
      channel.sendMessage(s"```\nnull\n```").queue(x => message = x)
    }
  }


}
