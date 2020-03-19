package com.github.radium226.forge.project

import java.nio.file.{Files, Path}

import cats._
import cats.effect._
import io.chrisdavenport.vault._
import cats.implicits._
import com.github.radium226.fs.LocalFileSystem
import com.github.radium226.git.Repo
import com.github.radium226.system.execute.Executor
import com.google.common.io.MoreFiles

object ProjectManager {

  def apply[F[_]](localFileSystem: LocalFileSystem[F], executor: Executor[F])(implicit F: Sync[F]): ProjectManager[F] = new ProjectManager[F](localFileSystem, executor)

}

class ProjectManager[F[_]](localFileSystem: LocalFileSystem[F], executor: Executor[F]) {

  def init[F[_]](baseFolderPath: Path, scriptFolderPath: Path, name: Name)(implicit F: Sync[F]): F[Project[F]] = {
    val folderPath = baseFolderPath.resolve(name)
    for {
      //rootFolderPath <- config.lookup(Keys.RootFolderPath).liftTo[F](new NoSuchElementException)
      _              <- F.delay(Files.createDirectories(folderPath))
      repoFolderPath  = folderPath.resolve("git")
      repo           <- Repo.init[F](repoFolderPath, shared = true, bare = true)
      _              <- repo.updateConfig("http.receivepack", "true")
      _              <- List("post-receive").traverse({ hookName =>
        val scriptFilePath = scriptFolderPath.resolve("emit-hook")
        repo.linkHook(hookName, scriptFilePath)
      })
    } yield new Project[F](folderPath, name, repo)
  }

}

object Project {

  object Keys {

    val RootFolderPath = Key.newKey[IO, Path].unsafeRunSync

  }



  def trash[F[_]](project: Project[F])(implicit F: Sync[F]) = {
    F.delay(MoreFiles.deleteRecursively(project.folderPath))
  }

  def lookUp[F[_]](baseFolderPath: Path, name: Name)(implicit F: Sync[F]): F[Project[F]] = {
    val folderPath = baseFolderPath.resolve(name)
    val repoFolderPath = folderPath.resolve("git")
    for {
      exists  <- F.delay(Files.exists(folderPath))
      repo    <- if (exists) Repo.in[F](repoFolderPath) else F.raiseError[Repo[F]](new IllegalArgumentException)
    } yield new Project(folderPath, name, repo)

  }

}

class Project[F[_]](folderPath: Path, name: Name, repo: Repo[F])(implicit F: Sync[F]) {
  self =>

  def trash: F[Unit] = {
    Project.trash(self)
  }

}
