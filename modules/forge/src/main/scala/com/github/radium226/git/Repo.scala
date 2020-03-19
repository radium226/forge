package com.github.radium226.git

import java.nio.file.{Files, Path, Paths}

import cats._
import cats.effect._
import cats.implicits._
import com.github.radium226.FileSystem
import com.github.radium226.fs.LocalFileSystem
import com.github.radium226.system.execute._
import org.http4s.Uri

object Repo {

  def in[F[_]](folderPath: Path)(localFileSystem: LocalFileSystem[F], executor: Executor[F])(implicit F: Sync[F]): F[Repo[F]] = for {
    stdout <- executor.withWorkingFolder(folderPath).execute("git", "rev-parse", "--is-bare-repository").foreground(Keep.stdout)
    bare    = stdout.trim == "true"
  } yield new Repo(folderPath, bare)(localFileSystem, executor)

}

class Repo[F[_]](folderPath: Path, bare: Boolean)(localFileSystem: LocalFileSystem[F], executor: Executor[F])(implicit F: Sync[F]) {
  self =>

  def linkHook(hookName: HookName, scriptFilePath: Path): F[Unit] = {
    F.delay(Files.createSymbolicLink(folderPath.resolve("hooks").resolve(hookName), scriptFilePath))
  }

  def add(magnet: AddMagnet[F]): F[Unit] = {
    magnet.filePathsToAdd(folderPath).flatMap({ filePaths =>
      git(List("add") ++ filePaths.map(_.toString): _*)
    })
  }

  def addRemote(name: RemoteName, uri: Uri): F[Unit] = {
    executor.withWorkingFolder(folderPath).execute("git", "remote", "add", name, uri.renderString).foreground.void
  }

  def git(subCommands: String*): F[Unit] = {
    val command = "git" +: subCommands
    executor.withWorkingFolder(folderPath).execute(command: _*).foreground.void
  }

  def updateConfig(key: ConfigKey, value: ConfigValue): F[Unit] = {
    executor
      .withWorkingFolder(folderPath)
      .execute("git", "config", key, value)
      .foreground
      .void
  }

  def updateConfig(config: Config): F[Unit] = {
    val (key, value) = config
    updateConfig(key, value)
  }

  def updateConfig(configs: Config*): F[Unit] = {
    configs.toList.traverse[F, Unit](updateConfig).void
  }

  def cloneTo(folderPath: Path): F[Repo[F]] = {
    Git(localFileSystem, executor).cloneRepo(self.folderPath.toString, folderPath)
  }

  def fetch(remoteName: RemoteName): F[Unit] = {
    git("fetch", remoteName)
  }

  def rebase(branchName: BranchName, remoteName: Option[RemoteName] = None): F[Unit] = {
    git("rebase", remoteName.map(_.concat("/").concat(branchName)).getOrElse(branchName))
  }

  def commit(message: String): F[Unit] = {
    git("commit", "-m", message)
  }

  def push(remoteName: RemoteName): F[Unit] = {
    git("push", remoteName)
  }

  def pull(remoteName: RemoteName): F[Unit] = {
    git("pull", remoteName)
  }

  def files: F[List[Path]] = {
    if (bare) {
      F.raiseError(new Exception("Unable to list files of a bare repo"))
    } else {
      localFileSystem.listFiles(folderPath).map(_.filter(!_.startsWith(Paths.get(".git"))))
    }
  }

}
