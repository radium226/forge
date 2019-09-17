package com.github.radium226.forge.run

import java.nio.file.Path

import cats.effect.Concurrent

import scala.util.matching.Regex

case object Sbt extends Kind with CommandSupport {

  override def fileNameRegex: Regex = "^build.sbt$".r

  override def command = {
    case Phase.Clean =>
      List("sbt", "clean")
  }

}
