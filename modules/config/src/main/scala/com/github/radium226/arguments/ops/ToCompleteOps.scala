package com.github.radium226.arguments.ops

import cats.MonadError
import com.github.radium226.arguments.ToComplete
import shapeless.HList

class ToCompleteOps[Input <: HList](input: Input) {

  def toComplete[F[_]](implicit toCompleteInstance: ToComplete[F, Input], F: MonadError[F, Throwable]): F[toCompleteInstance.Output] = toCompleteInstance.apply(input)

}
