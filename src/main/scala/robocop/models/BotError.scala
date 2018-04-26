package robocop.models

import net.dv8tion.jda.core.Permission

abstract sealed case class BotError(name: String, message: String, symbol: String = "") extends Throwable {
  override def toString: String =
    s"""$symbol An error of type `$name` occurred:
       |```
       |$message
       |```
       |""".stripMargin
}

object BotError {
  sealed class Unauthorized(message: String) extends BotError("Unauthorized", message, symbol = "🚫")
  sealed class NoPermissions(permission: Permission) extends BotError("NoPermissions", s"Missing Permission ${permission.getName}", symbol = "💢")
  object NotUnique extends BotError("NotUnique", s"Command did not resolve uniquely", symbol = "⚠️")
  object NotFound extends BotError("NotFound", s"Could not find this", symbol = "⚠️")
  object InsufficentRoles extends BotError("InsufficentRoles", s"Your top role is too low to do this.", symbol = "🔞")
  object InsufficentBotRoles extends BotError("InsufficentBotRoles", s"The bots top role is too low to do this.", symbol = "🔞")
  sealed class ExecutionError(message: String) extends BotError("Unknown", message, symbol = "‼️")
}
