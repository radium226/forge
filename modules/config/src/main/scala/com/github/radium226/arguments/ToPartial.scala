package com.github.radium226.arguments

import shapeless.HList

trait ToPartial[F[_], Input] {

  type Output <: HList

  def apply(input: Input): F[Output]

}

object ToPartial {

  type Aux[F[_], Input0, Output0 <: HList] = ToPartial[F, Input0] { type Output = Output0 }

  def apply[F[_], Input](implicit toPartial: ToPartial[F, Input]): ToPartial[F, Input] = toPartial

}
