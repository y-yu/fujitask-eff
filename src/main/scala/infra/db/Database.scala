package infra.db

import scalikejdbc._
import scalikejdbc.config.DBs

object Database {
  private val createUserTable = sql"""
    create table `user` (
      `id` bigint not null auto_increment,
      `name` varchar(64) not null
    )
  """

  private val deleteAll = sql"""
    delete from `user`
  """

  def setUp(): Unit = DBs.setupAll()

  def close(): Unit = DBs.closeAll()

  def createTable(): Unit = {
    DB localTx { implicit s =>
      createUserTable.execute().apply()
    }
  }

  def deleteAllData(): Unit = {
    DB localTx { implicit s =>
      deleteAll.execute().apply()
    }
  }
}
