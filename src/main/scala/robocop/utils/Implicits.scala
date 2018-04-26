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

}
