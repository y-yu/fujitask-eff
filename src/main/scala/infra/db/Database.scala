package infra.db

import scalikejdbc._
import scalikejdbc.config.DBs

object Database {
  private def createUserTable = sql"""
    create table `user` (
      `id` bigint not null auto_increment,
      `name` varchar(64) not null
    )
  """

  def setUp(): Unit = DBs.setupAll()

  def close(): Unit = DBs.closeAll()

  def createTable(): Unit = {
    DB localTx { implicit s =>
      createUserTable.execute().apply()
    }
  }
}
