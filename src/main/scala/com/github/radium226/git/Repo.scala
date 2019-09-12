package com.github.radium226.git

import java.nio.file.{Files, Path}

import cats._
import cats.effect._
import cats.implicits._
import com.github.radium226.system.execute._

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

  def addHook(name: HookName, scriptFilePath: Path): F[Unit] = {
    ???
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
