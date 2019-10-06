package com.github.radium226.forge.config

import cats.implicits._
import shapeless._

object OneLastTry extends App {

  // Result
  sealed trait ConfigError

  case object ConfigError extends ConfigError

  type ConfigResult[C] = Either[ConfigError, C]

  // Config
  trait PartialConfig[Complete] {

    type Partial

    //def toPartial(complete: Complete): ConfigResult[Partial]

    def empty: ConfigResult[Partial]

    def complete(partial: Partial): ConfigResult[Complete]

    def merge(onePartial: Partial, otherPartial: Partial): ConfigResult[Partial]

  }

  object PartialConfig {

    type Aux[Complete0, Partial0] = PartialConfig[Complete0] { type Partial = Partial0 }

  }

  trait LowPriorityPartialConfigInstances {

    implicit def genericConfig[Complete, ReprComplete <: HList](implicit
      generic: Generic.Aux[Complete, ReprComplete],
      partialConfig: PartialConfig[ReprComplete]
    ): PartialConfig[Complete] = new PartialConfig[Complete] {

      override type Partial = partialConfig.Partial

      override def toComplete(partial: Partial): ConfigResult[Complete] = {
        config.toComplete(partial).map(generic.from(_))
      }

      override def emptyPartial: ConfigResult[Partial] = {
        config.emptyPartial
      }

      override def mergePartials(onePartial: Partial, otherPartial: Partial): ConfigResult[Partial] = {
        config.mergePartials(onePartial, otherPartial)
      }

    }

  }

  trait ConfigInstances extends LowPriorityConfigInstances {

    implicit def hlistConfig[Complete <: HList, Partial <: HList](implicit
      toPartialInstance: ToPartial.Aux[Complete, Partial],
      toCompleteInstance: ToComplete.Aux[Partial, Complete],
      emptyPartialInstance: Empty[Partial],
      mergeInstance: Merge[Partial]
    ): Config[Complete] = new Config[Complete] {

      override type Partial = toPartialInstance.Output

      override def toPartial(complete: Complete): ConfigResult[Partial] = {
        toPartialInstance.apply(complete)
      }

      override def toComplete(partial: Partial): ConfigResult[Complete] = {
        toCompleteInstance.apply(partial)
      }

      override def emptyPartial: ConfigResult[Partial] = {
        empty[Partial]
      }

      override def mergePartials(onePartial: Partial, otherPartial: Partial): ConfigResult[Partial] = {
        merge[Partial](onePartial, otherPartial)
      }

    }

  }

  // Partial
  type Partial[A] = Option[A]

  object Partial {

    def present[A](a: A): Partial[A] = a.some

    def absent[A]: Partial[A] = None

  }

  // ToPartial
  trait ToPartial[Input] {

    type Output

    def apply(input: Input): ConfigResult[Output]

  }

  object ToPartial {

    type Aux[Input0, Output0] = ToPartial[Input0] { type Output = Output0 }

  }

  trait ToPartialInstances {

    implicit def hnilToPartial: ToPartial.Aux[HNil, HNil] = new ToPartial[HNil] {

      override type Output = HNil

      override def apply(hnil: HNil): ConfigResult[HNil] = Right(hnil)

    }

    implicit def hconsToPartial[Head, Tail <: HList, PartialTail <: HList](implicit
      tailToPartial: ToPartial.Aux[Tail, PartialTail]
    ): ToPartial.Aux[Head :: Tail, Partial[Head] :: PartialTail] = new ToPartial[Head :: Tail] {

      override type Output = Partial[Head] :: PartialTail

      override def apply(complete: Head :: Tail): ConfigResult[Output] = {
        tailToPartial.apply(complete.tail).map(Option(complete.head) :: _)
      }

    }

  }

  // ToComplete
  trait ToComplete[Input] {

    type Output

    def apply(input: Input): ConfigResult[Output]

  }

  object ToComplete {

    type Aux[Input0, Output0] = ToComplete[Input0] { type Output = Output0 }

  }

  trait LowPriorityToCompleteInstances {

    implicit def hnilToComplete: ToComplete.Aux[HNil, HNil] = new ToComplete[HNil] {

      type Output = HNil

      def apply(hnil: HNil): ConfigResult[Output] = Right(HNil)

    }

    implicit def hconsToComplete[Head, PartialTail <: HList, Tail <: HList](implicit
      tailToComplete: ToComplete.Aux[PartialTail, Tail]
    ): ToComplete.Aux[Partial[Head] :: PartialTail, Head :: Tail] = new ToComplete[Partial[Head] :: PartialTail] {

      override type Output = Head :: Tail

      override def apply(partial: Partial[Head] :: PartialTail): ConfigResult[Output] = {
        partial.head match {
          case Some(head) =>
            tailToComplete.apply(partial.tail).map(head :: _)
          case None =>
            Left(ConfigError)
        }
      }

    }

  }

  trait ToCompleteInstances extends LowPriorityToCompleteInstances

  // Default
  trait Empty[A] {

    def apply: ConfigResult[A]

  }

  object Empty {

    def instance[A](a: => ConfigResult[A]): Empty[A] = new Empty[A] {

      override def apply: ConfigResult[A] = a

    }

  }

  object empty {

    def apply[A](implicit empty: Empty[A]): ConfigResult[A] = empty.apply

  }

  trait LowPriorityEmptyInstances {

  }

  trait EmptyInstances extends LowPriorityEmptyInstances {

    implicit def emptyHNil: Empty[HNil] = Empty.instance(Right(HNil))

    implicit def emptyPartialHCons[Head, EmptyPartialTail <: HList](implicit
      emptyPartialTail: Empty[EmptyPartialTail]
    ): Empty[Partial[Head] :: EmptyPartialTail] = Empty.instance(emptyPartialTail.apply.map(none[Head] :: _))

  }

  // Merge
  trait Merge[A] {

    def apply(one: A, other: A): ConfigResult[A]

  }

  object Merge {

    def instance[A](f: (A, A) => ConfigResult[A]): Merge[A] = new Merge[A] {

      override def apply(one: A, other: A): ConfigResult[A] = f(one, other)

    }

  }

  object merge {

    def apply[A](one: A, other: A)(implicit merge: Merge[A]): ConfigResult[A] = merge.apply(one, other)

  }

  trait MergeInstances {

    implicit def mergeHNil: Merge[HNil] = Merge.instance[HNil]({ (_, _) => Right(HNil) })

    implicit def mergePartialHCons[Head, Tail <: HList](implicit
      mergePartialTailInstance: Merge[Tail]
    ): Merge[Partial[Head] :: Tail] = Merge.instance({ (a, b) =>
      mergePartialTailInstance(a.tail, b.tail).map(a.head.orElse(b.head) :: _)
    })

  }

  // Assemble
  object instances extends ConfigInstances
                      with ToPartialInstances
                      with ToCompleteInstances
                      with EmptyInstances
                      with MergeInstances

  // Test
  import instances._

  case class Example(value: String)

  val example = Example("Coucou! ")

  val config = Config.of[Example]

  println(config.emptyPartial)

  for {
    partial  <- config.toPartial(example)
    complete <- config.toComplete(partial)
  } yield assert(complete == example)

}
