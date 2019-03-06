package repository

import domain.entity.User
import kits.eff.Eff
import fujitask._
import fujitask.eff.Fujitask

trait UserRepository {
  def create(name: String): Eff[Fujitask[ReadWriteTransaction], User]

  def read(id: Long): Eff[Fujitask[ReadTransaction], Option[User]]

  def update(user: User): Eff[Fujitask[ReadWriteTransaction], Unit]

  def delete(id: Long): Eff[Fujitask[ReadWriteTransaction], Unit]
}
