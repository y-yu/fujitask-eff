package fujitask.eff

import kits.eff.{ApplicativeInterpreter, Eff, Fx}

import scala.concurrent.{ExecutionContext, Future}

sealed abstract class Fujitask extends Product with Serializable

object Fujitask {
  def apply[I, A](a: => A): Eff[I, A] =
    Eff(Execute(Future(a)(_)))

  def ask[I <: Session: Manifest](): Eff[I, I] =
    Eff(Ask[I]())

  def run[I <: Session: Manifest, A](
    eff: Eff[I, A]
  )(
    implicit runner: FujitaskRunner[I],
    ec: ExecutionContext
  ): Future[A] = {
    def handle(i: I) = new ApplicativeInterpreter[Fujitask, Any] {
      override type Result[T] = Future[T]

      def pure[T](a: T): Eff[Any, Result[T]] = Eff.Pure(Future.successful(a))

      def flatMap[T, B](fa: Fujitask with Fx[T])(k: T => Eff[Any, Future[B]]): Eff[Any, Future[B]] =
        fa match {
          case Execute(f) =>
            Eff.Pure(f(ec).flatMap(a => Eff.run(k(a))))
          case _: Ask[I] =>
            k(i.asInstanceOf[T])
        }

      def ap[T, B](fa: Fujitask with Fx[T])(k: Eff[Any, Result[T => B]]): Eff[Any, Result[B]] =
        fa match {
          case Execute(f) =>
            Eff.Pure(f(ec).flatMap(a => Eff.run(k).map(_(a))))
          case _: Ask[I] =>
            k.map(_.map(_(i.asInstanceOf[T])))
        }

      def map[T, B](fa: Future[T])(k: T => B): Future[B] = fa.map(k)
    }

    runner(i => Eff.run(handle(i)(eff.asInstanceOf[Eff[Fujitask, A]])))
  }

  final case class Execute[A](f: ExecutionContext => Future[A])
    extends Fujitask with Fx[A]

  abstract case class Session() extends Fujitask with Fx[Session]

  case class Ask[I <: Session]() extends Fujitask with Fx[I]
}