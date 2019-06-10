package repository

import domain.entity.User
import kits.eff.Eff

trait UserRepository {
  def create(name: String): Eff[ReadWriteTransaction, User]

  def read(id: Long): Eff[ReadTransaction, Option[User]]

  def update(user: User): Eff[ReadWriteTransaction, Unit]

  def delete(id: Long): Eff[ReadWriteTransaction, Unit]
}
