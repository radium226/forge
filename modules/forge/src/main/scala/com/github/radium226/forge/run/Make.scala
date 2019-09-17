package com.github.radium226.forge.run

import java.nio.file.Path

import cats.effect.Concurrent
import com.github.radium226.system.execute.Executor

import scala.util.matching.Regex

import cats.implicits._

case object Make extends Kind with CommandSupport {

  override def fileNameRegex: Regex = "^Makefile$".r

  def command: PartialFunction[Phase, List[String]] = {
    case Phase.Clean =>
      List("make", "clean")

    case Phase.Package =>
      List("make", "package")
  }

}
