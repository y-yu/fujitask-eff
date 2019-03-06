package fujitask.eff

import kits.eff.{Arrs, Eff, Union}

object FujitaskEffect {
  implicit class FujitaskEffImplicit[I, R, A](val eff: Eff[Fujitask[I] with R, A]) {
    def flatMap[J >: I, S, B](
      f: A => Eff[Fujitask[J] with S, B]
    )(
      implicit I: Manifest[I]
    ): Eff[Fujitask[I] with S with R, B] = {
      def concat[C](
        arrs: Arrs[Fujitask[I] with R, C, A],
        f: A => Eff[Fujitask[J] with S, B]
      ): Arrs[Fujitask[I] with R with S, C, B] =
        Arrs.Node(
          arrs,
          Arrs.LeafM { (a: A) =>
            new FujitaskEffImplicit[J, S, B](f(a)).bind(b =>
              Fujitask.ask[I].map(_ => b)
            )
          }
        )

      eff match {
        case Eff.Pure(v) =>
          f(v).asInstanceOf[Eff[Fujitask[I] with S with R, B]]
        case Eff.Impure(u, k) =>
          Eff.Impure(u.extend[S], concat(k, f))
      }
    }

    def flatMap[J <: I: Manifest, S, B](
      f: A => Eff[Fujitask[J] with S, B]
    )(
      implicit FJ: Manifest[Fujitask[J]]
    ): Eff[Fujitask[J] with S with R, B] = bind[J, S, B](f)

    def bind[J <: I: Manifest, S, B](
      f: A => Eff[Fujitask[J] with S, B]
    )(
      implicit FJ: Manifest[Fujitask[J]]
    ): Eff[Fujitask[J] with S with R, B] = {
        def concat[C](
          arrs: Arrs[Fujitask[I] with R, C, A])(
          f: A => Eff[Fujitask[J] with S, B]
        ): Arrs[Fujitask[J] with R with S, C, B] =
          Arrs.Node(arrs.asInstanceOf[Arrs[Fujitask[J] with R, C, A]], Arrs.LeafM(f))

        def extend[C](u: Union[Fujitask[I] with R, C]): Union[Fujitask[J] with R with S, C] =
          u.copy(tag = FJ)

        eff match {
          case Eff.Pure(v) =>
            f(v)
          case Eff.Impure(u, k) =>
            Eff.Impure(extend(u), concat(k)(f))
        }

    }

    def map[B](f: A => B): Eff[Fujitask[I] with R, B] =
      eff.map(f)
  }
}
