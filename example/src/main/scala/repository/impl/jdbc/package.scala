package repository.impl

import fujitask.eff.FujitaskRunner
import org.slf4j.{Logger, LoggerFactory}
import scalikejdbc.DB
import repository.{ReadTransaction, ReadWriteTransaction}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

package object jdbc {
  lazy val logger: Logger = LoggerFactory.getLogger(this.getClass)

  implicit def readRunner[I >: ReadTransaction](implicit ec: ExecutionContext): FujitaskRunner[I] =
    new FujitaskRunner[I] {
      def apply[A](task: I => Future[A]): Future[A] = {
        logger.info("ReadRunner begin --------->")
        val session = DB.readOnlySession()
        val future = task(new ScalikeJDBCReadTransaction(session))
        future.onComplete { _ =>
          logger.info("<--------- ReadRunner end")
          session.close()
        }
        future
      }
    }

  implicit def readWriteRunner[I >: ReadWriteTransaction](implicit ec: ExecutionContext): FujitaskRunner[I] =
    new FujitaskRunner[I] {
      def apply[A](task: I => Future[A]): Future[A] = {
        logger.info("ReadWriteRunner begin --------->")
        val future = DB.futureLocalTx { session =>
          task(new ScalikeJDBCWriteTransaction(session))
        }
        future.onComplete {
          case Success(_) =>
            logger.info("<--------- ReadWriteRunner end")
          case Failure(e) =>
            logger.warn(s"failed!: ${e.getMessage}")
        }
        future
      }
    }
}
