package robocop.utils

import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.{Member, Role, User}
import robocop.listeners.Listener

object Checks {

  def hasPerm(permission: Permission)(member: Member): Boolean = member.hasPermission(permission)

  def standardTrust(member: Member): Boolean = hasPerm(Permission.MESSAGE_ATTACH_FILES)(member)
  def standardMod(member: Member): Boolean = hasPerm(Permission.KICK_MEMBERS)(member)
  def standardAdmin(member: Member): Boolean = hasPerm(Permission.BAN_MEMBERS)(member)

  def hasRole(name: String)(member: Member): Boolean = member.getRoles.toArray.map(_.asInstanceOf[Role].getName.toLowerCase).contains(name.toLowerCase)
  
  def isOwner(member: Member): Boolean = isOwner(member.getUser)
  def isOwner(user: User): Boolean = user.getId == Listener.owner.getId

}
