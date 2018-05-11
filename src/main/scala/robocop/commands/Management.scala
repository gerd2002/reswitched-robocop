package robocop.commands

import java.io.{BufferedReader, InputStreamReader}

import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.{Member, Message, User}
import robocop.Main
import robocop.database.Robobase
import robocop.models.Command
import robocop.utils.{ChannelBufferEditing, Checks}

object Management {

  object ShutdownSingle extends Command {
    override def name: String = "shut1"

    override def help: String = "Shuts down ONLY the shard that the current guild is running on."

    override def checkGuild(member: Member): Boolean = Checks.isOwner(member)

    override def checkDM(user: User): Boolean = Checks.isOwner(user)

    override def hidden: Boolean = true

    override def execute(args: Array[String], message: Message, db: Robobase): Option[String] = {
      val shardId = message.getJDA.getShardInfo.getShardId
      Main.shardManager.shutdown(shardId)
      None
    }
  }

  object ShutdownAll extends Command {
    override def name: String = "shuta"

    override def help: String = "Shuts down all shards."

    override def checkGuild(member: Member): Boolean = Checks.isOwner(member)

    override def checkDM(user: User): Boolean = Checks.isOwner(user)

    override def hidden: Boolean = true

    override def execute(args: Array[String], message: Message, db: Robobase): Option[String] = {
      Main.shardManager.shutdown()
      None
    }
  }

  object FullShutdown extends Command {
    override def name: String = "shutdown"

    override def help: String = "Completely stop all shards."

    override def checkGuild(member: Member): Boolean = Checks.isOwner(member)

    override def checkDM(user: User): Boolean = Checks.isOwner(user)

    override def hidden: Boolean = true

    override def execute(args: Array[String], message: Message, db: Robobase): Option[String] = {
      Main.shardManager.shutdown()
      System.exit(0)
      None
    }
  }

  object RestartSingle extends Command {
    override def name: String = "restart1"

    override def help: String = "Restarts the current shard (in case it's malfunctioning)."

    override def checkGuild(member: Member): Boolean = Checks.isOwner(member)

    override def checkDM(user: User): Boolean = Checks.isOwner(user)

    override def hidden: Boolean = true

    override def execute(args: Array[String], message: Message, db: Robobase): Option[String] = {
      val shardId = message.getJDA.getShardInfo.getShardId
      Main.shardManager.restart(shardId)
      None
    }
  }

  object RestartAll extends Command {
    override def name: String = "restarta"

    override def help: String = "Restarts all shards without reloading the code."

    override def checkGuild(member: Member): Boolean = Checks.isOwner(member)

    override def checkDM(user: User): Boolean = Checks.isOwner(user)

    override def hidden: Boolean = true

    override def execute(args: Array[String], message: Message, db: Robobase): Option[String] = {
      Main.shardManager.restart()
      None
    }
  }

  object RestartShard extends Command {
    override def name: String = "restartn"

    override def help: String = "Restarts a specified shard (by internal ID)."

    override def checkGuild(member: Member): Boolean = Checks.isOwner(member)

    override def checkDM(user: User): Boolean = Checks.isOwner(user)

    override def hidden: Boolean = true

    override def execute(args: Array[String], message: Message, db: Robobase): Option[String] = {
      args.length match {
        case 0 => message.respondDM(s"List of Shards: `${Main.shardManager.getShards.toArray(Array[JDA]()).map(_.getShardInfo.getShardId).mkString(", ")}`\nCurrent Shard: **${message.getJDA.getShardInfo.getShardId}**")
        case 1 => if (args(0).matches("\\d+")) {
          val shardId = args(0).toInt
          Main.shardManager.restart(shardId)
        }
        case _ => message.respondDM("Requires either no or one argument.")
      }
      None
    }
  }

  object BetaTest extends Command {
    override def name: String = "isbeta"

    override def beta: Boolean = true

    override def help: String = "Sends a message if the guild is beta-enabled"

    override def execute(args: Array[String], message: Message, db: Robobase): Option[String] = {
      message.respond("Is beta enabled.")
      None
    }
  }

  object PrintConfig extends Command {
    override def name: String = "pconf"

    override def hidden: Boolean = true

    override def help: String = "Sends the config."

    override def checkGuild(member: Member): Boolean = Checks.isOwner(member)

    override def checkDM(user: User): Boolean = Checks.isOwner(user)

    override def execute(args: Array[String], message: Message, db: Robobase): Option[String] = {
      message.respond(db.config(message.getGuild.getIdLong).toString)
      None
    }
  }

  object Update extends Command {
    override def name: String = "update"

    override def hidden: Boolean = true

    override def help: String = "Pulls the latest update from GitHub and rebuilds the bot."

    override def checkGuild(member: Member): Boolean = Checks.isOwner(member)

    override def checkDM(user: User): Boolean = Checks.isOwner(user)

    override def execute(args: Array[String], message: Message, db: Robobase): Option[String] = {
      val thread = new Thread(() => {
        var command = "git pull"
        if (args.contains("--https"))
          command += " https master"
        val exit = execCommand(command, message)
        if (exit == 0) {
          execCommand("sbt assembly -batch", message)
        } else {
          message.respond(s"Pull exited with code $exit")
        }
      })
      thread.start()

      None
    }

    private def execCommand(command: String, message: Message): Integer = {
      val p = Runtime.getRuntime.exec(command)
      val errorReader = new BufferedReader(new InputStreamReader(p.getErrorStream))
      val stdReader = new BufferedReader(new InputStreamReader(p.getInputStream))

      val buffer = ChannelBufferEditing(message.getChannel)

      while (p.isAlive) {
        val lineErr = errorReader.readLine()
        val lineStd = stdReader.readLine()
        if (lineStd != null) {
          buffer += lineStd
          println(lineStd)
        }
        if (lineErr != null) {
          buffer += lineErr
          println(lineErr)
        }
      }

      var line = ""

      def check(bufferReader: BufferedReader): Boolean = {
        line = bufferReader.readLine()
        line != null
      }

      while (check(stdReader)) {
        buffer += line
        println(line)
      }

      while (check(errorReader)) {
        buffer += line
        println(line)
      }

      buffer.shutdown()
      p.exitValue()
    }
  }

  object FullRestart extends Command {
    override def name: String = "restart"

    override def hidden: Boolean = true

    override def help: String = "Fully restarts the bot."

    override def checkGuild(member: Member): Boolean = Checks.isOwner(member)

    override def checkDM(user: User): Boolean = Checks.isOwner(user)

    override def execute(args: Array[String], message: Message, db: Robobase): Option[String] = {
      Main.shardManager.shutdown()
      System.exit(124)
      None
    }
  }


}
