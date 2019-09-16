package com.github.radium226.forge.server

import java.nio.file.{Path, Paths}

case class Config[F[_]](
  port: Option[Int] = None,
  baseFolderPath: Option[Path] = None,
  scriptFolderPath: Option[Path] = None
)

object Config {

  def empty[F[_]]: Config[F] = {
    Config[F]()
  }

}
