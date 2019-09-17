package com.github.radium226.forge.run

import java.nio.file.Path

import cats.effect.Concurrent
import com.github.radium226.system.execute.Executor

import cats.implicits._

trait CommandSupport {
  self: Kind =>

  def command: PartialFunction[Phase, List[String]]

  override def run[F[_]](folderPath: Path)(implicit F: Concurrent[F]) = {
    command.andThen(Executor[F](workingFolderPath = Some(folderPath)).execute(_ :_*).foreground.void)
  }

}
