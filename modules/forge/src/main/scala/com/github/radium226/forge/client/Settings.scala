package com.github.radium226.forge.client

import java.nio.file.{Path, Paths}

import com.github.radium226.config._

@application("forge")
case class Settings(
  port: Int,
  host: String,
  folderPath: Option[Path],
  action: Action
)
