package repository.impl.jdbc

import domain.entity.{User, UserId}
import fujitask.eff.Fujitask
import kits.eff.{Eff, Opt}
import repository.{ReadTransaction, ReadWriteTransaction, UserRepository}
import scalikejdbc.DBSession
import scalikejdbc._

class UserRepositoryImpl extends UserRepository {
  def create(name: String): Eff[ReadWriteTransaction, User] =
    Fujitask.ask map { (i: ScalikeJDBCWriteTransaction) =>
      implicit val session: DBSession = i.ctx

      val sql = sql"""insert into user (name) values ($name)"""
      val id = sql.updateAndReturnGeneratedKey.apply()
      User(UserId(id), name)
    }

  def read(id: UserId): Eff[ReadTransaction with Opt, User] =
    Fujitask.ask flatMap { (i: ScalikeJDBCReadTransaction) =>
      implicit val session: DBSession = i.ctx

      val sql = sql"""select * from user where id = ${id.value}"""
      Opt.lift(
        sql.map(rs => User(UserId(rs.long("id")), rs.string("name"))).single.apply()
      )
    }

  def update(user: User): Eff[ReadWriteTransaction, Unit] =
    Fujitask.ask map { (i: ScalikeJDBCWriteTransaction) =>
      implicit val session: DBSession = i.ctx

      val sql = sql"""update user set name = ${user.name} where id = ${user.id.value}"""
      sql.update.apply()
    }

  def delete(id: UserId): Eff[ReadWriteTransaction, Unit] =
    Fujitask.ask map { (i: ScalikeJDBCWriteTransaction) =>
      implicit val session: DBSession = i.ctx

      val sql = sql"""delete user where id = ${id.value}"""
      sql.update.apply()
    }
}
