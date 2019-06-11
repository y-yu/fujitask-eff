package repository.impl.jdbc

import domain.entity.User
import fujitask.eff.Fujitask
import kits.eff.Eff
import repository.{ReadTransaction, ReadWriteTransaction, UserRepository}
import scalikejdbc.DBSession
import scalikejdbc._

class UserRepositoryImpl extends UserRepository {
  def create(name: String): Eff[ReadWriteTransaction, User] =
    Fujitask.ask map { (i: ScalikeJDBCWriteTransaction) =>
      implicit val session: DBSession = i.ctx

      val sql = sql"""insert into user (name) values ($name)"""
      val id = sql.updateAndReturnGeneratedKey.apply()
      User(id, name)
    }

  def read(id: Long): Eff[ReadTransaction, Option[User]] =
    Fujitask.ask map { (i: ScalikeJDBCReadTransaction) =>
      implicit val session: DBSession = i.ctx

      val sql = sql"""select * from user where id = $id"""
      sql.map(rs => User(rs.long("id"), rs.string("name"))).single.apply()
    }

  def update(user: User): Eff[ReadWriteTransaction, Unit] =
    Fujitask.ask map { (i: ScalikeJDBCWriteTransaction) =>
      implicit val session: DBSession = i.ctx

      val sql = sql"""update user set name = ${user.name} where id = ${user.id}"""
      sql.update.apply()
    }

  def delete(id: Long): Eff[ReadWriteTransaction, Unit] =
    Fujitask.ask map { (i: ScalikeJDBCWriteTransaction) =>
      implicit val session: DBSession = i.ctx

      val sql = sql"""delete user where id = $id"""
      sql.update.apply()
    }
}
