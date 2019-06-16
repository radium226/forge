package com.github.radium226.forge.pacman

import org.http4s.dsl.io.{Path => Http4sPath}
import java.nio.file.Path

case class RepoVar(storeFolderPath: Path) {

  def unapply(segment: String): Option[Repo] = {
    Some(Repo(storeFolderPath.resolve(segment)))
  }

}
