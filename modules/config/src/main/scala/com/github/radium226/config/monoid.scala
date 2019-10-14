package com.github.radium226.config

import cats.kernel.Monoid
import cats.implicits._

import shapeless._
import shapeless.labelled._

trait MonoidInstances {

  implicit def monoidForHNil: Monoid[HNil] = new Monoid[HNil] {

    override def empty: HNil = HNil

    override def combine(x: HNil, y: HNil): HNil = HNil

  }

  implicit def monoidForHCons[K <: Symbol, H, T <: HList](implicit
    T: Monoid[T]
  ): Monoid[FieldType[K, Partial[H]] :: T] = new Monoid[FieldType[K, Partial[H]] :: T] {

    override def empty: FieldType[K, Partial[H]] :: T = {
      field[K](Partial.absent[H]) :: T.empty
    }

    override def combine(x: FieldType[K, Partial[H]] :: T, y: FieldType[K, Partial[H]] :: T): FieldType[K, Partial[H]] :: T = {
      field[K](x.head orElse y.head) :: T.combine(x.tail, y.tail)
    }

  }

}
