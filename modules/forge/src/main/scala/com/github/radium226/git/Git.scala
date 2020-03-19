package com.github.radium226.git

import cats.effect._
import cats.implicits._

import java.nio.file.{Files, Path}

import com.github.radium226.fs.LocalFileSystem
import com.github.radium226.system.execute.Executor


class Git[F[_]](localFileSystem: LocalFileSystem[F], executor: Executor[F])(implicit F: Sync[F]) {

  def initRepo(folderPath: Path, shared: Boolean = false, bare: Boolean = false, templateFolderPath: Option[Path] = None): F[Repo[F]] = {
    val command = List("git", "init") ++
      (if (shared) List("--shared") else List.empty) ++
      (if (bare) List("--bare") else List.empty) ++
      templateFolderPath
        .map(_.toString)
        .map(List(_))
        .getOrElse(List.empty) :+
      folderPath.toString

    for {
      _    <- localFileSystem.createFolder(folderPath)
      _    <- executor.withWorkingFolder(folderPath).execute(command: _*).foreground
      repo <- Repo.in[F](folderPath)(localFileSystem, executor)
    } yield repo
  }

  def cloneRepo(url: RepoUrl, folderPath: Path): F[Repo[F]] = {
    for {
      _    <- localFileSystem.createFolder(folderPath)
      _    <- executor.execute("git", "clone", url, folderPath.toString).foreground
      repo <- Repo.in(folderPath)(localFileSystem, executor)
    } yield repo
  }

}

object Git {

  def apply[F[_]](localFileSystem: LocalFileSystem[F], executor: Executor[F])(implicit F: Sync[F]): Git[F] = new Git[F](localFileSystem, executor)

}
