package com.github.radium226.forge.config

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.{Applicative, ApplicativeError}
import cats.implicits._
import com.github.radium226.forge.config.Experiment.Resolver
import shapeless._
import shapeless.ops.hlist._

object Parser extends App {

  sealed trait SealerError

  case object UnableToSealNoneError extends SealerError

  type SealerResult[A] = Either[NonEmptyList[SealerError], A]

  trait Sealer[A, B] {

    def seal(a: A): SealerResult[B]

  }

  trait Sealer0 {

    implicit def genericSealer[A, HA <: HList, B, HB <: HList](implicit
      genericA: Generic.Aux[A, HA],
      genericB: Generic.Aux[B, HB],
      sealerHA: Lazy[Sealer[HA, HB]],
    ): Sealer[A, B] = { a: A =>
      sealerHA.value.seal(genericA.to(a)).map(genericB.from(_))
    }

    implicit def identitySealer[A]: Sealer[A, A] = { a: A =>
      a.rightNel
    }


  }

  object Sealer extends Sealer0 {

    implicit def optionSealer[A]: Sealer[Option[A], A] = {
      case Some(a: A) =>
        a.rightNel

      case _ =>
        UnableToSealNoneError.leftNel
    }

    implicit def hnilSealer: Sealer[HNil, HNil] = { _ =>
      HNil.rightNel
    }

    implicit def hconsSealer[HA, TA <: HList, HB, TB <: HList](
      implicit sealerH: Sealer[HA, HB], sealerT: Lazy[Sealer[TA, TB]]
    ): Sealer[HA :: TA, HB :: TB] = new Sealer[HA :: TA, HB :: TB] {

      override def seal(a: HA :: TA): SealerResult[HB :: TB] = {
        for {
          bH <- sealerH.seal(a.head)
          bT <- sealerT.value.seal(a.tail)
        } yield bH :: bT
      }

    }

    def apply[A, B](implicit sealer: Sealer[A, B]): Sealer[A, B] = {
      sealer
    }

  }

  case class PartialNested(
    name: Option[String]
  )

  case class PartialConfig(
    name: Option[String],
    value: Option[Int],
    nested: PartialNested
  )

  case class Nested(name: String)

  case class Config(
    name: Option[String],
    value: Int,
    nested: Nested
  )

  import Sealer._

  val partialConfig = PartialConfig(None, Some(1), PartialNested(None))
  //val config: Config = genericSealer.seal(partialConfig)


  val config = Sealer[PartialConfig, Config].seal(partialConfig)

  println(config)

}
