package com.github.radium226.forge.config

import shapeless._

import cats.implicits._

/*object AutoPartialAndSealer extends App {

  sealed trait ConfigError

  case object ConfigError extends ConfigError

  type ConfigResult[A] = Either[ConfigError, A]

  trait Config[C] {

    type P

    def partial(c: C): ConfigResult[P]

    def complete(p: P): ConfigResult[C]

  }

  object Config {

    def apply[C](implicit configC: Config[C]): Config[C] = configC

  }

  trait LowPriorityConfigInstances {

    implicit def genericConfig[C, ReprC <: HList](implicit
      generic: Generic.Aux[C, ReprC],
      reprCConfig: Config[ReprC]
    ): Config[C] = new Config[C] {

      type P = reprCConfig.P

      override def partial(c: C): ConfigResult[P] = {
        reprCConfig.partial(generic.to(c))
      }

      override def complete(p: P): ConfigResult[C] = {
        reprCConfig.complete(p).map(generic.from(_))
      }

    }

  }

  trait ConfigInstances extends LowPriorityConfigInstances {

    implicit def config[CompleteRepr <: HList, PartialRepr <: HList](implicit
      completeInstance: Complete.Aux[PartialRepr, CompleteRepr],
      partialInstance: Partial.Aux[CompleteRepr, PartialRepr]
    ): Config[CompleteRepr] = new Config[CompleteRepr] {

      type P = PartialRepr

      override def partial(c: CompleteRepr): ConfigResult[PartialRepr] = {
        Right(partialInstance.apply(c))
      }

      override def complete(p: PartialRepr): ConfigResult[CompleteRepr] = {
        completeInstance.apply(p)
      }

    }

  }

  trait Partial[A] {

    type Out

    def apply(a: A): Out

  }

  trait LowPriorityPartialInstances {

    implicit def hnilPartial: Partial.Aux[HNil, HNil] = new Partial[HNil] {

      type Out = HNil

      def apply(a: HNil): HNil = HNil

    }

    implicit def hconsPartial[ReprAHead, ReprATail <: HList, PartialReprATail <: HList](implicit
      partialReprATail: Partial.Aux[ReprATail, PartialReprATail]
    ): Partial.Aux[ReprAHead :: ReprATail, Option[ReprAHead] :: PartialReprATail] = new Partial[ReprAHead :: ReprATail] {

      type Out = Option[ReprAHead] :: PartialReprATail

      override def apply(a: ReprAHead :: ReprATail): Option[ReprAHead] :: PartialReprATail = { //PartialReprATail = {

        a.head.some :: partialReprATail.apply(a.tail)
      }

    }

  }

  trait PartialInstances extends LowPriorityPartialInstances

  object Partial {

    type Aux[A, ReprA] = Partial[A] { type Out = ReprA }

  }

  trait Complete[PartialA] {

    type Out

    def apply(partialA: PartialA): ConfigResult[Out]

  }

  object Complete {

    type Aux[PartialA, A] = Complete[PartialA] { type Out = A }

  }

  trait LowPriorityCompleteInstances {

    implicit def hnilComplete: Complete.Aux[HNil, HNil] = new Complete[HNil] {

      type Out = HNil

      def apply(partial: HNil): ConfigResult[HNil] = Right(HNil)

    }

    implicit def hconsComplete[ReprAHead, PartialReprATail <: HList, ReprATail <: HList](implicit
      reprATail: Complete.Aux[PartialReprATail, ReprATail]
    ): Complete.Aux[Option[ReprAHead] :: PartialReprATail, ReprAHead :: ReprATail] = new Complete[Option[ReprAHead] :: PartialReprATail] {

      type Out = ReprAHead :: ReprATail

      def apply(partialReprA: Option[ReprAHead] :: PartialReprATail): ConfigResult[Out] = {
        partialReprA.head match {
          case Some(reprAHead) =>
            reprATail.apply(partialReprA.tail).map({ reprATail => reprAHead :: reprATail})
          case None =>
            Left(ConfigError)
        }
      }

    }

  }

  trait CompleteInstances extends LowPriorityCompleteInstances

  object instances extends PartialInstances with CompleteInstances with ConfigInstances

  import instances._
  import ops._

  case class Plane(size: Int, color: String)
  val plane = Plane(2, "Green")
  println(Config[Plane])

}*/
