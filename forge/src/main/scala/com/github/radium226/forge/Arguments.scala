package com.github.radium226.forge

import java.nio.file.{Path, Paths}

case class Arguments(port: Port, folderPath: Path)

object Arguments {

  def default: Arguments = {
    Arguments(1234, Paths.get("/tmp/maven/repo"))
  }

}
