package fujitask.eff

import kits.eff.{ApplicativeInterpreter, Eff, Exc, Fx}
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

  def runWithEither[E: Manifest, I <: Transaction: Manifest, A](
    eff: Eff[Exc[E] with I, A]
  )(
    implicit runner: FujitaskRunner[I],
    ec: ExecutionContext
  ): Future[E Either A] = {
    final case class FujitaskErrorWrapper(
      value: E
    ) extends Throwable(value.toString)

    def handle(i: I) = new ApplicativeInterpreter[Fujitask, Exc[E]] {
      override type Result[T] = Future[T]

      def functor[T, B](fa: Future[T])(k: T => B): Future[B] = fa.map(k)

      def pure[T](a: T): Eff[Exc[E], Result[T]] = Eff.Pure(Future.successful(a))

      def flatMap[T, B](k: T => Eff[Exc[E], Future[B]]): PartialFunction[Fx[T], Eff[Exc[E], Future[B]]] = {
        case Execute(f) =>
          Eff.Pure(
            f(ec).flatMap { a =>
              Eff.run(
                Exc.run(k(a)).asInstanceOf[Eff[Any, E Either Future[B]]].map {
                  case Right(future) =>
                    future
                  case Left(e) =>
                    Future.failed(FujitaskErrorWrapper(e))
                }
              )
            }
          )
        case _: Ask[I] =>
          Eff.Pure(
            Eff.run(
              Exc.run(k(i.asInstanceOf[T])).asInstanceOf[Eff[Any, E Either Future[B]]].map {
                case Right(future) =>
                  future
                case Left(e) =>
                  Future.failed(FujitaskErrorWrapper(e))
              }
            )
          )
      }

      def ap[T, B](k: Eff[Exc[E], Future[T => B]]): PartialFunction[Fx[T], Eff[Exc[E], Future[B]]] = {
        case Execute(f) =>
          Eff.Pure(
            f(ec).flatMap(a => Eff.run(Exc.run(k).asInstanceOf[Eff[Any, E Either Future[T => B]]] map {
              case Right(future) =>
                future.map(_ (a))
              case Left(e) =>
                Future.failed(FujitaskErrorWrapper(e))
            }))
          )
        case _: Ask[I] =>
          Exc.run(k).asInstanceOf[Eff[Any, E Either Future[T => B]]] map {
            case Right(future) =>
              future.map(_(i.asInstanceOf[T]))
            case Left(e) =>
              Future.failed(FujitaskErrorWrapper(e))
          }

      }
    }

    runner { i =>
      Eff.run(
        Exc.run(
          handle(i)(eff.asInstanceOf[Eff[Fujitask with Exc[E], A]]) map { future =>
            future.map(Right.apply[E, A])
          }
        ).map {
          case Right(future) => future
          case l @ Left(_) => Future.successful(l)
        }
        .asInstanceOf[Eff[Any, Future[E Either A]]]
      )
    }.recover {
      case FujitaskErrorWrapper(e) =>
        Left(e)
    }
  }

  final case class Execute[A](f: ExecutionContext => Future[A])
    extends Fujitask with Fx[A]

  abstract case class Transaction() extends Fujitask with Fx[Transaction]

  final case class Ask[I <: Transaction]() extends Fujitask with Fx[I]
}
