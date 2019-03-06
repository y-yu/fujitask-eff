package repository.impl.jdbc

import scalikejdbc.DBSession
import fujitask._

abstract class ScalikeJDBCTransaction(val ctx: DBSession) extends Transaction

class ScalikeJDBCReadTransaction(ctx: DBSession) extends ScalikeJDBCTransaction(ctx) with ReadTransaction

class ScalikeJDBCWriteTransaction(ctx: DBSession) extends ScalikeJDBCReadTransaction(ctx) with ReadWriteTransaction
