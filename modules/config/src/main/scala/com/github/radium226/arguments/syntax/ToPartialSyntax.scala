package com.github.radium226.arguments.syntax

import com.github.radium226.arguments.ops.ToPartialOps

import scala.language.implicitConversions

trait ToPartialSyntax {

  implicit def toPartialSyntax[Input](input: Input): ToPartialOps[Input] = new ToPartialOps[Input](input)

}
