package com.github.radium226.forge.client

import java.nio.file.Paths

import enumeratum._

sealed trait Action

object Action {

  case class Init(
    projectName: String
  )  extends Action

  object Init {

    def default: Init = {
      Init(
        projectName = Paths.get(System.getProperty("user.dir")).getFileName.toString
      )
    }

  }

  case object Trash extends Action
  case object Help  extends Action

}