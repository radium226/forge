package com.github.radium226.config

import shapeless._
import shapeless.labelled._

trait ToComplete[Input] {

  type Output

  def apply(input: Input): Result[Output]

}

object ToComplete {

  implicit def toCompleteForAny[T]: ToComplete.Aux[Partial[T], T] = new ToComplete[Partial[T]] {

    type Output = T

    def apply(partialForT: Partial[T]): Result[T] = {
      partialForT match {
        case Some(t) =>
          Result.success(t)

        case None =>
          Result.failure(AbsentValueError)
      }
    }


  }

  type Aux[I, O] = ToComplete[I] { type Output = O }

}

trait ToCompleteSyntax {

  implicit class ToCompleteOps[I](input: I) {

    def complete(implicit toCompleteForI: ToComplete[I]): Result[toCompleteForI.Output] = toCompleteForI(input)

  }

}

trait ToCompleteInstances {

  implicit def toCompleteForGeneric[T, ReprT <: HList, PartialForReprT <: HList](implicit
    labelledGeneric: LabelledGeneric.Aux[T, ReprT],
    toPartialForReptT: ToPartial.Aux[ReprT, PartialForReprT]
  ): ToComplete.Aux[T, PartialForReprT] = new ToComplete[T] {

    type Output = PartialForReprT

    def apply(t: T): Result[Output] = {
      toPartialForReptT(labelledGeneric.to(t))
    }

  }

  implicit def toCompleteForHNil: ToComplete.Aux[HNil, HNil] = new ToComplete[HNil] {

    type Output = HNil

    def apply(hNil: HNil): Result[HNil] = {
      Result.success(HNil)
    }

  }

  implicit def toCompleteForHCons[K, PartialForH, PartialForT <: HList, H, T <: HList](implicit
    toCompleteForPartialForH: ToComplete.Aux[PartialForH, H],
    toCompleteForPartialForT: ToComplete.Aux[PartialForT, T],
    witnessForK: Witness.Aux[K]
  ): ToComplete.Aux[FieldType[K, PartialForH] :: PartialForT, FieldType[K, H] :: T] = new ToComplete[FieldType[K, PartialForH] :: PartialForT] {

    type Output = FieldType[K, H] :: T

    def apply(input: FieldType[K, PartialForH] :: PartialForT): Result[Output] = {
      for {
        h <- toCompleteForPartialForH(input.head)
        t <- toCompleteForPartialForT(input.tail)
      } yield field[K](h) :: t
    }

  }

}
