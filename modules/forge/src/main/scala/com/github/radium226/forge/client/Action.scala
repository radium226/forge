package com.github.radium226.forge.client

import java.nio.file.{Paths, Path}

sealed trait Action

object Action {

  case class Init(
    folderPath: Path,
    projectName: String
  )  extends Action

  object Init {

    def default: Init = {
      Init(
        folderPath = Paths.get(System.getProperty("user.dir")),
        projectName = Paths.get(System.getProperty("user.dir")).getFileName.toString
      )
    }

  }

  case object Trash extends Action

  case class EmitHook(
    hookName: Option[String],
    projectName: String
  ) extends Action

  object EmitHook {

    def default = EmitHook(
      hookName = None,
      projectName = Paths.get(System.getProperty("user.dir")).getFileName.toString
    )

  }

  case object Help  extends Action

}