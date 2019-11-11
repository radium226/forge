package com.github.radium226.forge.client

import java.nio.file.{Paths, Path}

sealed trait Action

object Action {

  case class Init(
    projectName: Option[String],
    templateProjectName: Option[String]
  )  extends Action

  case object Trash extends Action

  case class EmitHook(
    hookName: String,
    projectName: String
  ) extends Action

  case object UpdateTemplate extends Action

  case object Help  extends Action

}