package com.github.radium226.forge.server

import com.github.radium226.config._

import java.nio.file.{Path, Paths}

@application("forged")
case class Settings(
  port: Option[Int] = None,
  baseFolderPath: Option[Path] = None,
  scriptFolderPath: Option[Path] = None
)