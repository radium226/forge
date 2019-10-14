package com.github.radium226.arguments.instances

import cats.kernel.Monoid

import shapeless._
import shapeless.labelled._

import cats.implicits._


trait MonoidInstances {

  implicit def monoidHNil: Monoid[HNil] = new Monoid[HNil] {

    override def empty: HNil = HNil

    override def combine(x: HNil, y: HNil): HNil = HNil

  }

  implicit def monoidHCons[K <: Symbol, H, T <: HList](implicit
    T: Monoid[T]
  ): Monoid[FieldType[K, Option[H]] :: T] = new Monoid[FieldType[K, Option[H]] :: T] {

    override def empty: FieldType[K, Option[H]] :: T = {
      field[K](none[H]) :: T.empty
    }

    override def combine(x: FieldType[K, Option[H]] :: T, y: FieldType[K, Option[H]] :: T): FieldType[K, Option[H]] :: T = {
      field[K](x.head orElse y.head) :: T.combine(x.tail, y.tail)
    }

  }

}
