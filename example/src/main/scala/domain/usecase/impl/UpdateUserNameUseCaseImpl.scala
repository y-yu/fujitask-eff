package domain.usecase.impl

import domain.entity.{User, UserId}
import domain.exception.UserException
import domain.exception.UserException.NoSuchUserException
import domain.usecase.UpdateUserNameUseCase
import javax.inject.Inject
import kits.eff.{Eff, Exc, Opt}
import repository.{ReadWriteTransaction, UserRepository}

class UpdateUserNameUseCaseImpl @Inject()(
  userRepository: UserRepository
) extends UpdateUserNameUseCase {
  def updateUserName(
    id: UserId,
    name: String
  ): Eff[ReadWriteTransaction with Exc[UserException], User] = {
    val opt = for {
      user <- userRepository.read(id)
      updatedUser = user.copy(name = name)
      _ <- userRepository.update(updatedUser)
    } yield updatedUser

    Opt.run(opt) flatMap {
      case Some(updateUser) =>
        Exc.lift(Right(updateUser))
      case None =>
        Exc.lift(Left(NoSuchUserException("No such a user!")))
    }
  }
}
