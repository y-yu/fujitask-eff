package fujitask.eff

import kits.eff.{ApplicativeInterpreter, Eff, Fx}

import scala.concurrent.{ExecutionContext, Future}

sealed abstract class Fujitask extends Product with Serializable

object Fujitask {
  def apply[I, A](a: => A): Eff[I, A] =
    Eff(Execute(Future(a)(_)))

  def ask[R <: Transaction, I <: R]: Eff[R, I] =
    Eff(Ask())

  def run[I <: Transaction: Manifest, A](
    eff: Eff[I, A]
  )(
    implicit runner: FujitaskRunner[I],
    ec: ExecutionContext
  ): Future[A] = {
    def handle(i: I) = new ApplicativeInterpreter[Fujitask, Any] {
      override type Result[T] = Future[T]

      def functor[T, B](fa: Future[T])(k: T => B): Future[B] = fa.map(k)

      def pure[T](a: T): Eff[Any, Result[T]] = Eff.Pure(Future.successful(a))

      def flatMap[T, B](k: T => Eff[Any, Future[B]]): PartialFunction[Fx[T], Eff[Any, Future[B]]] = {
        case Execute(f) =>
          Eff.Pure(f(ec).flatMap(a => Eff.run(k(a))))
        case _: Ask[I] =>
          k(i.asInstanceOf[T])
      }

      def ap[T, B](k: Eff[Any, Future[T => B]]): PartialFunction[Fx[T], Eff[Any, Future[B]]] = {
        case Execute(f) =>
          Eff.Pure(f(ec).flatMap(a => Eff.run(k).map(_(a))))
        case _: Ask[I] =>
          k.map(_.map(_(i.asInstanceOf[T])))
      }
    }

    runner(i => Eff.run(handle(i)(eff.asInstanceOf[Eff[Fujitask, A]])))
  }

  final case class Execute[A](f: ExecutionContext => Future[A])
    extends Fujitask with Fx[A]

  abstract case class Transaction() extends Fujitask with Fx[Transaction]

  final case class Ask[I <: Transaction]() extends Fujitask with Fx[I]
}