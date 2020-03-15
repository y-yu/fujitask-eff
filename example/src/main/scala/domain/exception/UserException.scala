package domain.exception

sealed trait UserException extends Throwable

object UserException {
  case class NoSuchUserException(
    message: String = null,
    cause: Throwable = null
  ) extends UserException
}
