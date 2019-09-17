package com.github.radium226.forge.run

import java.nio.file.Path

import cats.effect.Concurrent

import scala.util.matching.Regex

case object Sbt extends Kind {

  override def fileNameRegex: Regex = "^build.sbt$".r

  override def run[F[_]](folderPath: Path)(implicit F: Concurrent[F]) = {
    case Phase.Clean =>
      F.unit
  }

}
