package com.github.radium226.git

import java.nio.file.{Files, Path}

import cats._
import cats.effect._
import cats.implicits._
import com.github.radium226.system.execute._
import org.http4s.Uri

object Repo {

  def init[F[_]](folderPath: Path, shared: Boolean = false, bare: Boolean = false, templateFolderPath: Option[Path] = None)(implicit F: Sync[F]): F[Repo[F]] = {
    val command = List("git", "init") ++
      (if (shared) List("--shared") else List.empty) ++
      (if (bare) List("--bare") else List.empty) ++
      templateFolderPath
        .map(_.toString)
        .map(List(_))
        .getOrElse(List.empty) :+
      folderPath.toString

    F.delay(Files.createDirectories(folderPath)) *> Executor[F].execute(command: _*).foreground.as(Repo[F](folderPath, bare))
  }

  def apply[F[_]](folderPath: Path, bare: Boolean): Repo[F] = {
    new Repo[F](folderPath, bare)
  }

  def in[F[_]](folderPath: Path): F[Repo[F]] = {
    ???
  }

}



case class Repo[F[_]](folderPath: Path, bare: Boolean) {

  def linkHook(hookName: HookName, scriptFilePath: Path)(implicit F: Sync[F]): F[Unit] = {
    F.delay(Files.createSymbolicLink(scriptFilePath, folderPath.resolve("hooks").resolve(hookName)))
  }

  def addRemote(name: RemoteName, uri: Uri)(implicit F: Sync[F]): F[Unit] = {
    Executor[F](workingFolderPath = Some(folderPath)).execute("git", "remote", "add", name, uri.renderString).foreground.void
  }

  def git(subCommands: String*)(implicit F: Sync[F]): F[Unit] = {
    val command = "git" +: subCommands
    Executor[F](workingFolderPath = Some(folderPath)).execute(command: _*).foreground.void
  }

  def updateConfig(key: ConfigKey, value: ConfigValue)(implicit F: Sync[F]): F[Unit] = {
    val command = if (bare) {
      Seq("git", "config", "--file", folderPath.resolve("config").toString, key, value)
    } else ???
    Executor[F].execute(command: _*).foreground.as(Repo[F](folderPath, bare))
  }

  def updateConfig(config: Config)(implicit F: Sync[F]): F[Unit] = {
    val (key, value) = config
    updateConfig(key, value)
  }

  def updateConfig(configs: Config*)(implicit F: Sync[F]): F[Unit] = {
    configs.toList.traverse[F, Unit](updateConfig).void
  }

}
