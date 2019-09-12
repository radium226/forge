package com.github.radium226.ficus

import java.nio.file.{Path, Paths}

import com.typesafe.config.Config
import net.ceedubs.ficus.readers.ValueReader

trait implicits {

  implicit def ficusPathValueReader: ValueReader[Path] = new ValueReader[Path] {

    override def read(config: Config, path: String): Path = {
      Paths.get(config.getString(path))
    }

  }

}

object implicits
