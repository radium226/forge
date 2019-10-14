package com.github.radium226.arguments.instances

import com.github.radium226.arguments.ToPartial

import shapeless._
import shapeless.labelled._

import cats._
import cats.implicits._


trait ToPartialInstances {

  implicit def genericToPartial[F[_], A, ReprA, PartialReprA <: HList](implicit
    labelledGeneric: LabelledGeneric.Aux[A, ReprA], reprAToPartial: ToPartial.Aux[F, ReprA, PartialReprA]
  ): ToPartial.Aux[F, A, PartialReprA] = new ToPartial[F, A] {

    type Output = PartialReprA

    def apply(a: A): F[Output] = reprAToPartial(labelledGeneric.to(a))

  }

  implicit def hnilToPartial[F[_]](implicit
    F: Applicative[F]
  ): ToPartial.Aux[F, HNil, HNil] = new ToPartial[F, HNil] {

    type Output = HNil

    def apply(hnil: HNil): F[Output] = F.pure(HNil)

  }

  implicit def hconsToPartial[F[_]: Applicative, HeadKey <: Symbol, HeadValue, Tail <: HList, PartialTail <: HList](implicit
    headKeyWitness: Witness.Aux[HeadKey],
    tailToPartial: ToPartial.Aux[F, Tail, PartialTail],
    F: Functor[F]
  ): ToPartial.Aux[F, FieldType[HeadKey, HeadValue] :: Tail, FieldType[HeadKey, Option[HeadValue]] :: PartialTail] = new ToPartial[F, FieldType[HeadKey, HeadValue] :: Tail] {

    type Output = FieldType[HeadKey, Option[HeadValue]] :: tailToPartial.Output

    def apply(input: FieldType[HeadKey, HeadValue] :: Tail): F[Output] = tailToPartial(input.tail).map(field[HeadKey](input.head.some) :: _)

  }

}
