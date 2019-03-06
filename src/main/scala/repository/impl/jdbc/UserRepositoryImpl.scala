package repository.impl.jdbc

import domain.entity.User
import kits.eff.Eff
import repository.UserRepository
import scalikejdbc.DBSession
import fujitask.{ReadTransaction, ReadWriteTransaction}
import fujitask.eff.Fujitask
import scalikejdbc._

class UserRepositoryImpl extends UserRepository {
  import fujitask.eff.Fujitask._

  def create(name: String): Eff[Fujitask[ReadWriteTransaction], User] =
    ask[ScalikeJDBCWriteTransaction] map { i =>
      implicit val session: DBSession = i.ctx

      val sql = sql"""insert into user (name) values ($name)"""
      val id = sql.updateAndReturnGeneratedKey.apply()
      User(id, name)
    }

  def read(id: Long): Eff[Fujitask[ReadTransaction], Option[User]] =
    ask[ScalikeJDBCReadTransaction] map { i =>
      implicit val session: DBSession = i.ctx

      val sql = sql"""select * from user where id = $id"""
      sql.map(rs => User(rs.long("id"), rs.string("name"))).single.apply()
    }

  def update(user: User): Eff[Fujitask[ReadWriteTransaction], Unit] =
    ask[ScalikeJDBCWriteTransaction] map { i =>
      implicit val session: DBSession = i.ctx

      val sql = sql"""update user set name = ${user.name} where id = ${user.id}"""
      sql.update.apply()
    }

  def delete(id: Long): Eff[Fujitask[ReadWriteTransaction], Unit] =
    ask[ScalikeJDBCWriteTransaction] map { i =>
      implicit val session: DBSession = i.ctx

      val sql = sql"""delete users where id = $id"""
      sql.update.apply()
    }
}
