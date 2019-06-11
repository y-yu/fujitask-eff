package repository.impl

import fujitask.eff.Fujitask.Ask
import fujitask.eff.FujitaskRunner
import kits.eff.Eff
import scalikejdbc.DB
import repository.{ReadTransaction, ReadWriteTransaction}

import scala.concurrent.{ExecutionContext, Future}

package object jdbc {
  implicit def readRunner[I >: ReadTransaction](implicit ec: ExecutionContext): FujitaskRunner[I] =
    new FujitaskRunner[I] {
      def apply[A](task: I => Future[A]): Future[A] = {
        println("ReadRunner begin --------->")
        val session = DB.readOnlySession()
        val future = task(new ScalikeJDBCReadTransaction(session))
        future.onComplete { _ =>
          session.close()
          println("<--------- ReadRunner end")

        }
        future
      }
    }

  implicit def readWriteRunner[I >: ReadWriteTransaction](implicit ec: ExecutionContext): FujitaskRunner[I] =
    new FujitaskRunner[I] {
      def apply[A](task: I => Future[A]): Future[A] = {
        println("ReadWriteRunner begin --------->")
        val future = DB.futureLocalTx(session => task(new ScalikeJDBCWriteTransaction(session)))
        future.onComplete(_ =>
          println("<--------- ReadWriteRunner end")
        )
        future
      }
    }
}
