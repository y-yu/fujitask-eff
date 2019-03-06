package fujitask.eff

import scala.concurrent.{ExecutionContext, Future}

trait FujitaskRunner[I] {
  def apply[A](task: I => ExecutionContext => Future[A]): Future[A]
}
