package com.github.radium226.forge.run

import java.nio.file.Path

import cats.effect._

import cats.implicits._


class Runner[F[_]](folderPath: Path, kinds: List[Kind]) {

  def run(phase: Phase)(implicit F: Concurrent[F]): F[Unit] = {
    kinds.traverse({ kind =>
      kind.run(folderPath).applyOrElse(phase, F.unit)
    }).void
  }

}

object Runner {

  def apply[F[_]](folderPath: Path)(implicit F: Concurrent[F]): F[Runner[F]] = {
    F.pure(new Runner[F](folderPath, List(Make, Sbt)))
  }

}
