package com.github.radium226.forge.config

import shapeless._

import cats.effect._
import cats.implicits._


object Experiment extends IOApp {

  type Argument = String

  trait Resolver[A] {

    def resolve(original: A, fallback: A): A

  }

  trait Resolver0 {
    implicit def genericResolver[A, G <: HList](
      implicit gen: Generic.Aux[A, G],
      updateG: Lazy[Resolver[G]]
    ): Resolver[A] = {
      new Resolver[A] {

        def resolve(original: A, fallback: A): A = {
          gen.from(updateG.value.resolve(gen.to(original), gen.to(fallback)))
        }

      }
    }
  }

  object Resolver extends Resolver0 {

    implicit def optionResolver[A]: Resolver[Option[A]] =
      new Resolver[Option[A]] {
        def resolve(origin: Option[A], fallback: Option[A]): Option[A] = origin orElse fallback
      }

    implicit def hnilResolver: Resolver[HNil] =
      new Resolver[HNil] {
        def resolve(base: HNil, update: HNil): HNil = HNil
      }

    implicit def hconsResolver[H, T <: HList](
      implicit resolverH: Resolver[H], resolverT: Lazy[Resolver[T]]
    ): Resolver[H :: T] =
      new Resolver[H :: T] {
        def resolve(base: H :: T, update: H :: T): H :: T =
          resolverH.resolve(base.head, update.head) :: resolverT.value.resolve(base.tail, update.tail)
      }

    def apply[A](implicit resolver: Resolver[A]): Resolver[A] = {
      resolver
    }

  }

  case class PartialConfig(
    someField: Option[String],
    someOtherField: Option[Int]
  )

  case class Config(
    someField: String,
    someOtherField: Option[Int]
  )

  override def run(arguments: List[Argument]): IO[ExitCode] = {

    val base = PartialConfig(None, Some(1))

    val fallback = PartialConfig(Some("S"), Some(2))

    import Resolver._

    val resolvedPartialConfig = Resolver[PartialConfig].resolve(base, fallback)

    IO(println(resolvedPartialConfig)).as(ExitCode.Success)
  }

}
