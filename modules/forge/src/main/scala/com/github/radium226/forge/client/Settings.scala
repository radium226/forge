package com.github.radium226.forge.client

import java.nio.file.{Path, Paths}

import com.github.radium226.config._

@application("forge")
case class Settings(
  port: Option[Int] = None,
  host: Option[String] = None,
  folderPath: Path = Paths.get(System.getProperty("user.dir")),
  action: Action = Action.Help
)
