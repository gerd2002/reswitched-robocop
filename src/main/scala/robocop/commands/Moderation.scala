package robocop.commands

import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities._
import robocop.database.Robobase
import robocop.models.{BotError, Command}
import robocop.utils.Checks
import robocop.utils.ModLog.UserLogs.Loglines
import robocop.utils.ModLog.ActionType

object Moderation {

  def checkPossible(mod: Member, target: Member): Unit = {
    val modRoles = mod.getRoles.toArray(Array[Role]())
    val targetRoles = target.getRoles.toArray(Array[Role]())
    val selfRoles = mod.getGuild.getSelfMember.getRoles.toArray(Array[Role]())
    if (targetRoles.nonEmpty) {
      if (modRoles.head.getPosition <= targetRoles.head.getPosition)
        throw BotError.InsufficentRoles
      if (selfRoles.head.getPosition <= targetRoles.head.getPosition)
        throw BotError.InsufficentBotRoles
    }
  }

  def softCheck(mod: Member, target: Member): Boolean = {
    val modRoles = mod.getRoles.toArray(Array[Role]())
    val targetRoles = target.getRoles.toArray(Array[Role]())
    val selfRoles = mod.getGuild.getSelfMember.getRoles.toArray(Array[Role]())
    if (targetRoles.nonEmpty) {
      if (modRoles.head.getPosition <= targetRoles.head.getPosition)
        false
      else if (selfRoles.head.getPosition <= targetRoles.head.getPosition)
        false
      else
        true
    } else {
      true
    }
  }

  object Check extends Command {
    override def name: String = "check"

    override def help: String = "Checks if an action can be applied to a member"

    override def checkGuild(member: Member): Boolean = Checks.standardMod(member)

    override def execute(args: Array[String], message: Message, db: Robobase): Option[String] = {
      if (message.isFromType(ChannelType.TEXT)) {
        val author = message.getAuthor
        val guild = message.getGuild
        val mentions = message.getMentionedUsers.toArray(Array[User]())
        if(mentions.nonEmpty) {
          if (message.getGuild.getSelfMember.hasPermission(Permission.KICK_MEMBERS)) {
            val target = mentions.head
            val reason = args.filter(!_.contains(target.getAsMention)).mkString(" ")
            val member = guild.getMember(target)
            if(member != null) {
              checkPossible(message.getMember, member)
              message.getGuild.getController.kick(member, s"${author.getName}#${author.getDiscriminator}: $reason").queue()
              Some(Loglines.logline(ActionType.Kick, target, author, reason))
            } else {
              throw BotError.NotFound
            }
          } else {
            throw new BotError.NoPermissions(Permission.KICK_MEMBERS)
          }
        } else {
          message.respondMention("Please mention the user you want to check.")
          None
        }
      } else {
        message.respondDM("This command can only be used in Servers.")
        None
      }
    }
  }

  object Kick extends Command {
    override def name: String = "kick"

    override def help: String = "Kicks the mentioned member, and adds the reason to both modlogs and audit logs."

    override def checkGuild(member: Member): Boolean = Checks.standardMod(member)

    override def execute(args: Array[String], message: Message, db: Robobase): Option[String] = {
      if (message.isFromType(ChannelType.TEXT)) {
        val author = message.getAuthor
        val guild = message.getGuild
        val mentions = message.getMentionedUsers.toArray(Array[User]())
        if(mentions.nonEmpty) {
          if (message.getGuild.getSelfMember.hasPermission(Permission.KICK_MEMBERS)) {
            val target = mentions.head
            val reason = args.filter(!_.contains(target.getAsMention)).mkString(" ")
            val member = guild.getMember(target)
            if(member != null) {
              checkPossible(guild.getMember(message.getAuthor), member)
              message.getGuild.getController.kick(member, s"${author.getName}#${author.getDiscriminator}: $reason").queue()
              Some(Loglines.logline(ActionType.Kick, target, author, reason))
            } else {
             throw BotError.NotFound
            }
          } else {
            throw new BotError.NoPermissions(Permission.KICK_MEMBERS)
          }
        } else {
          message.respondMention("Please mention the user you want to kick.")
          None
        }
      } else {
        message.respondDM("This command can only be used in Servers.")
        None
      }
    }
  }

  object Ban extends Command {
    override def name: String = "ban"

    override def help: String = "Bans the mentioned member, and adds the reason to both modlogs and audit logs."

    override def checkGuild(member: Member): Boolean = Checks.standardMod(member)

    override def execute(args: Array[String], message: Message, db: Robobase): Option[String] = {
      if (message.isFromType(ChannelType.TEXT)) {
        val author = message.getAuthor
        val guild = message.getGuild
        val mentions = message.getMentionedUsers.toArray(Array[User]())
        if(mentions.nonEmpty) {
          if (message.getGuild.getSelfMember.hasPermission(Permission.BAN_MEMBERS)) {
            val target = mentions.head
            val reason = args.filter(!_.contains(target.getAsMention)).mkString(" ")
            val member = guild.getMember(target)
            if(member != null) {
              checkPossible(message.getMember, member)
            }
            message.getGuild.getController.ban(target, 0, s"${author.getName}#${author.getDiscriminator}: $reason").queue()
            Some(Loglines.logline(ActionType.Ban, target, author, reason))
          } else {
            throw new BotError.NoPermissions(Permission.BAN_MEMBERS)
          }
        } else {
          message.respondMention("Please mention the user you want to ban.")
          None
        }
      } else {
        message.respondDM("This command can only be used in Servers.")
        None
      }
    }
  }

  object Forceban extends Command {
    override def name: String = "ban!"

    override def help: String = "Bans an user by ID, and adds the reason to both modlogs and audit logs."

    override def checkGuild(member: Member): Boolean = Checks.standardAdmin(member)

    override def execute(args: Array[String], message: Message, db: Robobase): Option[String] = {
      if (message.isFromType(ChannelType.TEXT)) {
        val author = message.getAuthor
        val guild = message.getGuild
        val ids = args.filter(_.matches("\\d+"))
        if(ids.nonEmpty) {
          if (message.getGuild.getSelfMember.hasPermission(Permission.BAN_MEMBERS)) {
            val target = ids.head
            val reason = args.filter(!_.contains(target)).mkString(" ")
            val member = guild.getMemberById(target)
            if(member != null) {
              checkPossible(message.getMember, member)
            }
            message.getGuild.getController.ban(target, 0, s"${author.getName}#${author.getDiscriminator}: $reason").queue()
            Some(Loglines.logline(ActionType.Forceban, target, author, reason))
          } else {
            throw new BotError.NoPermissions(Permission.BAN_MEMBERS)
          }
        } else {
          message.respondMention("Please mention the user ID you want to ban.")
          None
        }
      } else {
        message.respondDM("This command can only be used in Servers.")
        None
      }
    }
  }

  object Unban extends Command {
    override def name: String = "unban"

    override def help: String = "Unbans a user by ID and adds the reason to audit and modlogs."

    override def checkGuild(member: Member): Boolean = Checks.standardAdmin(member)

    override def execute(args: Array[String], message: Message, db: Robobase): Option[String] = {
      if (message.isFromType(ChannelType.TEXT)) {
        val author = message.getAuthor
        val guild = message.getGuild
        val ids = args.filter(_.matches("\\d+"))
        if(ids.nonEmpty) {
          if (message.getGuild.getSelfMember.hasPermission(Permission.BAN_MEMBERS)) {
            val target = ids.head
            val reason = args.filter(!_.contains(target)).mkString(" ")
            message.getGuild.getController.unban(target).reason(s"${author.getName}#${author.getDiscriminator}: $reason").queue()
            Some(Loglines.logline(ActionType.Unban, target, author, reason))
          } else {
            throw new BotError.NoPermissions(Permission.BAN_MEMBERS)
          }
        } else {
          message.respondMention("Please mention the user ID you want to unban.")
          None
        }
      } else {
        message.respondDM("This command can only be used in Servers.")
        None
      }
    }
  }

  val lamechars: Array[Char] = "!\"#$%&'()*+,-./0123456789[]_`".chars().toArray.map(_.toChar)
  def lamelist(guild: Guild, moderator: Member): Array[Member] = guild.getMembers.toArray(Array[Member]()).filter(x => lamechars.contains(x.getEffectiveName.charAt(0))).filter(softCheck(moderator, _))

  object Lameface extends Command {
    override def name: String = "lameface"

    override def help: String = "Changes all users nicknames whos nicknames or usernames are considered hoisting."

    override def checkGuild(member: Member): Boolean = Checks.standardAdmin(member)

    override def execute(args: Array[String], message: Message, db: Robobase): Option[String] = {
      if (message.isFromType(ChannelType.TEXT)) {
        val author = message.getAuthor
        val guild = message.getGuild
        if (message.getGuild.getSelfMember.hasPermission(Permission.NICKNAME_MANAGE)) {
          val targets = lamelist(guild, message.getMember)
          targets.foreach(x => guild.getController.setNickname(x, s"\uD82F\uDCA2${x.getEffectiveName.take(30)}").reason("Dehoisting").queue())
          Some(Loglines.logline(new ActionType.Lameface(targets.length), author))
        } else {
          throw new BotError.NoPermissions(Permission.NICKNAME_MANAGE)
        }
      } else {
        message.respondDM("This command can only be used in Servers.")
        None
      }
    }
  }

  object Lamecount extends Command {
    override def name: String = "lamecount"

    override def help: String = "Count of all users whos nicknames or usernames are considered hoisting."

    override def checkGuild(member: Member): Boolean = Checks.standardAdmin(member)

    override def execute(args: Array[String], message: Message, db: Robobase): Option[String] = {
      if (message.isFromType(ChannelType.TEXT)) {
        val author = message.getAuthor
        val guild = message.getGuild
        if (message.getGuild.getSelfMember.hasPermission(Permission.NICKNAME_MANAGE)) {
          val targets = lamelist(guild, message.getMember)
          message.respond(s"Would lameface ${targets.length} users. Execute lameface to continue.")
          None
        } else {
          throw new BotError.NoPermissions(Permission.NICKNAME_MANAGE)
        }
      } else {
        message.respondDM("This command can only be used in Servers.")
        None
      }
    }
  }

}
