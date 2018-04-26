package robocop.models

case class Configuration(guild: Long, modlogChannel: Option[Long], joinlogChannel: Option[Long], minAge: Option[Long], lockOnEveryone: Boolean, betaEnabled: Boolean) {
  def modlogChannel(value: Option[Long]): Configuration = {
    Configuration(guild, value, joinlogChannel, minAge, lockOnEveryone, betaEnabled)
  }

  def joinlogChannel(value: Option[Long]): Configuration = {
    Configuration(guild, modlogChannel, value, minAge, lockOnEveryone, betaEnabled)
  }

  def minAge(value: Option[Long]): Configuration = {
    Configuration(guild, modlogChannel, joinlogChannel, value, lockOnEveryone, betaEnabled)
  }

  def lockOnEveryone(value: Boolean): Configuration = {
    Configuration(guild, modlogChannel, joinlogChannel, minAge, value, betaEnabled)
  }

  def betaEnabled(value: Boolean): Configuration = {
    Configuration(guild, modlogChannel, joinlogChannel, minAge, lockOnEveryone, value)
  }
}

object Configuration {

  def default(guild: Long): Configuration = Configuration(guild, None, None, None, lockOnEveryone = false, betaEnabled = false)

}

