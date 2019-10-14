package com.github.radium226.arguments.ops

import cats.MonadError
import com.github.radium226.arguments.ToPartial

class ToPartialOps[Input](input: Input) {

  def toPartial[F[_]](implicit toPartialInstance: ToPartial[F, Input], F: MonadError[F, Throwable]): F[toPartialInstance.Output] = toPartialInstance.apply(input)

}
