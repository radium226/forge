package com.github.radium226.forge.pacman

import cats.implicits._
import cats.effect._

import sys.process._
import java.nio.file.{Files, Path}

import com.github.radium226.io._

case class Repo(folderPath: Path) {

  def name: Name = {
    folderPath.getFileName.toString
  }

  def addPkg[F[_]](sourcePkgFilePath: Path)(implicit F: Async[F], contextShift: ContextShift[F]): F[Unit] = {
    println("0.")
    for {
      pkg               <- Pkg.read[F](sourcePkgFilePath)
      _ = println("1.")
      _ = println(s"pkg.arch=${pkg.arch}")
      _ <- (pkg.arch match {
        case Arch.any =>
          Arch.all

        case arch =>
          List(arch)
      }).traverse({ arch =>
        val targetFolderPath   = folderPath.resolve("os").resolve(arch.name)
        println(s"targetFolderPath=${targetFolderPath}")
        val targetPkgFilePath  = targetFolderPath.resolve(pkg.fileName)
        for {
          _          <- makeParentFolder[F](targetPkgFilePath)
          _ = println("1.")
          _          <- F.delay(Files.copy(sourcePkgFilePath, targetPkgFilePath))
          _ = println("2.")
          dbFilePath  = targetFolderPath.resolve(s"${name}.db.tar.gz")
          _ = println("3.")
          exitCode   <- F.delay(Seq("repo-add", dbFilePath.toAbsolutePath.toString, targetPkgFilePath.toAbsolutePath.toString) !)
          _ = println("4.")
          _          <- if (exitCode > 0) F.raiseError[Unit](new Exception("Unable to add package to repo")) else F.unit
        } yield ()
      })
      _ <- F.delay(Files.delete(sourcePkgFilePath))
    } yield ()
  }

}
