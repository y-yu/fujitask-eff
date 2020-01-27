package repository

import domain.entity.{User, UserId}
import kits.eff.{Eff, Opt}

trait UserRepository {
  def create(name: String): Eff[ReadWriteTransaction, User]

  def read(id: UserId): Eff[ReadTransaction with Opt, User]

  def update(user: User): Eff[ReadWriteTransaction, Unit]

  def delete(id: UserId): Eff[ReadWriteTransaction, Unit]
}
