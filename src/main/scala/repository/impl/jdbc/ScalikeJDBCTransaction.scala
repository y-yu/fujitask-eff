package repository.impl.jdbc

import scalikejdbc.DBSession
import repository.{ReadTransaction, ReadWriteTransaction, Transaction}

trait ScalikeJDBCTransaction extends Transaction {
  val ctx: DBSession
}

class ScalikeJDBCReadTransaction(val ctx: DBSession)
    extends ScalikeJDBCTransaction
      with ReadTransaction

class ScalikeJDBCWriteTransaction(override val ctx: DBSession)
  extends ScalikeJDBCReadTransaction(ctx)
    with ReadWriteTransaction
