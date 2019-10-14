package com.github.radium226.config

import shapeless._
import shapeless.labelled._
import cats._
import cats.implicits._

import scala.reflect.ClassTag


trait ToPartial[Input] {

  type Output

  def apply(input: Input): Result[Output]

}

object ToPartial {

  // Default
  implicit def toPartialForAny[T]: ToPartial.Aux[T, Partial[T]] = new ToPartial[T] {

    type Output = Partial[T]

    def apply(t: T): Result[Output] = {
      Result.success(Partial.present(t))
    }


  }

  type Aux[I, O] = ToPartial[I] { type Output = O }

  //def apply[I](implicit instance: ToPartial[I]): ToPartial[I] = instance

}

trait ToPartialSyntax {

  implicit class ToPartialOps[I](input: I) {

    def partial(implicit instance: ToPartial[I]): Result[instance.Output] = instance.apply(input)

  }

}

trait ToPartialInstances {

  implicit def toPartialForGeneric[A, ReprA <: HList, PartialForReprA](implicit
    labelledGeneric: LabelledGeneric.Aux[A, ReprA],
    toPartialForReprA: Lazy[ToPartial.Aux[ReprA, PartialForReprA]]
  ): ToPartial.Aux[A, PartialForReprA] = new ToPartial[A] {

    override type Output = PartialForReprA

    override def apply(input: A): Result[Output] = {
      toPartialForReprA.value.apply(labelledGeneric.to(input))
    }

  }

  implicit def toPartialForHCons[K <: Symbol, H, T <: HList, PartialForH, PartialForT <: HList](implicit
    toPartialForH: ToPartial.Aux[H, PartialForH],
    toPartialForT: ToPartial.Aux[T, PartialForT],
    witnessForK: Witness.Aux[K]
  ): ToPartial.Aux[FieldType[K, H] :: T, FieldType[K, PartialForH] :: PartialForT] = new ToPartial[FieldType[K, H] :: T] {

    type Output = FieldType[K, PartialForH] :: PartialForT

    def apply(input: FieldType[K, H] :: T): Result[Output] = {
      for {
        partialForH <- toPartialForH(input.head.asInstanceOf[H])
        partialForT <- toPartialForT(input.tail)
      } yield field[K](partialForH) :: partialForT
    }

  }

  implicit def toPartialForHNil: ToPartial.Aux[HNil, HNil] = new ToPartial[HNil] {

    type Output = HNil

    def apply(hNil: HNil): Result[Output] = Result.success(HNil)

  }

}

//object ToPartialInstances extends ToPartialInstances
