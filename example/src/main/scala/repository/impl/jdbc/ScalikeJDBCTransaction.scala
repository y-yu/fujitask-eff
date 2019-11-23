package repository.impl.jdbc

import fujitask.eff.Fujitask.Transaction
import scalikejdbc.DBSession
import repository.{ReadTransaction, ReadWriteTransaction}

trait ScalikeJDBCTransaction extends Transaction {
  val ctx: DBSession
}

class ScalikeJDBCReadTransaction(val ctx: DBSession)
    extends ScalikeJDBCTransaction
      with ReadTransaction

class ScalikeJDBCWriteTransaction(override val ctx: DBSession)
  extends ScalikeJDBCReadTransaction(ctx)
    with ReadWriteTransaction
