package com.github.radium226.forge.server

import com.github.radium226.config._

import java.nio.file.Path

@application("forged")
case class Settings(
  port: Int,
  baseFolderPath: Path,
  scriptFolderPath: Path
)