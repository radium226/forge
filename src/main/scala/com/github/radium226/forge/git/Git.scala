package com.github.radium226.forge.git

import java.nio.file.Path
import java.util

import com.github.radium226.system.execute.{Executor, Keep}
import fs2._
import cats.effect._
import cats.effect.concurrent.MVar
import cats.implicits._
import fs2.concurrent.Topic

import scala.concurrent.duration._

class Repo[F[_]](val folderPath: Path, commitTopic: CommitTopic[F]) {

  def executor: Executor[F] = {
    Executor[F](workingFolderPath = Some(folderPath))
  }

  def add(filePaths: Path*)(implicit F: Sync[F]): F[Unit] = {
    executor.execute(Seq("git", "add") ++ filePaths.map(_.toString): _*).foreground.void
  }

  def commit(message: String)(implicit F: Sync[F]): F[Unit] = {
    executor.execute("git", "commit", "-m", message).foreground.void
  }

  def push(implicit F: Sync[F]): F[Unit] = {
    executor.execute("git", "push").foreground.void
  }

  def commits: Stream[F, Commit[F]] = {
    commitTopic.subscribe(1).unNone // FIXME: Not sure
  }

}

object Git {

  def apply[F[_]](folderPath: Path, polling: Polling[F])(implicit F: Concurrent[F], timer: Timer[F]): Git[F] = {
    new Git(folderPath, polling)
  }

}

class Git[F[_]](folderPath: Path, polling: Polling[F]) {

  def useRepo(implicit F: Concurrent[F], timer: Timer[F]): F[Repo[F]] = {
    polling.topic(folderPath).map({ commitTopic =>
      new Repo[F](folderPath, commitTopic)
    })
  }

  def cloneRepo(uri: URI)(implicit F: Concurrent[F], timer: Timer[F]): F[Repo[F]] = {
    val executor = Executor[F](workingFolderPath = Some(folderPath))
    F.delay(println(s"Cloning ${uri} to ${folderPath}")) *> executor.execute("git", "clone", uri, ".").foreground *> useRepo
  }

  def initRepo(): F[Repo[F]] = ???

}

case class Commit[F[_]](sha1: SHA1)