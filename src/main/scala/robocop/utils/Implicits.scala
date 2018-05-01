package robocop.utils

object Implicits {

  implicit class TertiaryOne(boolean: Boolean) {
    implicit class TertiaryTwo[T](left: T) {
      def ~(right: T): T = {
        if (boolean)
          left
        else
          right
      }
    }
    def ?[T](left: T): TertiaryTwo[T] = {
      new TertiaryTwo[T](left)
    }
  }

  implicit class Colored(private val sc: StringContext) {
    def error(args: Any*): String = {
      "\u001B[31m[ERROR]\u001B[0m " + FormatLogging.interpolate(sc.parts, args)
    }
    def warn(args: Any*): String = {
      "\u001B[33m[WARN]\u001B[0m  " + FormatLogging.interpolate(sc.parts, args)
    }
    def info(args: Any*): String = {
      "\u001B[37m[INFO]\u001B[0m  " + FormatLogging.interpolate(sc.parts, args)
    }
    def debug(args: Any*): String = {
      "\u001B[36m[DEBUG]\u001B[0m " + FormatLogging.interpolate(sc.parts, args)
    }
    def trace(args: Any*): String = {
      "\u001B[35m[TRACE]\u001B[0m " + FormatLogging.interpolate(sc.parts, args)
    }
  }

}
