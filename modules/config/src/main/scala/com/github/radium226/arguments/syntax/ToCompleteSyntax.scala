package com.github.radium226.arguments.syntax

import com.github.radium226.arguments.ops.ToCompleteOps
import shapeless.HList

import scala.language.implicitConversions

trait ToCompleteSyntax {

  implicit def toCompleteSyntax[Input <: HList](input: Input): ToCompleteOps[Input] = new ToCompleteOps[Input](input)

}
