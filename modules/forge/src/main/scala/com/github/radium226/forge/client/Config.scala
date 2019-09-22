package com.github.radium226.forge.client

import java.nio.file.{Path, Paths}

case class Config[F[_]](
  port: Option[Int] = None,
  host: Option[String] = None,
  folderPath: Path = Paths.get(System.getProperty("user.dir")),
  action: Action = Action.Help
) {

  def copyAction(f: PartialFunction[Action, Action]): Config[F] = {
    if (f.isDefinedAt(action)) copy(action = f(action)) else this
  }

}

object Config {

  def empty[F[_]]: Config[F] = {
    Config[F]()
  }

}
