package robocop.commands

import net.dv8tion.jda.core.entities.{Member, Message, User}
import robocop.Main
import robocop.database.Robobase
import robocop.models.Command
import robocop.utils.Checks

object Management {

  object ShutdownSingle extends Command {
    override def name: String = "shut1"

    override def help: String = "Shuts down ONLY the shard that the current guild is running on."

    override def checkGuild(member: Member): Boolean = Checks.isOwner(member)

    override def checkDM(user: User): Boolean = Checks.isOwner(user)

    override def hidden: Boolean = true

    override def execute(args: Array[String], message: Message, db: Robobase): Option[String] = {
      message.getJDA.shutdown()
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
      Main.shards.values.foreach(_.shutdown())
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
      Main.shards.foreach {
        case (shardId, shardThread) =>
          shardThread.shutdown()
          Main.shards -= shardId
      }
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
      message.getJDA.shutdown()
      Main.shards -= shardId
      Main.shards += (shardId -> Main.start(shardId))
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
      Main.shards.foreach {
        case (shardId, shardThread) =>
          shardThread.shutdown()
          Main.shards -= shardId
          Main.shards += (shardId -> Main.start(shardId))
      }
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
        case 0 => message.respondDM(s"List of Shards: `${Main.sharding.mkString(", ")}`\nCurrent Shard: **${message.getJDA.getShardInfo.getShardId}**")
        case 1 => if (args(0).matches("\\d+")) {
          val shardId = args(0).toInt
          if (Main.sharding.contains(shardId)) {
            Main.shards(shardId).shutdown()
            Main.shards -= shardId
            Main.shards += (shardId -> Main.start(shardId))
          }
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

    override def execute(args: Array[String], message: Message, db: Robobase): Option[String] = {
      message.respond(db.config(message.getGuild.getIdLong).toString)
      None
    }
  }


}
