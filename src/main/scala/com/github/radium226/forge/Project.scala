package com.github.radium226.forge

import java.nio.file.Path

import com.github.radium226.forge.git._

sealed trait Tool

case object MakePkg

//case object Maven

case object Sbt


case object Make

import com.github.radium226.system.execute._


// forge --remote|--local build

case class Project[F[_]](folderPath: Path) {

  def repoFolderPath: Path = folderPath.resolve("git")

  def buildFolderPath: Path = folderPath.resolve("build")

  def tool: F[Tool] = ???

  def build(commit: Commit[F]): F[Unit] = {
    ???
    /*tool.flatMap({
      case MakePkg =>
        repo.


      case Make =>
        make

    })*/
  }

  def make: F[Unit] = ???

  def makepkg: F[Unit] = ???

}

