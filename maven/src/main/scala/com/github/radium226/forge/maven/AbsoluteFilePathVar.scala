package com.github.radium226.forge.maven

import java.nio.file.Path

import org.http4s.dsl.io.{Path => Http4sPath}

case class AbsoluteFilePathVar(parentFolderPath: Path) {

  def unapply(path: Http4sPath): Option[Path] = {
    path.toPathOption.map(parentFolderPath.resolve(_))
  }

}
