package fujitask.eff

import kits.eff.{ApplicativeInterpreter, Arrs, Eff, Fx}

import scala.concurrent.{ExecutionContext, Future}

sealed abstract class Fujitask[-I] extends Product with Serializable

object Fujitask {
  def apply[I: Manifest, A](a: => A): Eff[Fujitask[I], A] =
    Eff(Execute( (_: I) => _ => Future.successful(a)))

  def ask[I: Manifest]: Eff[Fujitask[I], I] =
    Eff(Ask[I]())

  def run[I, A](
    eff: Eff[Fujitask[I], A]
  )(
    implicit runner: FujitaskRunner[I],
    ec: ExecutionContext,
    I: Manifest[I]
  ): Future[A] = {
    val handle = new ApplicativeInterpreter[Fujitask[I], Any] {
      override type Result[T] = I => ExecutionContext => Future[T]

      def pure[T](a: T): Eff[Any, Result[T]] = Eff.Pure(_ => _ => Future.successful(a))

      def flatMap[T, B](fa: Fujitask[I] with Fx[T])(k: T => Eff[Any, Result[B]]): Eff[Any, Result[B]] =
        fa match {
          case Execute(f) =>
            Eff.Pure( i => implicit ec =>
              f(i)(ec).map(k).flatMap(Eff.run(_)(i)(ec))
            )
          case Ask() =>
            Eff.Pure( i => implicit ec =>
              Eff.run(k(i.asInstanceOf[T]).map(_(i)(ec)))
            )
        }

      def ap[T, B](fa: Fujitask[I] with Fx[T])(k: Eff[Any, Result[T => B]]): Eff[Any, Result[B]] =
        fa match {
          case Execute(f) =>
            Eff.Pure(i => implicit ec =>
              f(i)(ec).flatMap(a => Eff.run(k)(i)(ec).map(f => f(a)))
            )
          case Ask() =>
            Eff.Pure( i => implicit ec =>
              Eff.run(k.map(_(i)(ec).map(f => f(i.asInstanceOf[T]))))
            )
        }

      def map[T, B](fa: Result[T])(k: T => B): Result[B] =
        i => implicit ec => fa(i)(ec).map(k)
    }

    runner(
      Eff.run(handle(eff))
    )
  }

  case class Execute[-I, A](f: I => ExecutionContext => Future[A])
    extends Fujitask[I] with Fx[A]

  case class Ask[I]() extends Fujitask[I] with Fx[I]
}
