package domain.usecase

import domain.entity.{User, UserId}
import domain.exception.UserException
import kits.eff.{Eff, Exc}
import repository.ReadWriteTransaction

trait UpdateUserNameUseCase {
  def updateUserName(
    id: UserId,
    name: String
  ): Eff[ReadWriteTransaction with Exc[UserException], User]
}
