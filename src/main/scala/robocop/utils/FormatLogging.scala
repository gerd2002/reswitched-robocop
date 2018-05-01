package robocop.utils

import scala.StringContext.treatEscapes

object FormatLogging {

  private[utils] def interpolate(parts: Seq[String], args: Seq[Any]): String = {

    def checkLengths(args: Seq[Any]): Unit =
      if (parts.length != args.length + 1)
        throw new IllegalArgumentException("wrong number of arguments ("+ args.length
          +") for interpolated string with "+ parts.length +" parts")

    checkLengths(args)
    val pi = parts.iterator
    val ai = args.iterator
    val bldr = new StringBuilder(treatEscapes(pi.next()))
    while (ai.hasNext) {
      bldr append ai.next
      bldr append treatEscapes(pi.next())
    }
    bldr.toString
  }
}
