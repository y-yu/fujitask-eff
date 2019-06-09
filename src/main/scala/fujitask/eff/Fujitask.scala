package fujitask.eff

import kits.eff.{ApplicativeInterpreter, Arrs, Eff, Fx}

import scala.concurrent.{ExecutionContext, Future}

sealed abstract class Fujitask[-I] extends Product with Serializable

object Fujitask {
  def apply[I: Manifest, A](a: => A): Eff[Fujitask[I], A] =
    Eff(Execute(Future(a)(_)))

  def ask[I: Manifest]: Eff[Fujitask[I], I] =
    Eff(Ask[I]())

  def run[I: Manifest, A](
    eff: Eff[Fujitask[I], A]
  )(
    implicit runner: FujitaskRunner[I],
    ec: ExecutionContext
  ): Future[A] = {
    def handle(i: I) = new ApplicativeInterpreter[Fujitask[I], Any] {
      override type Result[T] = Future[T]

      def pure[T](a: T): Eff[Any, Result[T]] = Eff.Pure(Future.successful(a))

      def flatMap[T, B](fa: Fujitask[I] with Fx[T])(k: T => Eff[Any, Future[B]]): Eff[Any, Future[B]] =
        fa match {
          case Execute(f) =>
            Eff.Pure(f(ec).flatMap(a => Eff.run(k(a))))
          case _: Ask[I] => k(i)
        }

      def ap[T, B](fa: Fujitask[I] with Fx[T])(k: Eff[Any, Result[T => B]]): Eff[Any, Result[B]] =
        fa match {
          case Execute(f) =>
            Eff.Pure(f(ec).flatMap(a => Eff.run(k).map(_(a))))
          case _: Ask[I] =>
            k.map(_.map(_(i)))
        }

      def map[T, B](fa: Future[T])(k: T => B): Future[B] = fa.map(k)
    }

    runner(i => Eff.run(handle(i)(eff)))
  }

  case class Execute[-I, A](f: ExecutionContext => Future[A])
    extends Fujitask[I] with Fx[A]

  case class Ask[I]() extends Fujitask[I] with Fx[I]
}
