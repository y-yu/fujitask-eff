package fujitask.eff

import scala.concurrent.Future

trait FujitaskRunner[I] {
  def apply[A](task: I => Future[A]): Future[A]
}
