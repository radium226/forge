package com.github.radium226.arguments

import shapeless.HList

trait ToComplete[F[_], Input <: HList] {

  type Output

  def apply(input: Input): F[Output]

}

object ToComplete {

  type Aux[F[_], Input0 <: HList, Output0] = ToComplete[F, Input0] { type Output = Output0 }

}
