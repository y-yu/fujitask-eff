package infra.db

import scalikejdbc._
import scalikejdbc.config.DBs

object Database {
  private val createUserTable = sql"""
    create table `user` (
      `id` bigint not null primary key auto_increment,
      `name` varchar(64) not null
    )
  """

  private val deleteAll = sql"""
    delete from `user`
  """

  private val count = sql"""
    select count(1) from `user`
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

  def isEmpty: Boolean = {
    (DB readOnly { implicit s =>
      count.map(_.int(1)).single.apply()
    }).map(_ == 0).get
  }
}
