package com.github.radium226.arguments.instances

import com.github.radium226.arguments.ToComplete

import shapeless._
import shapeless.labelled._

import cats._
import cats.implicits._


trait ToCompleteInstances {

  implicit def hconsToComplete[F[_], HeadKey <: Symbol, Head, PartialTail <: HList, Tail <: HList](implicit
    partialTailToComplete: ToComplete.Aux[F, PartialTail, Tail],
    headKeyWitness: Witness.Aux[HeadKey],
    F: MonadError[F, Throwable]
  ): ToComplete.Aux[F, FieldType[HeadKey, Option[Head]] :: PartialTail, FieldType[HeadKey, Head] :: Tail] = new ToComplete[F, FieldType[HeadKey, Option[Head]] :: PartialTail] {

    type Output = FieldType[HeadKey, Head] :: Tail

    def apply(input: FieldType[HeadKey, Option[Head]] :: PartialTail): F[Output] = {
      for {
        head <- input.head.liftTo[F](new NoSuchElementException)
        tail <- partialTailToComplete(input.tail)
      } yield field[HeadKey](head) :: tail
    }

  }

  implicit def hnilToComplete[F[_]](implicit
    F: MonadError[F, Throwable]
  ): ToComplete.Aux[F, HNil, HNil] = new ToComplete[F, HNil] {

    type Output = HNil

    def apply(hnil: HNil): F[Output] = {
      F.pure(HNil)
    }

  }

  implicit def genericToComplete[F[_], PartialReprA <: HList, ReprA <: HList, A](implicit
    labelledGeneric: LabelledGeneric.Aux[A, ReprA],
    reprAToComplete: ToComplete.Aux[F, PartialReprA, ReprA],
    F: MonadError[F, Throwable]
  ): ToComplete.Aux[F, PartialReprA, A] = new ToComplete[F, PartialReprA] {

    type Output = A

    def apply(input: PartialReprA): F[Output] = {
      reprAToComplete(input).map(labelledGeneric.from(_))
    }

  }

}
